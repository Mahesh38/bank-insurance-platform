package com.bank.insurance.onesb.api.dto;

import com.bank.common.domain.JobStatus;
import com.bank.common.domain.QuoteOffer;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * GET {@code /v1/proposals/{jobId}} response (FUNC-006).
 * {@code applicationNumber} omitted while in-progress; {@code offers} is never null (empty for proposals).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProposalJobResponse(
        String jobId,
        JobStatus status,
        String applicationNumber,
        String failureReason,
        List<QuoteOffer> offers
) {}
