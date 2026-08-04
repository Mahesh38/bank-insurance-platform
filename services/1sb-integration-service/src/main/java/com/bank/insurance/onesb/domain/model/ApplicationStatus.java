package com.bank.insurance.onesb.domain.model;

import java.time.Instant;

/**
 * Bank-canonical application status (FUNC-009). {@code rawStatus} is retained for audit only —
 * never surfaced in the bank-facing API response (compliance §8.1).
 */
public record ApplicationStatus(
        String applicationNumber,
        BankApplicationStatus bankStatus,
        String rawStatus,
        String manufacturerSubStatus,
        String policyNumber,
        Instant lastUpdated
) {}
