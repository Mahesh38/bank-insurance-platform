package com.bank.persistence.adapter.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "payment_session")
public class PaymentSessionEntity {

  @Id
  @Column(name = "session_id", length = 36, nullable = false)
  private String sessionId;

  @Column(name = "job_id", length = 36)
  private String jobId;

  @Column(name = "application_number", length = 50, nullable = false)
  private String applicationNumber;

  @Column(name = "lob", length = 20, nullable = false)
  private String lob;

  @Column(name = "payment_url", nullable = false, columnDefinition = "TEXT")
  private String paymentUrl;

  @Column(name = "redirect_url", nullable = false, columnDefinition = "TEXT")
  private String redirectUrl;

  @Column(name = "status", length = 30, nullable = false)
  private String status;

  @Column(name = "external_txn_id", length = 100)
  private String externalTxnId;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "expires_at")
  private Instant expiresAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Column(name = "created_by_actor", length = 100, nullable = false)
  private String createdByActor;

  public PaymentSessionEntity() {}

  public String getSessionId() {
    return sessionId;
  }

  public void setSessionId(String sessionId) {
    this.sessionId = sessionId;
  }

  public String getJobId() {
    return jobId;
  }

  public void setJobId(String jobId) {
    this.jobId = jobId;
  }

  public String getApplicationNumber() {
    return applicationNumber;
  }

  public void setApplicationNumber(String applicationNumber) {
    this.applicationNumber = applicationNumber;
  }

  public String getLob() {
    return lob;
  }

  public void setLob(String lob) {
    this.lob = lob;
  }

  public String getPaymentUrl() {
    return paymentUrl;
  }

  public void setPaymentUrl(String paymentUrl) {
    this.paymentUrl = paymentUrl;
  }

  public String getRedirectUrl() {
    return redirectUrl;
  }

  public void setRedirectUrl(String redirectUrl) {
    this.redirectUrl = redirectUrl;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getExternalTxnId() {
    return externalTxnId;
  }

  public void setExternalTxnId(String externalTxnId) {
    this.externalTxnId = externalTxnId;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public void setExpiresAt(Instant expiresAt) {
    this.expiresAt = expiresAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }

  public String getCreatedByActor() {
    return createdByActor;
  }

  public void setCreatedByActor(String createdByActor) {
    this.createdByActor = createdByActor;
  }
}
