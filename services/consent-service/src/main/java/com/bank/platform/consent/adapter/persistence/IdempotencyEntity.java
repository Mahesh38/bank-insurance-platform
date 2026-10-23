package com.bank.platform.consent.adapter.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "idempotency_record", schema = "consent")
public class IdempotencyEntity {

  @Id
  @Column(name = "idempotency_key", length = 128)
  private String key;

  @Column(name = "request_hash", nullable = false, length = 64)
  private String requestHash;

  @Column(name = "http_status", nullable = false)
  private int httpStatus;

  @Column(name = "response_body", nullable = false, length = 8000)
  private String responseBody;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public String getRequestHash() {
    return requestHash;
  }

  public void setRequestHash(String requestHash) {
    this.requestHash = requestHash;
  }

  public int getHttpStatus() {
    return httpStatus;
  }

  public void setHttpStatus(int httpStatus) {
    this.httpStatus = httpStatus;
  }

  public String getResponseBody() {
    return responseBody;
  }

  public void setResponseBody(String responseBody) {
    this.responseBody = responseBody;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public void setExpiresAt(Instant expiresAt) {
    this.expiresAt = expiresAt;
  }
}
