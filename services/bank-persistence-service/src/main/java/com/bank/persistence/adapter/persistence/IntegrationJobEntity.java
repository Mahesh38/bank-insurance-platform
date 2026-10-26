package com.bank.persistence.adapter.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;

@Entity
@Table(name = "integration_job")
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

  public IntegrationJobEntity() {}

  public String getJobId() {
    return jobId;
  }

  public void setJobId(String jobId) {
    this.jobId = jobId;
  }

  public String getJobType() {
    return jobType;
  }

  public void setJobType(String jobType) {
    this.jobType = jobType;
  }

  public String getLob() {
    return lob;
  }

  public void setLob(String lob) {
    this.lob = lob;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getFailureReason() {
    return failureReason;
  }

  public void setFailureReason(String failureReason) {
    this.failureReason = failureReason;
  }

  public String getJourneyId() {
    return journeyId;
  }

  public void setJourneyId(String journeyId) {
    this.journeyId = journeyId;
  }

  public String getApplicationNumber() {
    return applicationNumber;
  }

  public void setApplicationNumber(String applicationNumber) {
    this.applicationNumber = applicationNumber;
  }

  public String getPolicyNumber() {
    return policyNumber;
  }

  public void setPolicyNumber(String policyNumber) {
    this.policyNumber = policyNumber;
  }

  public String getExternalReqId() {
    return externalReqId;
  }

  public void setExternalReqId(String externalReqId) {
    this.externalReqId = externalReqId;
  }

  public String getExternalProvider() {
    return externalProvider;
  }

  public void setExternalProvider(String externalProvider) {
    this.externalProvider = externalProvider;
  }

  public String getIdempotencyKey() {
    return idempotencyKey;
  }

  public void setIdempotencyKey(String idempotencyKey) {
    this.idempotencyKey = idempotencyKey;
  }

  public String getResultBlobId() {
    return resultBlobId;
  }

  public void setResultBlobId(String resultBlobId) {
    this.resultBlobId = resultBlobId;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }

  public Instant getCompletedAt() {
    return completedAt;
  }

  public void setCompletedAt(Instant completedAt) {
    this.completedAt = completedAt;
  }

  public String getOwnedByInstance() {
    return ownedByInstance;
  }

  public void setOwnedByInstance(String ownedByInstance) {
    this.ownedByInstance = ownedByInstance;
  }

  public Long getVersion() {
    return version;
  }

  public void setVersion(Long version) {
    this.version = version;
  }

  public String getCreatedByActor() {
    return createdByActor;
  }

  public void setCreatedByActor(String createdByActor) {
    this.createdByActor = createdByActor;
  }
}
