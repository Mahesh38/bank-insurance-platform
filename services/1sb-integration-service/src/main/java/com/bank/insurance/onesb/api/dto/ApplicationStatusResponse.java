package com.bank.insurance.onesb.api.dto;

import java.time.Instant;

/**
 * Bank-facing application status response for {@code GET /v1/status/{applicationNumber}} (FUNC-009).
 * Does not expose raw 1SB {@code onesbStatus} / {@code applicationStatus}.
 */
public record ApplicationStatusResponse(
        String applicationNumber,
        String bankStatus,
        String manufacturerAppStatus,
        String manufacturerAppStatusDesc,
        String policyNumber,
        Instant lastUpdated
) {}
