package com.bank.platform.integration.hub.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@Tag("FUNC-032")
class ProviderCircuitBreakerTest {

  @Test
  void opensAfterThreshold_andRejects_AT_HUB_09() {
    ProviderCircuitBreaker breaker = new ProviderCircuitBreaker(true, 2, 10_000L, 1);
    assertThat(breaker.allowRequest()).isTrue();
    breaker.recordFailure();
    assertThat(breaker.state()).isEqualTo(ProviderCircuitBreaker.State.CLOSED);
    breaker.recordFailure();
    assertThat(breaker.state()).isEqualTo(ProviderCircuitBreaker.State.OPEN);
    assertThat(breaker.allowRequest()).isFalse();
  }
}
