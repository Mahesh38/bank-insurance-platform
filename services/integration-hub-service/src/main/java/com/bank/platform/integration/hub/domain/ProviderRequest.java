package com.bank.platform.integration.hub.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * Durable (port-backed) provider request row. State transitions follow Hub LLD §4. In-memory store
 * is interim until Aurora {@code hub.provider_request} (HUB-1 / FUNC-032).
 */
public final class ProviderRequest {

  private final String providerRequestId;
  private final String idempotencyKey;
  private final String requestHash;
  private final String operation;
  private final String lob;
  private final String productClass;
  private final String mode;
  private final CallerRef callerRef;
  private final String journeyId;
  private final String provider;
  private final String routingVersion;
  private final Instant createdAt;
  private CanonicalState state;
  private String adapterJobId;
  private QuoteResult result;
  private Failure failure;

  public ProviderRequest(
      String providerRequestId,
      String idempotencyKey,
      String requestHash,
      String operation,
      String lob,
      String productClass,
      String mode,
      CallerRef callerRef,
      String journeyId,
      String provider,
      String routingVersion,
      Instant createdAt) {
    this.providerRequestId = Objects.requireNonNull(providerRequestId);
    this.idempotencyKey = Objects.requireNonNull(idempotencyKey);
    this.requestHash = Objects.requireNonNull(requestHash);
    this.operation = Objects.requireNonNull(operation);
    this.lob = Objects.requireNonNull(lob);
    this.productClass = Objects.requireNonNull(productClass);
    this.mode = Objects.requireNonNull(mode);
    this.callerRef = Objects.requireNonNull(callerRef);
    this.journeyId = Objects.requireNonNull(journeyId);
    this.provider = Objects.requireNonNull(provider);
    this.routingVersion = Objects.requireNonNull(routingVersion);
    this.createdAt = Objects.requireNonNull(createdAt);
    this.state = CanonicalState.DISPATCHING;
  }

  public void markInProgress(String jobId) {
    this.adapterJobId = Objects.requireNonNull(jobId);
    this.state = CanonicalState.IN_PROGRESS;
  }

  public void markSubmissionUncertain() {
    this.state = CanonicalState.SUBMISSION_UNCERTAIN;
  }

  public void markFailed(Failure failure) {
    this.failure = Objects.requireNonNull(failure);
    this.state = CanonicalState.FAILED;
  }

  public void applyAdapterJob(CanonicalState terminal, QuoteResult result, Failure failure) {
    this.state = Objects.requireNonNull(terminal);
    this.result = result;
    this.failure = failure;
  }

  public String providerRequestId() {
    return providerRequestId;
  }

  public String idempotencyKey() {
    return idempotencyKey;
  }

  public String requestHash() {
    return requestHash;
  }

  public String operation() {
    return operation;
  }

  public String lob() {
    return lob;
  }

  public String productClass() {
    return productClass;
  }

  public String mode() {
    return mode;
  }

  public CallerRef callerRef() {
    return callerRef;
  }

  public String journeyId() {
    return journeyId;
  }

  public String provider() {
    return provider;
  }

  public String routingVersion() {
    return routingVersion;
  }

  public Instant createdAt() {
    return createdAt;
  }

  public CanonicalState state() {
    return state;
  }

  public String adapterJobId() {
    return adapterJobId;
  }

  public QuoteResult result() {
    return result;
  }

  public Failure failure() {
    return failure;
  }
}
