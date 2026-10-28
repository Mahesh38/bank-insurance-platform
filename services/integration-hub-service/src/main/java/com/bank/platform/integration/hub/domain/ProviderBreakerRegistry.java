package com.bank.platform.integration.hub.domain;

import java.util.concurrent.ConcurrentHashMap;

/** One breaker per provider id (ONE_SB, …). */
public final class ProviderBreakerRegistry {

  private final boolean enabled;
  private final int failureThreshold;
  private final long openWaitMs;
  private final int halfOpenSuccessThreshold;
  private final ConcurrentHashMap<String, ProviderCircuitBreaker> breakers =
      new ConcurrentHashMap<>();

  public ProviderBreakerRegistry(
      boolean enabled, int failureThreshold, long openWaitMs, int halfOpenSuccessThreshold) {
    this.enabled = enabled;
    this.failureThreshold = failureThreshold;
    this.openWaitMs = openWaitMs;
    this.halfOpenSuccessThreshold = halfOpenSuccessThreshold;
  }

  public ProviderCircuitBreaker forProvider(String provider) {
    return breakers.computeIfAbsent(
        provider,
        ignored ->
            new ProviderCircuitBreaker(
                enabled, failureThreshold, openWaitMs, halfOpenSuccessThreshold));
  }

  /** Clears all breakers (integration tests between cases). */
  public void resetAll() {
    breakers.values().forEach(ProviderCircuitBreaker::reset);
  }
}
