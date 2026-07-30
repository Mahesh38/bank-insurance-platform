package com.bank.insurance.onesb.domain.model;

import java.time.Instant;

/**
 * Application status snapshot from 1SB prostat (FUNC-009).
 * {@code onesbStatus} is retained for audit / internal use and must not be exposed on the API.
 */
public record ApplicationStatus(
        String applicationNumber,
        BankStage bankStatus,
        String onesbStatus,
        String manufacturerAppStatus,
        String manufacturerAppStatusDesc,
        String policyNumber,
        String insuranceCompanyCode,
        Lob lob,
        Instant lastUpdated
) {}
