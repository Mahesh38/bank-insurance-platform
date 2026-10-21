package com.bank.platform.proposal.adapter.dynamodb;

import com.bank.platform.proposal.domain.WorkingRetention;
import java.time.Duration;

/** Table name and expiry rules of {@code {env}-proposal-working} (schemas/20 §1–2). */
public record ProposalTableSettings(
    String tableName, WorkingRetention retention, Duration idempotencyTtl, Duration outboxTtl) {}
