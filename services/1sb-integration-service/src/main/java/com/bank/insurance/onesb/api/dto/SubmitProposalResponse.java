package com.bank.insurance.onesb.api.dto;

import com.bank.common.domain.JobStatus;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 201 Created body for proposal submit — bank proposal/job id + normalised status.
 */
public record SubmitProposalResponse(
        String proposalJobId,
        JobStatus status
) {
    /** Alias for clients that expect {@code jobId} (same value as {@link #proposalJobId()}). */
    @JsonProperty("jobId")
    public String jobId() {
        return proposalJobId;
    }
}
