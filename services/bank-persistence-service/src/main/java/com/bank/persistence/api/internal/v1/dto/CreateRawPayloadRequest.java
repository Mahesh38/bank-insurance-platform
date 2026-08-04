package com.bank.persistence.api.internal.v1.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;

/**
 * {@code payload} is the plaintext raw 1SB request/response body; the server encrypts it
 * (AES-256-GCM) before it ever reaches the database — see
 * {@link com.bank.persistence.crypto.RawPayloadEncryptionService}. Never logged.
 */
public record CreateRawPayloadRequest(
        @NotBlank String jobId,
        @NotBlank @Pattern(regexp = "REQ|RES") String direction,
        @NotBlank String operation,
        @NotBlank String lob,
        @NotBlank String payload,
        Short httpStatus,
        LocalDate retainUntil
) {}
