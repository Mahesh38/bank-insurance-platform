package com.bank.insurance.onesb.api.dto;

import com.bank.insurance.onesb.domain.model.Lob;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Bank-facing create-quote request body for {@code POST /v1/quotes}.
 */
public record CreateQuoteRequest(
        @NotNull Lob lob,
        String mode,
        String category,
        @NotNull BigDecimal sumAssured,
        BigDecimal premiumAmount,
        @NotEmpty @Valid List<MemberRequest> members,
        Map<String, Object> preferences,
        DistributionRequest distribution,
        String journeyId,
        String sessionId
) {
    public record MemberRequest(
            String role,
            Integer sequenceNumber,
            @NotNull String dob,
            @NotNull String gender,
            Boolean tobacco,
            BigDecimal annualIncome,
            String pincode
    ) {}

    public record DistributionRequest(
            String rmEmployeeId,
            String agentId,
            String channelType
    ) {}
}
