package com.bank.insurance.onesb.domain.command;

import com.bank.common.domain.Lob;

import java.util.Map;

/**
 * Command for submitting a Term (or LOB) proposal.
 * Bank-canonical field names only — no 1SB field names in the public shape.
 * <p>
 * {@code distributorId} is intentionally absent: COMP-004 / D7 — injected from
 * {@code SecretProvider} only when building the outbound 1SB payload.
 */
public record SubmitProposalCommand(
        Lob lob,
        String schemaId,
        String offerId,
        String productCode,
        String manufacturerId,
        String version,
        Map<String, Object> values,
        String consentRef,
        String agentId,
        DistributionContext distribution,
        String journeyId,
        String sessionId,
        String idempotencyKey,
        String actorId
) {
    public record DistributionContext(
            String rmEmployeeId,
            String agentId,
            String channelType
    ) {}
}
