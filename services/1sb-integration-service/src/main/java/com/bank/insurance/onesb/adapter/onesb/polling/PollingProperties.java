package com.bank.insurance.onesb.adapter.onesb.polling;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Backoff defaults per architecture §7.4: base 1s, multiplier 2, cap 30s, max 20 attempts.
 */
@ConfigurationProperties(prefix = "onesb.poll")
public record PollingProperties(
        @DefaultValue("1000") long baseDelayMs,
        @DefaultValue("2.0") double multiplier,
        @DefaultValue("30000") long maxDelayMs,
        @DefaultValue("20") int maxAttempts
) {
    /** Delay before attempt {@code attemptNumber} (1-based), after a previous incomplete poll. */
    public long delayBeforeAttemptMs(int attemptNumber) {
        if (attemptNumber <= 1) {
            return 0L;
        }
        double delay = baseDelayMs * Math.pow(multiplier, attemptNumber - 2);
        return Math.min(maxDelayMs, Math.round(delay));
    }
}
