package com.bank.platform.proposal.domain;

import com.bank.common.error.ErrorCodes;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The proposal while the sale is open: plaintext control attributes (schemas/20 §2) plus the
 * decrypted envelope. Frozen once {@link #materializedAt()} is set (DB-DEC-0005).
 */
public final class WorkingProposal {

  /** Everything the store persists. {@code encryptionKeyId} is null until first written. */
  public record Snapshot(
      String proposalId,
      String leadId,
      String journeyId,
      String customerId,
      String quoteId,
      String offerId,
      String consentId,
      String lob,
      String applicationNumber,
      String schemaId,
      String schemaVersion,
      ProposalState state,
      String underwritingState,
      String issuanceMode,
      String configVersionRef,
      boolean paymentInFlight,
      String heldByPaymentId,
      Instant materializedAt,
      long version,
      Instant createdAt,
      Instant updatedAt,
      ProposalEnvelope envelope,
      String encryptionKeyId) {}

  private final String proposalId;
  private final String leadId;
  private final String journeyId;
  private final String customerId;
  private final String quoteId;
  private final String offerId;
  private final String consentId;
  private final String lob;
  private final String schemaId;
  private final String schemaVersion;
  private final String issuanceMode;
  private final String configVersionRef;
  private final Instant createdAt;
  private String applicationNumber;
  private ProposalState state;
  private String underwritingState;
  private boolean paymentInFlight;
  private String heldByPaymentId;
  private Instant materializedAt;
  private long version;
  private Instant updatedAt;
  private ProposalEnvelope envelope;
  private String encryptionKeyId;
  // Set by acknowledge until the store has claimed the APPNO marker for it.
  private String unclaimedApplicationNumber;

  private WorkingProposal(Snapshot s) {
    this.proposalId = s.proposalId();
    this.leadId = s.leadId();
    this.journeyId = s.journeyId();
    this.customerId = s.customerId();
    this.quoteId = s.quoteId();
    this.offerId = s.offerId();
    this.consentId = s.consentId();
    this.lob = s.lob();
    this.schemaId = s.schemaId();
    this.schemaVersion = s.schemaVersion();
    this.issuanceMode = s.issuanceMode();
    this.configVersionRef = s.configVersionRef();
    this.createdAt = s.createdAt();
    this.applicationNumber = s.applicationNumber();
    this.state = s.state();
    this.underwritingState = s.underwritingState();
    this.paymentInFlight = s.paymentInFlight();
    this.heldByPaymentId = s.heldByPaymentId();
    this.materializedAt = s.materializedAt();
    this.version = s.version();
    this.updatedAt = s.updatedAt();
    this.envelope = s.envelope();
    this.encryptionKeyId = s.encryptionKeyId();
  }

  /** A {@code DRAFT} with an empty envelope; version 0 until written. */
  public static WorkingProposal draft(String proposalId, NewProposal request, Instant now) {
    Instant at = truncate(now);
    return new WorkingProposal(
        new Snapshot(
            proposalId,
            request.leadId(),
            request.journeyId(),
            request.customerId(),
            request.quoteId(),
            request.offerId(),
            request.consentId(),
            request.lob(),
            null,
            request.schemaId(),
            request.schemaVersion(),
            ProposalState.DRAFT,
            null,
            request.issuanceMode(),
            request.configVersionRef(),
            false,
            null,
            null,
            0,
            at,
            at,
            ProposalEnvelope.empty(),
            null));
  }

  public static WorkingProposal restore(Snapshot snapshot) {
    return new WorkingProposal(snapshot);
  }

  /**
   * Page-by-page form save; answers and nominee only ever enter the envelope (INV-PRP-05). Each
   * answer carries the question text and schema version shown (D8): a re-answered question keeps
   * its place, a new one is appended, so the envelope order is the order shown.
   */
  public void saveAnswers(List<ProposalAnswer> answers, Nominee nominee, Instant now) {
    requireOpen();
    requireState(ProposalState.DRAFT);
    Map<String, ProposalAnswer> merged = new LinkedHashMap<>();
    envelope.answers().forEach(a -> merged.put(a.questionId(), a));
    for (ProposalAnswer answer : answers == null ? List.<ProposalAnswer>of() : answers) {
      if (answer.questionId() == null || answer.questionId().isBlank()) {
        throw new ProposalRuleException(ErrorCodes.VALIDATION_ERROR, "questionId is required");
      }
      if (!answer.isEvidenceComplete()) {
        throw new ProposalRuleException(
            ErrorCodes.VALIDATION_ERROR,
            "answer to " + answer.questionId() + " needs the question text and schema version shown");
      }
      merged.put(answer.questionId(), answer);
    }
    this.envelope =
        new ProposalEnvelope(
            List.copyOf(merged.values()),
            nominee == null ? envelope.nominee() : nominee,
            envelope.requirements(),
            envelope.documents(),
            envelope.counterOffer());
    touch(now);
  }

  /** {@code DRAFT → SUBMITTED}, after the C2 consent gate. */
  public void submit(Instant now) {
    requireOpen();
    requireState(ProposalState.DRAFT);
    this.state = ProposalState.SUBMITTED;
    touch(now);
  }

  /** The insurer acknowledged with an application number: {@code SUBMITTED → UNDER_WRITING}. */
  public void acknowledge(String number, Instant now) {
    requireOpen();
    requireState(ProposalState.SUBMITTED);
    if (number == null || number.isBlank()) {
      throw new ProposalRuleException(
          ErrorCodes.MISSING_REQUIRED_FIELD, "application number is required");
    }
    this.applicationNumber = number;
    this.unclaimedApplicationNumber = number;
    this.state = ProposalState.UNDER_WRITING;
    touch(now);
  }

  /** Insurer underwriting status sync (TI-03): the bank tracks, the insurer decides. */
  public void recordUnderwriting(
      ProposalState next,
      String insurerUnderwritingState,
      List<UwRequirement> requirements,
      List<DocumentRef> documents,
      Instant now) {
    recordUnderwriting(next, insurerUnderwritingState, requirements, documents, null, now);
  }

  /**
   * As above; a {@code UW_COUNTER_OFFER} carries the insurer's counter-offer, kept in the envelope
   * and materialised to {@code counter_offer} (LLD §4.4).
   */
  public void recordUnderwriting(
      ProposalState next,
      String insurerUnderwritingState,
      List<UwRequirement> requirements,
      List<DocumentRef> documents,
      CounterOffer counterOffer,
      Instant now) {
    requireOpen();
    if (state != ProposalState.UNDER_WRITING && state != ProposalState.REQUIREMENTS_PENDING) {
      throw illegal("record underwriting");
    }
    if (next == null || !next.isUnderwritingOutcome()) {
      throw illegal("move to " + next + " from underwriting");
    }
    this.state = next;
    this.underwritingState = insurerUnderwritingState;
    this.envelope =
        new ProposalEnvelope(
            envelope.answers(),
            envelope.nominee(),
            requirements == null ? envelope.requirements() : requirements,
            documents == null ? envelope.documents() : documents,
            counterOffer == null ? envelope.counterOffer() : counterOffer);
    touch(now);
  }

  /** {@code UW_APPROVED → AWAITING_PAYMENT}. */
  public void awaitPayment(Instant now) {
    requireOpen();
    requireState(ProposalState.UW_APPROVED);
    this.state = ProposalState.AWAITING_PAYMENT;
    touch(now);
  }

  /**
   * {@code POST …/payment-hold} (LLD §4.5): payable only in {@code AWAITING_PAYMENT}, which E1
   * makes reachable only from {@code UW_APPROVED}. The same payment again changes nothing; any
   * other payment is refused while held.
   *
   * @return true when the hold is new and must be written
   */
  public boolean holdForPayment(String paymentId, Instant now) {
    requireOpen();
    if (state != ProposalState.AWAITING_PAYMENT) {
      throw new ProposalRuleException(
          ErrorCodes.PROPOSAL_NOT_PAYABLE,
          "proposal " + proposalId + " is " + state + ", not AWAITING_PAYMENT");
    }
    if (paymentId.equals(heldByPaymentId)) {
      return false;
    }
    if (paymentInFlight) {
      throw new ProposalRuleException(
          ErrorCodes.PAYMENT_ALREADY_IN_PROGRESS,
          "proposal " + proposalId + " is held by another payment");
    }
    this.paymentInFlight = true;
    this.heldByPaymentId = paymentId;
    touch(now);
    return true;
  }

  /**
   * {@code POST …/payment-release}: only the holding payment releases; a stale or repeated release
   * and a materialised proposal are no-ops. The proposal stays {@code AWAITING_PAYMENT}.
   *
   * @return true when the hold was released and must be written
   */
  public boolean releasePaymentHold(String paymentId, Instant now) {
    if (materializedAt != null || paymentId == null || !paymentId.equals(heldByPaymentId)) {
      return false;
    }
    requireState(ProposalState.AWAITING_PAYMENT);
    this.paymentInFlight = false;
    this.heldByPaymentId = null;
    touch(now);
    return true;
  }

  /** Payment started without a payment id (as first built): no expiry while money moves. */
  public void markPaymentInFlight(Instant now) {
    requireOpen();
    requireState(ProposalState.AWAITING_PAYMENT);
    this.paymentInFlight = true;
    touch(now);
  }

  /** Clears any hold: expiry applies again. */
  public void paymentEnded(Instant now) {
    requireOpen();
    requireState(ProposalState.AWAITING_PAYMENT);
    this.paymentInFlight = false;
    this.heldByPaymentId = null;
    touch(now);
  }

  /** INV-PRP-04: withdrawal is refused at or after {@code AWAITING_PAYMENT}. */
  public void withdraw(Instant now) {
    requireOpen();
    if (state == ProposalState.AWAITING_PAYMENT
        || state == ProposalState.PAID
        || state == ProposalState.CONVERTED
        || state.endsWithoutSale()) {
      throw new ProposalRuleException(
          ErrorCodes.WITHDRAWAL_NOT_PERMITTED,
          "proposal " + proposalId + " is " + state + " and cannot be withdrawn");
    }
    this.state = ProposalState.WITHDRAWN;
    touch(now);
  }

  /** {@code AWAITING_PAYMENT → PAID} on {@code PaymentReconciled}; frozen from here on. */
  public void materialize(Instant now) {
    requireOpen();
    requireState(ProposalState.AWAITING_PAYMENT);
    Instant at = truncate(now);
    this.state = ProposalState.PAID;
    this.paymentInFlight = false;
    this.heldByPaymentId = null;
    this.materializedAt = at;
    this.updatedAt = at;
  }

  /** The application number whose uniqueness marker the next write must claim, if any. */
  public String unclaimedApplicationNumber() {
    return unclaimedApplicationNumber;
  }

  /** Called by the store after a successful write. */
  public void markPersisted(long newVersion, String keyId) {
    this.version = newVersion;
    this.encryptionKeyId = keyId;
    this.unclaimedApplicationNumber = null;
  }

  public boolean isMaterialized() {
    return materializedAt != null;
  }

  public Snapshot snapshot() {
    return new Snapshot(
        proposalId,
        leadId,
        journeyId,
        customerId,
        quoteId,
        offerId,
        consentId,
        lob,
        applicationNumber,
        schemaId,
        schemaVersion,
        state,
        underwritingState,
        issuanceMode,
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

  public String proposalId() {
    return proposalId;
  }

  public String journeyId() {
    return journeyId;
  }

  public ProposalState state() {
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

  public String quoteId() {
    return quoteId;
  }

  public String offerId() {
    return offerId;
  }

  public String lob() {
    return lob;
  }

  public String issuanceMode() {
    return issuanceMode;
  }

  public String configVersionRef() {
    return configVersionRef;
  }

  public String underwritingState() {
    return underwritingState;
  }

  public Instant materializedAt() {
    return materializedAt;
  }

  public Instant updatedAt() {
    return updatedAt;
  }

  public String applicationNumber() {
    return applicationNumber;
  }

  public ProposalEnvelope envelope() {
    return envelope;
  }

  private void requireOpen() {
    if (materializedAt != null) {
      throw new ProposalRuleException(
          ErrorCodes.ILLEGAL_TRANSITION,
          "proposal " + proposalId + " was materialised at " + materializedAt + " and is frozen");
    }
  }

  private void requireState(ProposalState expected) {
    if (state != expected) {
      throw new ProposalRuleException(
          ErrorCodes.ILLEGAL_TRANSITION,
          "proposal " + proposalId + " is " + state + ", expected " + expected);
    }
  }

  private ProposalRuleException illegal(String action) {
    return new ProposalRuleException(
        ErrorCodes.ILLEGAL_TRANSITION,
        "cannot " + action + " on proposal " + proposalId + " in " + state);
  }

  private void touch(Instant now) {
    this.updatedAt = truncate(now);
  }

  static Instant truncate(Instant instant) {
    return instant.truncatedTo(ChronoUnit.MILLIS);
  }
}
