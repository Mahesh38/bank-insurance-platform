package com.bank.persistence.api.internal.v1.dto;

import java.time.Instant;
import java.time.LocalDate;

/** Metadata only — no decrypted plaintext — used for listing a job's captured payloads. */
public record RawPayloadSummaryResponse(
        String payloadId,
        String jobId,
        String direction,
        String operation,
        String lob,
        Short httpStatus,
        Instant createdAt,
        LocalDate retainUntil
) {}
