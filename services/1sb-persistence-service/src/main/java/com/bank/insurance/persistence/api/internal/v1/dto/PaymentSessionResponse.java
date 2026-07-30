package com.bank.insurance.persistence.api.internal.v1.dto;

import java.time.Instant;

public record PaymentSessionResponse(
        String sessionId,
        String jobId,
        String applicationNumber,
        String lob,
        String paymentUrl,
        String redirectUrl,
        String status,
        String externalTxnId,
        Instant createdAt,
        Instant expiresAt,
        Instant updatedAt,
        String createdByActor
) {}
