package com.bank.insurance.onesb.domain.model;

/**
 * Immediate result of {@code POST /v1/proposals} — bank job id + normalised status.
 */
public record ProposalSubmitResult(
        String proposalJobId,
        JobStatus status
) {}
