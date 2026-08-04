package com.bank.insurance.onesb.domain.command;

import com.bank.insurance.onesb.domain.model.Lob;

import java.math.BigDecimal;

/**
 * Command for creating a 1SB payment session (FUNC-007). Bank-canonical field names only —
 * {@code distributorID} is intentionally absent: injected from {@code SecretProvider} only
 * when building the outbound 1SB payload (COMP-004 / D7), same rule as proposal submit.
 */
public record CreatePaymentCommand(
        Lob lob,
        String applicationNumber,
        String jobId,
        String insurerCode,
        String productCode,
        String redirectUrl,
        String journeyId,
        String currency,
        Payer payer,
        BigDecimal amountToBePaid,
        String premiumFrequency,
        String rmEmployeeId,
        String agentId,
        String actorId
) {
    public record Payer(
            String firstName,
            String lastName,
            String mobileNumber,
            String email
    ) {}
}
