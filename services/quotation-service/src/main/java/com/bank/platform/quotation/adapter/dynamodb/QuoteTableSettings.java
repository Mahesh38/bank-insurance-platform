package com.bank.platform.quotation.adapter.dynamodb;

import com.bank.platform.quotation.domain.WorkingRetention;
import java.time.Duration;

/**
 * Table name and expiry rules of {@code {env}-quotation-working} (schemas/20 §1): idempotency items
 * live 24 h, outbox items 7 days, the working quote per {@link WorkingRetention}.
 */
public record QuoteTableSettings(
    String tableName, WorkingRetention retention, Duration idempotencyTtl, Duration outboxTtl) {}
