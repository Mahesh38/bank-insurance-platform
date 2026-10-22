package com.bank.platform.adminconfig.domain;

import java.time.Instant;

/**
 * Transactional outbox (ADR-025 point 3): the event row is written in the same transaction as the
 * state change and relayed later; nothing is sent to a broker inside a request.
 */
public interface OutboxPort {

  void append(OutboxEvent event);

  /** {@code payload} is the full event-envelope/v1 JSON. */
  record OutboxEvent(
      String outboxId,
      String aggregateType,
      String aggregateId,
      String eventType,
      String payload,
      Instant createdAt) {}
}
