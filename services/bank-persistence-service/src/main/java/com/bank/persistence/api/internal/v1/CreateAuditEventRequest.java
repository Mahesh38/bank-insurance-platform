package com.bank.persistence.api.internal.v1;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/**
 * One audit row. Fields after {@code traceId} are the V3 reconstruction columns
 * (schemas/14-audit_event_delta.sql); {@code actingCapacity} carries INV-ACT-04.
 */
public record CreateAuditEventRequest(
    @Size(max = 36) String eventId,
    Instant eventTime,
    @NotBlank @Size(max = 100) String action,
    @NotBlank @Size(max = 100) String actorId,
    @NotBlank @Size(max = 20) String actorType,
    @NotBlank @Size(max = 50) String resourceType,
    @NotBlank @Size(max = 100) String resourceId,
    @NotBlank @Size(max = 20) String outcome,
    @Size(max = 20) String lob,
    @Size(max = 36) String journeyId,
    @Size(max = 50) String distributorId,
    @Size(max = 50) String agentId,
    String metadata,
    @Size(max = 64) String traceId,
    String priorState,
    String newState,
    @Size(max = 36) String consentRef,
    @Size(max = 36) String suitabilityRef,
    @Min(1) Short eventSchemaVersion,
    Instant retainUntil,
    @Min(0) Integer sequenceNo,
    @Pattern(regexp = "SP_ACCOUNTABLE|ASSIST_ONLY") String actingCapacity,
    @Size(max = 64) String actorInsurerId,
    @Size(max = 64) String assistedActorId,
    @Size(max = 64) String configVersionRef) {}
