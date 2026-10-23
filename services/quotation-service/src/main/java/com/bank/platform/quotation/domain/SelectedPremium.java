package com.bank.platform.quotation.domain;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * The selected offer as Payment reads it — control attributes only ({@code INV-PAY-03}). Amount
 * payable is {@code premiumAmount + taxAmount}.
 */
public record SelectedPremium(
    String quoteId,
    String offerId,
    BigDecimal premiumAmount,
    BigDecimal taxAmount,
    BigDecimal sumAssured,
    String insurerCode,
    String productCode,
    String premiumFrequency,
    Instant validUntil) {}
