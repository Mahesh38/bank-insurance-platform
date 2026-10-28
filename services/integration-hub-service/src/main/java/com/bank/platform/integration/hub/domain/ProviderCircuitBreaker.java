package com.bank.platform.integration.hub.domain;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Per-provider consecutive-failure circuit breaker (Hub LLD §4.1 step 6 / AT-HUB-09). Same shape as
 * the 1SB egress breaker — no new dependency.
 */
public final class ProviderCircuitBreaker {

  public enum State {
    CLOSED,
    OPEN,
    HALF_OPEN
  }

  private final int failureThreshold;
  private final long openWaitMs;
  private final int halfOpenSuccessThreshold;
  private final boolean enabled;
  private final AtomicInteger consecutiveFailures = new AtomicInteger();
  private final AtomicInteger halfOpenSuccesses = new AtomicInteger();
  private final AtomicLong openedAtMs = new AtomicLong();
  private final AtomicReference<State> state = new AtomicReference<>(State.CLOSED);

  public ProviderCircuitBreaker(
      boolean enabled, int failureThreshold, long openWaitMs, int halfOpenSuccessThreshold) {
    this.enabled = enabled;
    this.failureThreshold = failureThreshold;
    this.openWaitMs = openWaitMs;
    this.halfOpenSuccessThreshold = halfOpenSuccessThreshold;
  }

  public State state() {
    maybeTransitionToHalfOpen();
    return state.get();
  }

  public boolean allowRequest() {
    if (!enabled) {
      return true;
    }
    maybeTransitionToHalfOpen();
    State current = state.get();
    return current == State.CLOSED || current == State.HALF_OPEN;
  }

  public void recordSuccess() {
    if (!enabled) {
      return;
    }
    consecutiveFailures.set(0);
    if (state.get() == State.HALF_OPEN) {
      if (halfOpenSuccesses.incrementAndGet() >= halfOpenSuccessThreshold) {
        state.set(State.CLOSED);
        halfOpenSuccesses.set(0);
      }
    } else {
      state.set(State.CLOSED);
    }
  }

  public void recordFailure() {
    if (!enabled) {
      return;
    }
    if (state.get() == State.HALF_OPEN) {
      tripOpen();
      return;
    }
    if (consecutiveFailures.incrementAndGet() >= failureThreshold) {
      tripOpen();
    }
  }

  /** Test / admin reset — returns to CLOSED with cleared counters. */
  public void reset() {
    consecutiveFailures.set(0);
    halfOpenSuccesses.set(0);
    openedAtMs.set(0);
    state.set(State.CLOSED);
  }

  private void tripOpen() {
    state.set(State.OPEN);
    openedAtMs.set(System.currentTimeMillis());
    halfOpenSuccesses.set(0);
  }

  private void maybeTransitionToHalfOpen() {
    if (state.get() == State.OPEN && System.currentTimeMillis() - openedAtMs.get() >= openWaitMs) {
      if (state.compareAndSet(State.OPEN, State.HALF_OPEN)) {
        halfOpenSuccesses.set(0);
      }
    }
  }
}
