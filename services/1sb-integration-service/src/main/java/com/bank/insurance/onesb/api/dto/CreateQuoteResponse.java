package com.bank.insurance.onesb.api.dto;

import com.bank.common.domain.JobStatus;

/** 202 Accepted body for quote create — bank {@code jobId} only (never raw 1SB reqId). */
public record CreateQuoteResponse(
        String jobId,
        JobStatus status
) {}
