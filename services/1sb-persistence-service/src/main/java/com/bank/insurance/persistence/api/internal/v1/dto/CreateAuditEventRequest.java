package com.bank.insurance.persistence.api.internal.v1.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.Instant;

public record CreateAuditEventRequest(
        String eventId,
        Instant eventTime,
        @NotBlank String action,
        @NotBlank String actorId,
        @NotBlank String actorType,
        @NotBlank String resourceType,
        @NotBlank String resourceId,
        @NotBlank String outcome,
        String lob,
        String journeyId,
        String distributorId,
        String agentId,
        String metadata,
        String traceId
) {}
