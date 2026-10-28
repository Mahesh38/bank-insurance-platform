package com.bank.platform.integration.hub.domain;

import java.util.List;
import java.util.Objects;

/** Completed / partially-completed quote payload. */
public record QuoteResult(List<CanonicalOffer> offers, List<PartialError> partialErrors) {

  public QuoteResult {
    Objects.requireNonNull(offers, "offers");
    Objects.requireNonNull(partialErrors, "partialErrors");
    offers = List.copyOf(offers);
    partialErrors = List.copyOf(partialErrors);
  }

  public record PartialError(String insurerCode, String productCode, String code, String detail) {}
}
