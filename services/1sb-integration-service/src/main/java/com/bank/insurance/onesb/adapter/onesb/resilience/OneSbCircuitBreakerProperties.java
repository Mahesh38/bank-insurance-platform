package com.bank.insurance.onesb.adapter.onesb.resilience;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Circuit breaker settings for 1SB HTTP egress ({@code NFR-004} / {@code RESILIENCE-POLICY.md}).
 */
@ConfigurationProperties(prefix = "onesb.circuit-breaker")
public record OneSbCircuitBreakerProperties(
        boolean enabled,
        int failureThreshold,
        long openWaitMs,
        int halfOpenSuccessThreshold
) {
    public OneSbCircuitBreakerProperties {
        if (failureThreshold <= 0) {
            failureThreshold = 5;
        }
        if (openWaitMs <= 0) {
            openWaitMs = 30_000L;
        }
        if (halfOpenSuccessThreshold <= 0) {
            halfOpenSuccessThreshold = 1;
        }
    }
}
