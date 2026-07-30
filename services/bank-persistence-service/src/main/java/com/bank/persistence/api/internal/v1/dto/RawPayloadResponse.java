package com.bank.persistence.api.internal.v1.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Raw payload metadata. {@code payloadBase64} is set only on decrypt GET; create/list never echo bytes.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RawPayloadResponse(
        String payloadId,
        String jobId,
        String direction,
        String operation,
        String lob,
        String encryptionKeyId,
        Short httpStatus,
        Instant createdAt,
        LocalDate retainUntil,
        String payloadBase64
) {
    public static RawPayloadResponse metadata(
            String payloadId,
            String jobId,
            String direction,
            String operation,
            String lob,
            String encryptionKeyId,
            Short httpStatus,
            Instant createdAt,
            LocalDate retainUntil) {
        return new RawPayloadResponse(
                payloadId, jobId, direction, operation, lob,
                encryptionKeyId, httpStatus, createdAt, retainUntil, null);
    }
}
