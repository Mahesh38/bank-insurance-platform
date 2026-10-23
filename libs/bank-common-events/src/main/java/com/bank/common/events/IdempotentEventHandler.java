package com.bank.common.events;

import java.util.Objects;

/**
 * At-least-once delivery made effectively-once (ADR-025 §4): claims {@code eventId} for this
 * consumer, then runs the handler; a duplicate is acknowledged without running it. Invoke inside
 * the consumer's database transaction — if the handler throws, the claim rolls back with the work
 * and the redelivery is processed. Retry / DLQ routing of a throwing handler is the caller's (Kafka
 * listener) job.
 */
public final class IdempotentEventHandler implements EventHandler {

  /** What one delivery did. Both are an ack. */
  public enum Outcome {
    PROCESSED,
    DUPLICATE
  }

  private final ProcessedEventStore store;
  private final String consumer;
  private final EventHandler delegate;

  public IdempotentEventHandler(ProcessedEventStore store, String consumer, EventHandler delegate) {
    this.store = Objects.requireNonNull(store);
    if (consumer == null || consumer.isBlank()) {
      throw new IllegalArgumentException("consumer is required");
    }
    this.consumer = consumer;
    this.delegate = Objects.requireNonNull(delegate);
  }

  @Override
  public void handle(EventEnvelope event) {
    process(event);
  }

  public Outcome process(EventEnvelope event) {
    if (!store.claim(event.eventId(), consumer)) {
      return Outcome.DUPLICATE;
    }
    delegate.handle(event);
    return Outcome.PROCESSED;
  }
}
