package com.bank.platform.quotation.domain;

import com.bank.common.error.ErrorCodes;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The quote while the sale is open: plaintext control attributes (schemas/20 §1.2) plus the
 * decrypted envelope. Once {@link #materializedAt()} is set the quote is frozen and every change is
 * refused — Aurora is the record from then on (DB-DEC-0005).
 */
public final class WorkingQuote {

  /** Everything the store persists. {@code encryptionKeyId} is null until first written. */
  public record Snapshot(
      String quoteId,
      String leadId,
      String journeyId,
      String customerId,
      String suitabilityId,
      String consentId,
      String lob,
      String productClass,
      String mode,
      String category,
      QuoteState state,
      Instant validUntil,
      String provider,
      String providerJobId,
      String configVersionRef,
      boolean paymentInFlight,
      String heldByPaymentId,
      Instant materializedAt,
      long version,
      Instant createdAt,
      Instant updatedAt,
      QuoteEnvelope envelope,
      String encryptionKeyId) {}

  private final String quoteId;
  private final String leadId;
  private final String journeyId;
  private final String customerId;
  private final String suitabilityId;
  private final String consentId;
  private final String lob;
  private final String productClass;
  private final String mode;
  private final String category;
  private final String configVersionRef;
  private final Instant createdAt;
  private QuoteState state;
  private Instant validUntil;
  private String provider;
  private String providerJobId;
  private boolean paymentInFlight;
  private String heldByPaymentId;
  private Instant materializedAt;
  private long version;
  private Instant updatedAt;
  private QuoteEnvelope envelope;
  private String encryptionKeyId;

  private WorkingQuote(Snapshot s) {
    this.quoteId = s.quoteId();
    this.leadId = s.leadId();
    this.journeyId = s.journeyId();
    this.customerId = s.customerId();
    this.suitabilityId = s.suitabilityId();
    this.consentId = s.consentId();
    this.lob = s.lob();
    this.productClass = s.productClass();
    this.mode = s.mode();
    this.category = s.category();
    this.configVersionRef = s.configVersionRef();
    this.createdAt = s.createdAt();
    this.state = s.state();
    this.validUntil = s.validUntil();
    this.provider = s.provider();
    this.providerJobId = s.providerJobId();
    this.paymentInFlight = s.paymentInFlight();
    this.heldByPaymentId = s.heldByPaymentId();
    this.materializedAt = s.materializedAt();
    this.version = s.version();
    this.updatedAt = s.updatedAt();
    this.envelope = s.envelope();
    this.encryptionKeyId = s.encryptionKeyId();
  }

  /** A new quote in {@code REQUESTED}; version 0 until the store writes it as version 1. */
  public static WorkingQuote request(String quoteId, NewQuote request, Instant now) {
    Instant at = truncate(now);
    QuoteEnvelope envelope =
        new QuoteEnvelope(
            request.requestedCover(),
            request.mode(),
            request.category(),
            request.members(),
            List.of(),
            List.of(),
            null);
    return new WorkingQuote(
        new Snapshot(
            quoteId,
            request.leadId(),
            request.journeyId(),
            request.customerId(),
            request.suitabilityId(),
            request.consentId(),
            request.lob(),
            request.productClass(),
            request.mode(),
            request.category(),
            QuoteState.REQUESTED,
            null,
            null,
            null,
            request.configVersionRef(),
            false,
            null,
            null,
            0,
            at,
            at,
            envelope,
            null));
  }

  public static WorkingQuote restore(Snapshot snapshot) {
    return new WorkingQuote(snapshot);
  }

  /** {@code REQUESTED → IN_PROGRESS}: the Hub accepted the provider request. */
  public void startProviderJob(String providerName, String jobId, Instant now) {
    requireOpen();
    requireState(QuoteState.REQUESTED);
    this.provider = providerName;
    this.providerJobId = jobId;
    this.state = QuoteState.IN_PROGRESS;
    touch(now);
  }

  /**
   * Ingests the insurers' answers. Offers with a non-positive premium or sum assured become {@code
   * INVALID} ({@code INV-QUO-03}); at least one valid offer is {@code QUOTED}, or {@code
   * PARTIALLY_QUOTED} when some insurer failed ({@code INV-QUO-02}); none is {@code FAILED}.
   */
  public void recordOffers(
      List<QuoteOffer> offers, List<InsurerError> errors, Instant offersValidUntil, Instant now) {
    requireOpen();
    if (state != QuoteState.REQUESTED && state != QuoteState.IN_PROGRESS) {
      throw illegal("record offers");
    }
    List<QuoteOffer> classified = new ArrayList<>();
    boolean anyValid = false;
    for (QuoteOffer offer : offers) {
      if (offer.hasPositiveAmounts()) {
        classified.add(offer.withState(OfferState.QUOTED, offer.errorSummary()));
        anyValid = true;
      } else {
        classified.add(
            offer.withState(OfferState.INVALID, "INV-QUO-03: non-positive premium or sum assured"));
      }
    }
    List<InsurerError> failures = errors == null ? List.of() : errors;
    if (!anyValid) {
      state = QuoteState.FAILED;
    } else {
      state = failures.isEmpty() ? QuoteState.QUOTED : QuoteState.PARTIALLY_QUOTED;
    }
    this.validUntil = offersValidUntil == null ? null : truncate(offersValidUntil);
    this.envelope =
        new QuoteEnvelope(
            envelope.requestedCover(),
            envelope.mode(),
            envelope.category(),
            envelope.members(),
            classified,
            failures,
            null);
    touch(now);
  }

  /**
   * Selects one offer. The chosen offer becomes {@code SELECTED} and every valid sibling {@code
   * NOT_SELECTED} in the same envelope that carries the {@code selected*} attributes ({@code
   * INV-QUO-05}); past {@code validUntil} the quote cannot be selected ({@code INV-QUO-04}).
   */
  public void select(String offerId, Instant now) {
    requireOpen();
    if (!state.isSelectable()) {
      throw illegal("select an offer");
    }
    if (validUntil == null || !now.isBefore(validUntil)) {
      throw new QuoteRuleException(
          ErrorCodes.QUOTE_EXPIRED, "quote " + quoteId + " was valid until " + validUntil);
    }
    QuoteOffer chosen =
        envelope.offers().stream()
            .filter(o -> o.offerId().equals(offerId))
            .findFirst()
            .orElseThrow(
                () ->
                    new QuoteRuleException(
                        ErrorCodes.INVALID_OFFER_REFERENCE,
                        "offer " + offerId + " is not part of quote " + quoteId));
    if (chosen.offerState() != OfferState.QUOTED) {
      throw new QuoteRuleException(
          ErrorCodes.INVALID_OFFER_REFERENCE,
          "offer " + offerId + " is " + chosen.offerState() + ", not selectable");
    }
    List<QuoteOffer> marked = new ArrayList<>();
    for (QuoteOffer offer : envelope.offers()) {
      if (offer.offerId().equals(offerId)) {
        marked.add(offer.withState(OfferState.SELECTED, offer.errorSummary()));
      } else if (offer.offerState() == OfferState.INVALID) {
        marked.add(offer);
      } else {
        marked.add(offer.withState(OfferState.NOT_SELECTED, offer.errorSummary()));
      }
    }
    this.envelope =
        new QuoteEnvelope(
            envelope.requestedCover(),
            envelope.mode(),
            envelope.category(),
            envelope.members(),
            marked,
            envelope.partialErrors(),
            offerId);
    this.state = QuoteState.SELECTED;
    touch(now);
  }

  /**
   * Lazy expiry (quotation LLD §4.5): a quote in {@code QUOTED|PARTIALLY_QUOTED|SELECTED} whose
   * {@code validUntil} has passed, not held by a payment and not materialised, becomes {@code
   * EXPIRED}. No {@code Scan} exists to find it, so every touch point calls this first. Returns
   * true when the state changed and must be written.
   */
  public boolean expireIfDue(Instant now) {
    if (materializedAt != null || paymentInFlight || validUntil == null) {
      return false;
    }
    if (!state.isExpirable() || now.isBefore(validUntil)) {
      return false;
    }
    this.state = QuoteState.EXPIRED;
    touch(now);
    return true;
  }

  /**
   * {@code POST …/payment-hold} (quotation LLD §4.4): the payment {@code paymentId} holds the
   * selected offer. While held the quote cannot expire and has no {@code ttl}. A repeat by the same
   * payment changes nothing; a hold by any other payment is refused.
   *
   * @return true when the hold is new and must be written
   */
  public boolean holdForPayment(String paymentId, String offerId, Instant now) {
    requireOpen();
    requireState(QuoteState.SELECTED);
    if (offerId == null || !offerId.equals(envelope.selectedOfferId())) {
      throw new QuoteRuleException(
          ErrorCodes.INVALID_OFFER_REFERENCE,
          "offer " + offerId + " is not the selected offer of quote " + quoteId);
    }
    if (paymentId.equals(heldByPaymentId)) {
      return false;
    }
    if (paymentInFlight) {
      throw new QuoteRuleException(
          ErrorCodes.PAYMENT_ALREADY_IN_PROGRESS,
          "quote " + quoteId + " is held by another payment");
    }
    if (validUntil == null || !now.isBefore(validUntil)) {
      throw new QuoteRuleException(
          ErrorCodes.QUOTE_EXPIRED, "quote " + quoteId + " was valid until " + validUntil);
    }
    this.paymentInFlight = true;
    this.heldByPaymentId = paymentId;
    touch(now);
    return true;
  }

  /**
   * {@code POST …/payment-release}: the payment ended without {@code RECONCILED}. Only the payment
   * that holds the quote can release it; anything else (stale release, already released,
   * materialised) is a no-op, so the call and the event consumer can both repeat it.
   *
   * @return true when the hold was released and must be written
   */
  public boolean releasePaymentHold(String paymentId, Instant now) {
    if (materializedAt != null || paymentId == null || !paymentId.equals(heldByPaymentId)) {
      return false;
    }
    requireState(QuoteState.SELECTED);
    this.paymentInFlight = false;
    this.heldByPaymentId = null;
    touch(now);
    return true;
  }

  /**
   * Deprecated {@code GET …/selected}: sets {@code paymentInFlight} without a payment id
   * (schemas/20 §3.3 as first built). Kept until payment-service calls {@code payment-hold}
   * (ADR-027 §4).
   */
  public SelectedPremium markPaymentInFlight(Instant now) {
    requireOpen();
    requireState(QuoteState.SELECTED);
    if (!paymentInFlight) {
      this.paymentInFlight = true;
      touch(now);
    }
    return selectedPremium().orElseThrow();
  }

  /** Clears any hold, including a legacy one without a payment id: expiry applies again. */
  public void paymentEnded(Instant now) {
    requireOpen();
    requireState(QuoteState.SELECTED);
    this.paymentInFlight = false;
    this.heldByPaymentId = null;
    touch(now);
  }

  /** {@code SELECTED → CONVERTED} once the payment is reconciled; frozen from here on (D8). */
  public void materialize(Instant now) {
    requireOpen();
    requireState(QuoteState.SELECTED);
    Instant at = truncate(now);
    this.state = QuoteState.CONVERTED;
    this.paymentInFlight = false;
    this.heldByPaymentId = null;
    this.materializedAt = at;
    this.updatedAt = at;
  }

  public Optional<QuoteOffer> selectedOffer() {
    String id = envelope.selectedOfferId();
    if (id == null) {
      return Optional.empty();
    }
    return envelope.offers().stream().filter(o -> o.offerId().equals(id)).findFirst();
  }

  public Optional<SelectedPremium> selectedPremium() {
    return selectedOffer()
        .map(
            o ->
                new SelectedPremium(
                    quoteId,
                    o.offerId(),
                    o.premiumAmount(),
                    o.taxAmount(),
                    o.sumAssured(),
                    o.insurerCode(),
                    o.productCode(),
                    o.frequency(),
                    validUntil));
  }

  /** Called by the store after a successful write. */
  public void markPersisted(long newVersion, String keyId) {
    this.version = newVersion;
    this.encryptionKeyId = keyId;
  }

  public boolean isMaterialized() {
    return materializedAt != null;
  }

  public Snapshot snapshot() {
    return new Snapshot(
        quoteId,
        leadId,
        journeyId,
        customerId,
        suitabilityId,
        consentId,
        lob,
        productClass,
        mode,
        category,
        state,
        validUntil,
        provider,
        providerJobId,
        configVersionRef,
        paymentInFlight,
        heldByPaymentId,
        materializedAt,
        version,
        createdAt,
        updatedAt,
        envelope,
        encryptionKeyId);
  }

  public String quoteId() {
    return quoteId;
  }

  public String journeyId() {
    return journeyId;
  }

  public QuoteState state() {
    return state;
  }

  public long version() {
    return version;
  }

  public boolean paymentInFlight() {
    return paymentInFlight;
  }

  public String heldByPaymentId() {
    return heldByPaymentId;
  }

  public String leadId() {
    return leadId;
  }

  public String customerId() {
    return customerId;
  }

  public String productClass() {
    return productClass;
  }

  public String configVersionRef() {
    return configVersionRef;
  }

  public Instant validUntil() {
    return validUntil;
  }

  public Instant materializedAt() {
    return materializedAt;
  }

  public Instant updatedAt() {
    return updatedAt;
  }

  public QuoteEnvelope envelope() {
    return envelope;
  }

  private void requireOpen() {
    if (materializedAt != null) {
      throw new QuoteRuleException(
          ErrorCodes.ILLEGAL_TRANSITION,
          "quote " + quoteId + " was materialised at " + materializedAt + " and is frozen");
    }
  }

  private void requireState(QuoteState expected) {
    if (state != expected) {
      throw new QuoteRuleException(
          ErrorCodes.ILLEGAL_TRANSITION,
          "quote " + quoteId + " is " + state + ", expected " + expected);
    }
  }

  private QuoteRuleException illegal(String action) {
    return new QuoteRuleException(
        ErrorCodes.ILLEGAL_TRANSITION,
        "cannot " + action + " on quote " + quoteId + " in " + state);
  }

  private void touch(Instant now) {
    this.updatedAt = truncate(now);
  }

  // Stored as fixed-width millisecond ISO strings; truncating keeps the round trip lossless.
  static Instant truncate(Instant instant) {
    return instant.truncatedTo(ChronoUnit.MILLIS);
  }
}
