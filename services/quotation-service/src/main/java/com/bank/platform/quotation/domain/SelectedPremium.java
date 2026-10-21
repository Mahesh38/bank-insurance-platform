package com.bank.platform.quotation.domain;

import java.math.BigDecimal;

/** The selected offer as Payment reads it — control attributes only ({@code INV-PAY-03}). */
public record SelectedPremium(
    String quoteId,
    String offerId,
    BigDecimal premiumAmount,
    BigDecimal taxAmount,
    BigDecimal sumAssured,
    String insurerCode,
    String productCode) {}
