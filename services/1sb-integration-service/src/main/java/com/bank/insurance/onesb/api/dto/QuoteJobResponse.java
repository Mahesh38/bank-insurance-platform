package com.bank.insurance.onesb.api.dto;

import com.bank.insurance.onesb.domain.model.JobStatus;
import com.bank.insurance.onesb.domain.model.Lob;
import com.bank.insurance.onesb.domain.model.QuoteOffer;

import java.time.Instant;
import java.util.List;

/** Thin GET result for AC readiness (FUNC-003 will harden). */
public record QuoteJobResponse(
        String jobId,
        JobStatus status,
        Lob lob,
        String journeyId,
        List<QuoteOffer> offers,
        List<String> partialErrors,
        Instant createdAt,
        Instant completedAt
) {}
