package com.bank.insurance.onesb.api.dto;

import com.bank.insurance.onesb.domain.model.Lob;
import jakarta.validation.constraints.NotBlank;

/**
 * Bank-facing create-payment request for {@code POST /v1/payments} (FUNC-007).
 */
public record CreatePaymentRequest(
        @NotBlank String applicationNumber,
        String proposalJobId,
        String redirectUrl,
        Lob lob,
        String journeyId
) {}
