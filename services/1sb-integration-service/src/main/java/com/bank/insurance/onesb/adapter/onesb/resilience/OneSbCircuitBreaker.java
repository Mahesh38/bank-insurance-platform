package com.bank.insurance.onesb.adapter.onesb.resilience;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Simple consecutive-failure circuit breaker for 1SB egress ({@code NFR-004}).
 * Thread-safe; does not retry 401 — callers decide what counts as a failure.
 */
public final class OneSbCircuitBreaker {

    public enum State { CLOSED, OPEN, HALF_OPEN }

    private final OneSbCircuitBreakerProperties properties;
    private final AtomicInteger consecutiveFailures = new AtomicInteger();
    private final AtomicInteger halfOpenSuccesses = new AtomicInteger();
    private final AtomicLong openedAtMs = new AtomicLong();
    private final AtomicReference<State> state = new AtomicReference<>(State.CLOSED);

    public OneSbCircuitBreaker(OneSbCircuitBreakerProperties properties) {
        this.properties = properties;
    }

    public State state() {
        maybeTransitionToHalfOpen();
        return state.get();
    }

    /** @return true when the call may proceed */
    public boolean allowRequest() {
        if (!properties.enabled()) {
            return true;
        }
        maybeTransitionToHalfOpen();
        State current = state.get();
        return current == State.CLOSED || current == State.HALF_OPEN;
    }

    public void recordSuccess() {
        if (!properties.enabled()) {
            return;
        }
        consecutiveFailures.set(0);
        if (state.get() == State.HALF_OPEN) {
            if (halfOpenSuccesses.incrementAndGet() >= properties.halfOpenSuccessThreshold()) {
                state.set(State.CLOSED);
                halfOpenSuccesses.set(0);
            }
        } else {
            state.set(State.CLOSED);
        }
    }

    public void recordFailure() {
        if (!properties.enabled()) {
            return;
        }
        if (state.get() == State.HALF_OPEN) {
            tripOpen();
            return;
        }
        if (consecutiveFailures.incrementAndGet() >= properties.failureThreshold()) {
            tripOpen();
        }
    }

    private void tripOpen() {
        state.set(State.OPEN);
        openedAtMs.set(System.currentTimeMillis());
        halfOpenSuccesses.set(0);
    }

    private void maybeTransitionToHalfOpen() {
        if (state.get() == State.OPEN
                && System.currentTimeMillis() - openedAtMs.get() >= properties.openWaitMs()) {
            if (state.compareAndSet(State.OPEN, State.HALF_OPEN)) {
                halfOpenSuccesses.set(0);
            }
        }
    }
}
