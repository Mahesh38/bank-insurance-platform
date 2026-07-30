package com.bank.insurance.persistence.api.internal.v1.dto;

import java.time.Instant;

public record JobResponse(
        String jobId,
        String jobType,
        String lob,
        String status,
        String failureReason,
        String journeyId,
        String applicationNumber,
        String policyNumber,
        String externalReqId,
        String externalProvider,
        String idempotencyKey,
        String resultBlobId,
        Instant createdAt,
        Instant updatedAt,
        Instant completedAt,
        String ownedByInstance,
        Long version,
        String createdByActor
) {}
