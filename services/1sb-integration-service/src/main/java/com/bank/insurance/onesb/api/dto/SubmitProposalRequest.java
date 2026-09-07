package com.bank.insurance.onesb.api.dto;

import com.bank.common.domain.Lob;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

/**
 * Bank-facing submit-proposal request for {@code POST /v1/proposals}.
 * <p>
 * No {@code distributorId} field — COMP-004: distributor is injected from secrets only.
 */
public record SubmitProposalRequest(
        @NotNull Lob lob,
        String schemaId,
        String offerId,
        String productCode,
        String manufacturerId,
        String version,
        Map<String, Object> values,
        String consentRef,
        String agentId,
        DistributionRequest distribution,
        String journeyId,
        String sessionId
) {
    public record DistributionRequest(
            String rmEmployeeId,
            String agentId,
            String channelType
    ) {}
}
