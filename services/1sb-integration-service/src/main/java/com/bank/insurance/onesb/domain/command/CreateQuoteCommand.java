package com.bank.insurance.onesb.domain.command;

import com.bank.common.domain.Lob;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Command object for creating a new quote job.
 * Bank-canonical field names only — no 1SB field names.
 */
public record CreateQuoteCommand(
        Lob lob,
        String mode,
        String category,
        BigDecimal sumAssured,
        BigDecimal premiumAmount,
        List<MemberDetail> members,
        Map<String, Object> preferences,
        DistributionContext distribution,
        String journeyId,
        String sessionId,
        String idempotencyKey,
        String actorId
) {
    public record MemberDetail(
            String role,
            int sequenceNumber,
            String dob,
            String gender,
            boolean tobacco,
            BigDecimal annualIncome,
            String pincode
    ) {}

    public record DistributionContext(
            String rmEmployeeId,
            String agentId,
            String channelType
    ) {}
}
