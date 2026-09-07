package com.bank.insurance.onesb.adapter.onesb.resilience;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("NFR-004")
class OneSbCircuitBreakerTest {

    @Test
    void opensAfterFailureThresholdAndRejectsThenHalfOpens() {
        OneSbCircuitBreaker cb = new OneSbCircuitBreaker(
                new OneSbCircuitBreakerProperties(true, 2, 10L, 1));

        assertThat(cb.allowRequest()).isTrue();
        cb.recordFailure();
        assertThat(cb.state()).isEqualTo(OneSbCircuitBreaker.State.CLOSED);
        cb.recordFailure();
        assertThat(cb.state()).isEqualTo(OneSbCircuitBreaker.State.OPEN);
        assertThat(cb.allowRequest()).isFalse();

        try {
            Thread.sleep(15L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        assertThat(cb.allowRequest()).isTrue();
        assertThat(cb.state()).isEqualTo(OneSbCircuitBreaker.State.HALF_OPEN);
        cb.recordSuccess();
        assertThat(cb.state()).isEqualTo(OneSbCircuitBreaker.State.CLOSED);
    }

    @Test
    void disabledBreakerAlwaysAllows() {
        OneSbCircuitBreaker cb = new OneSbCircuitBreaker(
                new OneSbCircuitBreakerProperties(false, 1, 1L, 1));
        cb.recordFailure();
        cb.recordFailure();
        assertThat(cb.allowRequest()).isTrue();
        assertThat(cb.state()).isEqualTo(OneSbCircuitBreaker.State.CLOSED);
    }
}
