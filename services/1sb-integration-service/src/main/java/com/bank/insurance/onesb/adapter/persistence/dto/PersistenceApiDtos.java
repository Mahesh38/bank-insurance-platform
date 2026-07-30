package com.bank.insurance.onesb.adapter.persistence.dto;

import java.time.Instant;
import java.util.List;

/** Request/response shapes matching 1sb-persistence-service /internal/v1 contract. */
public final class PersistenceApiDtos {

    private PersistenceApiDtos() {}

    public record CreateJobRequest(
            String lob,
            String jobType,
            String journeyId,
            String idempotencyKey,
            String createdByActor
    ) {}

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

    public record PatchJobStatusRequest(
            String status,
            String failureReason,
            String externalReqId,
            Instant completedAt
    ) {}

    public record CreateOfferRequest(
            String offerId,
            String insurerCode,
            String productCode,
            String productName,
            java.math.BigDecimal premiumAmount,
            String premiumFrequency,
            java.math.BigDecimal sumAssured,
            Boolean outOfBound,
            String offerStatus,
            String errorSummary,
            String rawOfferBlobId
    ) {}

    public record OfferResponse(
            String offerId,
            String jobId,
            String insurerCode,
            String productCode,
            String productName,
            java.math.BigDecimal premiumAmount,
            String premiumFrequency,
            java.math.BigDecimal sumAssured,
            Boolean outOfBound,
            String offerStatus,
            String errorSummary,
            String rawOfferBlobId,
            Instant createdAt
    ) {}
}
