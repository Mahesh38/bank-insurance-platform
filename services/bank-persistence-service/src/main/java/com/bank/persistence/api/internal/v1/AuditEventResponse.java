package com.bank.persistence.api.internal.v1;

import java.time.Instant;

public record AuditEventResponse(
    String eventId,
    Instant eventTime,
    String action,
    String actorId,
    String actorType,
    String resourceType,
    String resourceId,
    String outcome,
    String lob,
    String journeyId,
    String distributorId,
    String agentId,
    String metadata,
    String traceId,
    String priorState,
    String newState,
    String consentRef,
    String suitabilityRef,
    Short eventSchemaVersion,
    Instant retainUntil,
    Integer sequenceNo,
    String actingCapacity,
    String actorInsurerId,
    String assistedActorId,
    String configVersionRef) {}
