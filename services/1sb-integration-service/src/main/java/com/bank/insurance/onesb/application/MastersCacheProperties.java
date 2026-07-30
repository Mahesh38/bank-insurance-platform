package com.bank.insurance.onesb.application;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * In-process master-data cache TTL. Redis deferred (TD-010).
 */
@ConfigurationProperties(prefix = "insurance.masters")
public record MastersCacheProperties(
        @DefaultValue("14400") long cacheTtlSeconds
) {}
