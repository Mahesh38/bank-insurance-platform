package com.bank.insurance.onesb.api.dto;

import com.bank.common.domain.BankApplicationStatus;

import java.time.Instant;

/**
 * Bank-facing response for {@code GET /v1/status/{applicationNumber}}. Raw 1SB status text is
 * intentionally omitted (compliance §8.1) — only the normalised {@code bankStatus} and the
 * manufacturer sub-status (for RM display) are surfaced.
 */
public record ApplicationStatusResponse(
        String applicationNumber,
        BankApplicationStatus bankStatus,
        String manufacturerSubStatus,
        String policyNumber,
        Instant lastUpdated
) {}
