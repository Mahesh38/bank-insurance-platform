package com.bank.insurance.onesb.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Proposal-related settings ({@code insurance.proposals.*}).
 */
@ConfigurationProperties(prefix = "insurance.proposals")
public record ProposalProperties(
        long schemaCacheTtlSeconds
) {
    public ProposalProperties {
        if (schemaCacheTtlSeconds <= 0) {
            schemaCacheTtlSeconds = 3600L;
        }
    }
}
