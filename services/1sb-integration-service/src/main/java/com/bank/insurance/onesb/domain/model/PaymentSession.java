package com.bank.insurance.onesb.domain.model;

import java.time.Instant;

/**
 * Represents a payment session created for an insurance application.
 */
public record PaymentSession(
        String sessionId,
        String jobId,
        String applicationNumber,
        Lob lob,
        String paymentUrl,
        String redirectUrl,
        PaymentStatus status,
        String externalTxnId,
        Instant createdAt,
        Instant expiresAt
) {}
