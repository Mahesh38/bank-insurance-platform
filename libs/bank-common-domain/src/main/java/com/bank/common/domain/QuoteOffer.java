package com.bank.common.domain;

import java.math.BigDecimal;

/**
 * A single quote offer from an insurer, returned as part of a quote job result.
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
        String errorSummary
) {}
