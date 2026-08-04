package com.bank.persistence.api.internal.v1.dto;

import java.time.Instant;
import java.time.LocalDate;

/** Decrypted payload — returned only from the by-id lookup, for dispute/audit use. */
public record RawPayloadResponse(
        String payloadId,
        String jobId,
        String direction,
        String operation,
        String lob,
        String payload,
        Short httpStatus,
        Instant createdAt,
        LocalDate retainUntil
) {}
