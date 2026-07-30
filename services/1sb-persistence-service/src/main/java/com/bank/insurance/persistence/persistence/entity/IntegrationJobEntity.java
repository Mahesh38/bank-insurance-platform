package com.bank.insurance.persistence.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "integration_job")
@Getter
@Setter
@NoArgsConstructor
public class IntegrationJobEntity {

    @Id
    @Column(name = "job_id", length = 36, nullable = false)
    private String jobId;

    @Column(name = "job_type", length = 20, nullable = false)
    private String jobType;

    @Column(name = "lob", length = 20, nullable = false)
    private String lob;

    @Column(name = "status", length = 20, nullable = false)
    private String status;

    @Column(name = "failure_reason", length = 100)
    private String failureReason;

    @Column(name = "journey_id", length = 36)
    private String journeyId;

    @Column(name = "application_number", length = 50)
    private String applicationNumber;

    @Column(name = "policy_number", length = 50)
    private String policyNumber;

    @Column(name = "external_req_id", length = 100)
    private String externalReqId;

    @Column(name = "external_provider", length = 20, nullable = false)
    private String externalProvider = "ONE_SB";

    @Column(name = "idempotency_key", length = 128, unique = true)
    private String idempotencyKey;

    @Column(name = "result_blob_id", length = 36)
    private String resultBlobId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "owned_by_instance", length = 100)
    private String ownedByInstance;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    @Column(name = "created_by_actor", length = 100, nullable = false)
    private String createdByActor;
}
