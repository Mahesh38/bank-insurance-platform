package com.bank.insurance.onesb.domain.model;

import java.time.Instant;
import java.util.List;

/**
 * Value object representing a quote job and its result.
 * Contains no Spring annotations — pure domain.
 */
public record QuoteJob(
        String jobId,
        JobStatus status,
        String failureReason,
        Lob lob,
        String journeyId,
        List<QuoteOffer> offers,
        List<String> partialErrors,
        Instant createdAt,
        Instant completedAt
) {}
