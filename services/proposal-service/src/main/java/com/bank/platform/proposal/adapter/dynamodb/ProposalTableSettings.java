package com.bank.platform.proposal.adapter.dynamodb;

import com.bank.platform.proposal.domain.WorkingRetention;
import java.time.Duration;

/**
 * Table name and expiry rules of {@code {env}-proposal-working} (schemas/20 §1–2): outbox items
 * live {@code outboxTtl} after they are published (ADR-025 §3); unpublished ones are spread over
 * {@code outboxPendingShards} partitions of the sparse pending index.
 */
public record ProposalTableSettings(
    String tableName,
    WorkingRetention retention,
    Duration idempotencyTtl,
    Duration outboxTtl,
    int outboxPendingShards) {

  public ProposalTableSettings {
    if (outboxPendingShards < 1) {
      throw new IllegalArgumentException("outboxPendingShards must be >= 1");
    }
  }
}
