package com.bank.persistence.api.internal.v1.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.Instant;

public record PatchJobStatusRequest(
        @NotBlank String status,
        String failureReason,
        String externalReqId,
        Instant completedAt,
        String applicationNumber
) {}
