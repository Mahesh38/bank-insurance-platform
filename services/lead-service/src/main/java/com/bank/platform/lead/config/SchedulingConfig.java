package com.bank.platform.lead.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Enables the lead expiry sweep ({@code LeadExpiryJob}); {@code lead.expiry.enabled=false} off. */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "lead.expiry.enabled", havingValue = "true", matchIfMissing = true)
public class SchedulingConfig {}
