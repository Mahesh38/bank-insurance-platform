package com.bank.persistence.api.internal.v1.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateRawPayloadRequest(
        @NotBlank String jobId,
        @NotBlank @Pattern(regexp = "REQ|RES", message = "direction must be REQ or RES") String direction,
        @NotBlank String operation,
        @NotBlank String lob,
        @NotBlank String payloadBase64,
        Short httpStatus
) {}
