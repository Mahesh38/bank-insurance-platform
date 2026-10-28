package com.bank.platform.quotation.domain;

import java.math.BigDecimal;
import java.util.List;

/**
 * Integration Hub quote submit/poll (S-09 / S-11). Quotation maps once Quotation envelope ↔ Hub
 * canonical; Hub owns Hub ↔ 1SB. No fourth mega-DTO.
 */
public interface HubQuotePort {

  /**
   * {@code POST /internal/v1/provider-requests} with {@code Idempotency-Key =
   * quote:{quoteId}:submit}.
   */
  Accepted submitQuote(String quoteId, NewQuote request, String actorId, String correlationId);

  /** {@code POST /internal/v1/provider-requests/{id}} — poll advances Hub state. */
  PollStatus poll(String providerRequestId);

  record Accepted(String providerRequestId, String provider, String state) {}

  record PollStatus(
      String providerRequestId,
      String state,
      String provider,
      List<HubOffer> offers,
      List<HubPartialError> partialErrors) {

    public PollStatus {
      offers = offers == null ? List.of() : List.copyOf(offers);
      partialErrors = partialErrors == null ? List.of() : List.copyOf(partialErrors);
    }

    public boolean terminal() {
      return "COMPLETED".equals(state)
          || "PARTIALLY_COMPLETED".equals(state)
          || "FAILED".equals(state)
          || "TIMED_OUT".equals(state);
    }
  }

  /** Hub-canonical offer fields needed to build a quotation envelope offer. */
  record HubOffer(
      String providerOfferRef,
      String insurerCode,
      String productCode,
      String productName,
      BigDecimal premiumAmount,
      String premiumFrequency,
      BigDecimal sumAssured,
      boolean outOfBound,
      Integer coverTerm,
      Integer premiumPayingTerm) {}

  record HubPartialError(String insurerCode, String code) {}
}
