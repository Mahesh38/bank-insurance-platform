package com.bank.platform.quotation.adapter.dynamodb;

import com.bank.platform.quotation.domain.WorkingRetention;
import java.time.Duration;

/**
 * Table name and expiry rules of {@code {env}-quotation-working} (schemas/20 §1): idempotency items
 * live 24 h, outbox items {@code outboxTtl} after they are published (ADR-025 §3), the working
 * quote per {@link WorkingRetention}. Unpublished outbox items are spread over {@code
 * outboxPendingShards} partitions of the sparse pending index.
 */
public record QuoteTableSettings(
    String tableName,
    WorkingRetention retention,
    Duration idempotencyTtl,
    Duration outboxTtl,
    int outboxPendingShards) {

  public QuoteTableSettings {
    if (outboxPendingShards < 1) {
      throw new IllegalArgumentException("outboxPendingShards must be >= 1");
    }
  }
}
