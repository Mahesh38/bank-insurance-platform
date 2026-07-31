package com.bank.insurance.onesb.domain.model;

import java.time.Instant;
import java.util.List;

/**
 * Value object representing an integration job (quote or proposal) and its result.
 * Contains no Spring annotations — pure domain.
 * {@code applicationNumber} is set for completed proposal jobs when the insurer assigned one.
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
        Instant completedAt,
        String applicationNumber
) {}
