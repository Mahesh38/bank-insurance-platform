package com.bank.insurance.onesb.domain.model;

import java.time.Instant;

/**
 * Normalised 1SB payment-url outcome (adapter → application). {@code expiresAt} is computed by
 * the adapter from configured TTL — 1SB does not return one.
 */
public record OneSbPaymentUrlResult(
        String paymentUrl,
        String externalTxnId,
        Instant expiresAt
) {}
