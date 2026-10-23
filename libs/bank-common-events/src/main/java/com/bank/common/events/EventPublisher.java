package com.bank.common.events;

/**
 * Transport port (ADR-025 §5). Called only by an outbox relay, never from a request path (§3).
 * {@link #publish} returns once the transport has durably accepted the event (for Kafka: acked by
 * all in-sync replicas) and throws {@link EventPublishException} otherwise, so the relay never
 * marks a row published that the broker does not hold. Services never import Kafka classes
 * directly.
 */
@FunctionalInterface
public interface EventPublisher {

  void publish(EventEnvelope event);
}
