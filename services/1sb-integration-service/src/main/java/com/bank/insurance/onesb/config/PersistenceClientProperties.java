package com.bank.insurance.onesb.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "bank.persistence")
public record PersistenceClientProperties(String baseUrl) {

    public PersistenceClientProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "http://localhost:8081";
        }
    }
}
