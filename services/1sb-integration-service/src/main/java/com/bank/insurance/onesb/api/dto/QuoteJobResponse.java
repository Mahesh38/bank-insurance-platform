package com.bank.insurance.onesb.api.dto;

import com.bank.insurance.onesb.domain.model.JobStatus;
import com.bank.insurance.onesb.domain.model.QuoteOffer;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * GET {@code /v1/quotes/{jobId}} response (FUNC-003).
 * {@code offers} is never null — empty while in-progress or terminal without results.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record QuoteJobResponse(
        String jobId,
        JobStatus status,
        String failureReason,
        List<QuoteOffer> offers
) {}
