package com.bank.insurance.onesb.adapter.onesb.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Outbound 1SB HTTP client settings ({@code onesb.client.*}).
 */
@ConfigurationProperties(prefix = "onesb.client")
public record OneSbClientProperties(
        String baseUrl,
        long connectTimeoutMs,
        long readTimeoutMs
) {
    public OneSbClientProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://demo.api.1silverbullet.tech";
        }
        if (connectTimeoutMs <= 0) {
            connectTimeoutMs = 3000L;
        }
        if (readTimeoutMs <= 0) {
            readTimeoutMs = 30000L;
        }
    }
}
