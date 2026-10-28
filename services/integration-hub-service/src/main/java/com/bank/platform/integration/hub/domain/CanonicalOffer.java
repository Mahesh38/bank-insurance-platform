package com.bank.platform.integration.hub.domain;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/** Bank-canonical offer — no 1SB reqId. */
public record CanonicalOffer(
    String providerOfferRef,
    String insurerCode,
    String insurerName,
    String productCode,
    String productName,
    BigDecimal premiumAmount,
    String premiumFrequency,
    BigDecimal sumAssured,
    boolean outOfBound,
    Integer coverTerm,
    Integer premiumPayingTerm,
    List<FundAllocation> funds) {

  public CanonicalOffer {
    Objects.requireNonNull(providerOfferRef, "providerOfferRef");
    Objects.requireNonNull(insurerCode, "insurerCode");
    Objects.requireNonNull(productCode, "productCode");
    funds = funds == null ? List.of() : List.copyOf(funds);
  }

  public record FundAllocation(String code, String name, BigDecimal allocationPercent) {}
}
