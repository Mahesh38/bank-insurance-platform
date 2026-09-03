package com.bank.insurance.onesb.api.dto;

import com.bank.common.domain.PaymentStatus;

import java.time.Instant;

/**
 * Bank-facing create-payment response for {@code POST /v1/payments}.
 */
public record CreatePaymentResponse(
        String paymentSessionId,
        String paymentUrl,
        Instant expiresAt,
        PaymentStatus status
) {}
