package com.bank.platform.lead.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@code lead.expiry-days}: unset by default — the BRD leaves the ageing horizon to configuration
 * (OPEN-D4); when set it must be positive.
 */
@ConfigurationProperties(prefix = "lead")
public record LeadProperties(Integer expiryDays) {}
