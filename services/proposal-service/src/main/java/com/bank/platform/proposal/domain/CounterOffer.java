package com.bank.platform.proposal.domain;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * The insurer's counter-offer during pre-payment underwriting (proposal LLD §4.4); materialised to
 * {@code proposal.counter_offer} JSONB — ids, amounts and a reason code, no PII.
 */
public record CounterOffer(
    String counterOfferId,
    BigDecimal premiumAmount,
    BigDecimal sumAssured,
    String reasonCode,
    Instant expiresAt) {}
