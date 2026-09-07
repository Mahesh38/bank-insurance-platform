package com.bank.insurance.onesb.api.dto;

import com.bank.common.domain.Lob;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Bank-facing create-payment request body for {@code POST /v1/payments}.
 */
public record CreatePaymentRequest(
        @NotNull Lob lob,
        @NotBlank String applicationNumber,
        String jobId,
        @NotBlank String insurerCode,
        @NotBlank String productCode,
        @NotBlank String redirectUrl,
        String journeyId,
        String currency,
        @NotNull @Valid Payer payer,
        @NotNull BigDecimal amountToBePaid,
        @NotBlank String premiumFrequency,
        DistributionRequest distribution
) {
    public record Payer(
            @NotBlank String firstName,
            @NotBlank String lastName,
            @NotBlank String mobileNumber,
            @NotBlank String email
    ) {}

    public record DistributionRequest(
            String rmEmployeeId,
            String agentId
    ) {}
}
