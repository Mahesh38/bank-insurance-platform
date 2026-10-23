package com.bank.platform.quotation.domain;

import java.math.BigDecimal;

/**
 * One insurer offer. Field names follow envelope v1 (payload-storage-and-transactions §3.2), so the
 * serialised envelope is exactly the documented shape.
 */
public record QuoteOffer(
    String offerId,
    String insurerCode,
    String productCode,
    String productName,
    BigDecimal premiumAmount,
    BigDecimal taxAmount,
    BigDecimal sumAssured,
    String frequency,
    Integer coverTerm,
    Integer ppt,
    boolean outOfBound,
    OfferState offerState,
    String errorSummary) {

  /** {@code INV-QUO-03}: a non-positive premium or sum assured is never a selectable offer. */
  public boolean hasPositiveAmounts() {
    return premiumAmount != null
        && sumAssured != null
        && premiumAmount.signum() > 0
        && sumAssured.signum() > 0;
  }

  public QuoteOffer withState(OfferState state, String summary) {
    return new QuoteOffer(
        offerId,
        insurerCode,
        productCode,
        productName,
        premiumAmount,
        taxAmount,
        sumAssured,
        frequency,
        coverTerm,
        ppt,
        outOfBound,
        state,
        summary);
  }
}
