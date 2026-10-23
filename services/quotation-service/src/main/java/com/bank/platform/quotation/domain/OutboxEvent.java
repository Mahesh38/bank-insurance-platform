package com.bank.platform.quotation.domain;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Outbox item written in the same DynamoDB transaction as the change (ADR-025 §3). {@code data} is
 * the event's {@code data} member — ids and states only, never PII — and must match {@code
 * docs/platform/contracts/events/schemas/<eventType>.v1.json}. The adapter wraps it in {@code
 * event-envelope/v1}.
 *
 * @param actorId {@code X-Actor-Id} of the caller, or null for background work (actor {@code
 *     SYSTEM})
 */
public record OutboxEvent(
    String eventId,
    String eventType,
    String aggregateId,
    Instant occurredAt,
    String journeyId,
    String leadId,
    String configVersionRef,
    String actorId,
    Map<String, Object> data) {

  public OutboxEvent {
    // Insertion order kept so the stored JSON reads in the order the producer wrote it.
    data = Collections.unmodifiableMap(new LinkedHashMap<>(data));
  }
}
