package com.bank.insurance.onesb.api.dto;

import java.time.Instant;

/**
 * Bank-facing create-payment response for {@code POST /v1/payments} (FUNC-007).
 */
public record CreatePaymentResponse(
        String paymentRef,
        String paymentUrl,
        Instant expiresAt
) {}
