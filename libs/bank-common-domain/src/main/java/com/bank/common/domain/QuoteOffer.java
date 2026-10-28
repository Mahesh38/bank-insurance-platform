package com.bank.common.domain;

import java.math.BigDecimal;
import java.util.List;

/**
 * A single quote offer from an insurer, returned as part of a quote job result. Bank-facing term
 * names: {@code coverTerm} (PT), {@code premiumPayingTerm} (PPT), {@code premiumFrequency}.
 */
public record QuoteOffer(
    String offerId,
    String insurerCode,
    String insurerName,
    String productCode,
    String productName,
    BigDecimal premiumAmount,
    String premiumFrequency,
    BigDecimal sumAssured,
    boolean outOfBound,
    String offerStatus,
    String errorSummary,
    Integer coverTerm,
    Integer premiumPayingTerm,
    List<FundAllocation> funds) {
  public QuoteOffer {
    funds = funds == null ? List.of() : List.copyOf(funds);
  }

  /** Compatibility constructor used by call sites without term or fund rows. */
  public QuoteOffer(
      String offerId,
      String insurerCode,
      String insurerName,
      String productCode,
      String productName,
      BigDecimal premiumAmount,
      String premiumFrequency,
      BigDecimal sumAssured,
      boolean outOfBound,
      String offerStatus,
      String errorSummary) {
    this(
        offerId,
        insurerCode,
        insurerName,
        productCode,
        productName,
        premiumAmount,
        premiumFrequency,
        sumAssured,
        outOfBound,
        offerStatus,
        errorSummary,
        null,
        null,
        List.of());
  }

  /** Compatibility constructor with funds but without PT/PPT. */
  public QuoteOffer(
      String offerId,
      String insurerCode,
      String insurerName,
      String productCode,
      String productName,
      BigDecimal premiumAmount,
      String premiumFrequency,
      BigDecimal sumAssured,
      boolean outOfBound,
      String offerStatus,
      String errorSummary,
      List<FundAllocation> funds) {
    this(
        offerId,
        insurerCode,
        insurerName,
        productCode,
        productName,
        premiumAmount,
        premiumFrequency,
        sumAssured,
        outOfBound,
        offerStatus,
        errorSummary,
        null,
        null,
        funds);
  }
}
