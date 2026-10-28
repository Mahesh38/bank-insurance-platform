package com.bank.platform.integration.hub.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * Durable provider request row ({@code hub.provider_request}). Offer payloads are not persisted
 * (LLD §10) — poll reloads them from the adapter via {@code adapterJobId}.
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
  private Instant updatedAt;
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
    this.updatedAt = createdAt;
    this.state = CanonicalState.DISPATCHING;
  }

  public static ProviderRequest rehydrate(
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
      Instant createdAt,
      Instant updatedAt,
      CanonicalState state,
      String adapterJobId,
      Failure failure) {
    ProviderRequest row =
        new ProviderRequest(
            providerRequestId,
            idempotencyKey,
            requestHash,
            operation,
            lob,
            productClass,
            mode,
            callerRef,
            journeyId,
            provider,
            routingVersion,
            createdAt);
    row.updatedAt = Objects.requireNonNull(updatedAt);
    row.state = Objects.requireNonNull(state);
    row.adapterJobId = adapterJobId;
    row.failure = failure;
    return row;
  }

  public void markInProgress(String jobId, Instant at) {
    this.adapterJobId = Objects.requireNonNull(jobId);
    this.state = CanonicalState.IN_PROGRESS;
    this.updatedAt = at;
    this.failure = null;
  }

  public void markSubmissionUncertain(Instant at) {
    this.state = CanonicalState.SUBMISSION_UNCERTAIN;
    this.updatedAt = at;
  }

  public void markFailed(Failure failure, Instant at) {
    this.failure = Objects.requireNonNull(failure);
    this.state = CanonicalState.FAILED;
    this.updatedAt = at;
    this.result = null;
  }

  public void applyAdapterJob(
      CanonicalState terminal, QuoteResult result, Failure failure, Instant at) {
    this.state = Objects.requireNonNull(terminal);
    this.result = result;
    this.failure = failure;
    this.updatedAt = at;
  }

  public void attachResult(QuoteResult result) {
    this.result = result;
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

  public Instant updatedAt() {
    return updatedAt;
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
