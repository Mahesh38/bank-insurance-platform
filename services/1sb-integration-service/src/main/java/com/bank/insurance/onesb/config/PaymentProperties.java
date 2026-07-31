package com.bank.insurance.onesb.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Payment-related settings ({@code insurance.payments.*}).
 */
@ConfigurationProperties(prefix = "insurance.payments")
public record PaymentProperties(
        String defaultRedirectUrl
) {
    public PaymentProperties {
        if (defaultRedirectUrl == null || defaultRedirectUrl.isBlank()) {
            defaultRedirectUrl = "https://bank.local/insurance/payment-complete";
        }
    }
}
