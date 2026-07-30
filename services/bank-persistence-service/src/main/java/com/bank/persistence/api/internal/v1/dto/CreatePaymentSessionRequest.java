package com.bank.persistence.api.internal.v1.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.Instant;

public record CreatePaymentSessionRequest(
        String sessionId,
        String jobId,
        @NotBlank String applicationNumber,
        @NotBlank String lob,
        @NotBlank String paymentUrl,
        @NotBlank String redirectUrl,
        @NotBlank String status,
        String externalTxnId,
        Instant expiresAt,
        @NotBlank String createdByActor
) {}
