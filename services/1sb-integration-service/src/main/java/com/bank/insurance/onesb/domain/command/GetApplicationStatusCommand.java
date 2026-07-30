package com.bank.insurance.onesb.domain.command;

/**
 * Command for {@code GET /v1/status/{applicationNumber}} (FUNC-009).
 */
public record GetApplicationStatusCommand(
        String applicationNumber,
        String lob,
        String insuranceCompanyCode,
        String policyNo,
        String actorId,
        String journeyId
) {}
