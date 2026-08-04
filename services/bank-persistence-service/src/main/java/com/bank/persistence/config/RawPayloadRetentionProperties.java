package com.bank.persistence.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Default retention window applied to {@code raw_payload.retain_until} when a caller does not
 * supply one explicitly. COMP-003 AC: 7-year default, configurable.
 */
@ConfigurationProperties(prefix = "raw-payload.retention")
public record RawPayloadRetentionProperties(Integer years) {

    private static final int DEFAULT_YEARS = 7;

    public int yearsOrDefault() {
        return years != null && years > 0 ? years : DEFAULT_YEARS;
    }
}
