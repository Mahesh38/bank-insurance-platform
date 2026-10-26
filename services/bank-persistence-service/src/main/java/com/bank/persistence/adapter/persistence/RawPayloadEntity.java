package com.bank.persistence.adapter.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "raw_payload")
public class RawPayloadEntity {

  @Id
  @Column(name = "payload_id", length = 36, nullable = false)
  private String payloadId;

  @Column(name = "job_id", length = 36, nullable = false)
  private String jobId;

  @Column(name = "direction", length = 3, nullable = false)
  private String direction;

  @Column(name = "operation", length = 100, nullable = false)
  private String operation;

  @Column(name = "lob", length = 20, nullable = false)
  private String lob;

  @JdbcTypeCode(SqlTypes.VARBINARY)
  @Column(name = "payload_enc", nullable = false)
  private byte[] payloadEnc;

  @Column(name = "encryption_key_id", length = 50, nullable = false)
  private String encryptionKeyId;

  @Column(name = "http_status")
  private Short httpStatus;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "retain_until", nullable = false)
  private LocalDate retainUntil;

  public RawPayloadEntity() {}

  public String getPayloadId() {
    return payloadId;
  }

  public void setPayloadId(String payloadId) {
    this.payloadId = payloadId;
  }

  public String getJobId() {
    return jobId;
  }

  public void setJobId(String jobId) {
    this.jobId = jobId;
  }

  public String getDirection() {
    return direction;
  }

  public void setDirection(String direction) {
    this.direction = direction;
  }

  public String getOperation() {
    return operation;
  }

  public void setOperation(String operation) {
    this.operation = operation;
  }

  public String getLob() {
    return lob;
  }

  public void setLob(String lob) {
    this.lob = lob;
  }

  public String getEncryptionKeyId() {
    return encryptionKeyId;
  }

  public void setEncryptionKeyId(String encryptionKeyId) {
    this.encryptionKeyId = encryptionKeyId;
  }

  public Short getHttpStatus() {
    return httpStatus;
  }

  public void setHttpStatus(Short httpStatus) {
    this.httpStatus = httpStatus;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public LocalDate getRetainUntil() {
    return retainUntil;
  }

  public void setRetainUntil(LocalDate retainUntil) {
    this.retainUntil = retainUntil;
  }

  public byte[] getPayloadEnc() {
    return payloadEnc;
  }

  public void setPayloadEnc(byte[] payloadEnc) {
    this.payloadEnc = payloadEnc;
  }
}
