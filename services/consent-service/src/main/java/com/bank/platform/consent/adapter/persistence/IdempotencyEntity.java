package com.bank.platform.consent.adapter.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "idempotency_record")
public class IdempotencyEntity {

  @Id
  @Column(name = "idempotency_key", nullable = false, length = 200)
  private String idempotencyKey;

  @Column(name = "request_hash", nullable = false, length = 64)
  private String requestHash;

  @Column(name = "http_status", nullable = false)
  private int httpStatus;

  @Column(name = "response_body", nullable = false, length = 8000)
  private String responseBody;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  protected IdempotencyEntity() {}

  public IdempotencyEntity(
      String idempotencyKey,
      String requestHash,
      int httpStatus,
      String responseBody,
      Instant expiresAt) {
    this.idempotencyKey = idempotencyKey;
    this.requestHash = requestHash;
    this.httpStatus = httpStatus;
    this.responseBody = responseBody;
    this.expiresAt = expiresAt;
  }

  public void replace(String requestHash, int httpStatus, String responseBody, Instant expiresAt) {
    this.requestHash = requestHash;
    this.httpStatus = httpStatus;
    this.responseBody = responseBody;
    this.expiresAt = expiresAt;
  }

  public String requestHash() {
    return requestHash;
  }

  public int httpStatus() {
    return httpStatus;
  }

  public String responseBody() {
    return responseBody;
  }

  public Instant expiresAt() {
    return expiresAt;
  }
}
