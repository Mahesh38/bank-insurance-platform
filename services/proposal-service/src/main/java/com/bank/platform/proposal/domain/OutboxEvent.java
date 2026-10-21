package com.bank.platform.proposal.domain;

import java.time.Instant;
import java.util.Map;

/**
 * Outbox item written in the same DynamoDB transaction as the change. Ids and state only, no PII.
 */
public record OutboxEvent(
    String eventId,
    String eventType,
    String aggregateId,
    Instant occurredAt,
    Map<String, String> attributes) {

  public OutboxEvent {
    attributes = Map.copyOf(attributes);
  }
}
