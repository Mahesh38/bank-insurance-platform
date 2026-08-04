package com.bank.insurance.onesb.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Payment-related settings ({@code insurance.payments.*}). 1SB does not return a payment-url
 * expiry, so the bank computes one from this TTL.
 */
@ConfigurationProperties(prefix = "insurance.payments")
public record PaymentProperties(
        long sessionTtlSeconds
) {
    public PaymentProperties {
        if (sessionTtlSeconds <= 0) {
            sessionTtlSeconds = 3600L;
        }
    }
}
