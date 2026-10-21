package com.bank.platform.proposal.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** {@code {env}-proposal-working} (schemas/20 §1); RET-WORKING defaults to 90 days. */
@ConfigurationProperties(prefix = "proposal.working-store")
public record WorkingStoreProperties(
    String tableName,
    String region,
    String endpointOverride,
    Duration retentionWorking,
    Duration retentionAfterMaterialize,
    Duration idempotencyTtl,
    Duration outboxTtl) {

  public WorkingStoreProperties {
    tableName = tableName == null || tableName.isBlank() ? "local-proposal-working" : tableName;
    region = region == null || region.isBlank() ? "ap-south-1" : region;
    retentionWorking = retentionWorking == null ? Duration.ofDays(90) : retentionWorking;
    retentionAfterMaterialize =
        retentionAfterMaterialize == null ? Duration.ofDays(30) : retentionAfterMaterialize;
    idempotencyTtl = idempotencyTtl == null ? Duration.ofHours(24) : idempotencyTtl;
    outboxTtl = outboxTtl == null ? Duration.ofDays(7) : outboxTtl;
  }
}
