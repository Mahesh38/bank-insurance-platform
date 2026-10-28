package com.bank.platform.quotation.application;

import com.bank.platform.quotation.domain.HubQuotePort;
import com.bank.platform.quotation.domain.HubQuotePort.HubOffer;
import com.bank.platform.quotation.domain.HubQuotePort.HubPartialError;
import com.bank.platform.quotation.domain.HubQuotePort.PollStatus;
import com.bank.platform.quotation.domain.InsurerError;
import com.bank.platform.quotation.domain.OfferState;
import com.bank.platform.quotation.domain.QuoteOffer;
import com.bank.platform.quotation.domain.QuoteState;
import com.bank.platform.quotation.domain.Ulid;
import com.bank.platform.quotation.domain.WorkingQuote;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * FUNC-033 — getQuote: load working quote; while {@code IN_PROGRESS} poll Hub once and ingest
 * terminal offers (frequency ← premiumFrequency, coverTerm, ppt ← premiumPayingTerm).
 */
@Service
public class GetQuoteService {

  /** Interim validity until QUO-2 names the rule set (fail-closed at go-live). */
  static final Duration OFFER_VALIDITY = Duration.ofDays(7);

  private final QuoteWorkingStateService quotes;
  private final HubQuotePort hub;
  private final Clock clock;

  public GetQuoteService(QuoteWorkingStateService quotes, HubQuotePort hub, Clock clock) {
    this.quotes = quotes;
    this.hub = hub;
    this.clock = clock;
  }

  public WorkingQuote get(String quoteId) {
    WorkingQuote quote = quotes.get(quoteId);
    if (quote.state() != QuoteState.IN_PROGRESS) {
      return quote;
    }
    String providerRequestId = quote.snapshot().providerJobId();
    if (providerRequestId == null || providerRequestId.isBlank()) {
      return quote;
    }
    PollStatus status = hub.poll(providerRequestId);
    if (!status.terminal()) {
      return quote;
    }
    List<QuoteOffer> offers = mapOffers(status.offers());
    List<InsurerError> failures = mapErrors(status.partialErrors());
    Instant validUntil = status.offers().isEmpty() ? null : clock.instant().plus(OFFER_VALIDITY);
    return quotes.recordOffers(quoteId, offers, failures, validUntil);
  }

  static List<QuoteOffer> mapOffers(List<HubOffer> hubOffers) {
    List<QuoteOffer> offers = new ArrayList<>();
    for (HubOffer hub : hubOffers) {
      offers.add(
          new QuoteOffer(
              bankOfferId(hub.providerOfferRef()),
              hub.insurerCode(),
              hub.productCode(),
              hub.productName() == null ? "" : hub.productName(),
              hub.premiumAmount(),
              BigDecimal.ZERO,
              hub.sumAssured(),
              hub.premiumFrequency(),
              hub.coverTerm(),
              hub.premiumPayingTerm(),
              hub.outOfBound(),
              OfferState.QUOTED,
              null));
    }
    return offers;
  }

  private static List<InsurerError> mapErrors(List<HubPartialError> partialErrors) {
    List<InsurerError> errors = new ArrayList<>();
    for (HubPartialError error : partialErrors) {
      errors.add(new InsurerError(error.insurerCode(), error.code()));
    }
    return errors;
  }

  /**
   * Bank-minted offer id. Reuses a Hub {@code providerOfferRef} when it is already a ULID;
   * otherwise mints one so the envelope stays bank-canonical.
   */
  private static String bankOfferId(String providerOfferRef) {
    if (providerOfferRef != null && Ulid.isValid(providerOfferRef)) {
      return providerOfferRef.toUpperCase();
    }
    return Ulid.mint();
  }
}
