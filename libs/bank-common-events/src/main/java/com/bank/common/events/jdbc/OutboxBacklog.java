package com.bank.common.events.jdbc;

import java.time.Instant;

/**
 * Alerting inputs (NFR-DAT-05 outbox age): unpublished rows still being retried, parked rows (each
 * one an operations task), and the {@code created_at} of the oldest unpublished, unparked row (null
 * when none).
 */
public record OutboxBacklog(long pending, long parked, Instant oldestPendingCreatedAt) {}
