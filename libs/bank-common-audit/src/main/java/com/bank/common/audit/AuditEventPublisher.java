package com.bank.common.audit;

/**
 * SPI for publishing {@link AuditEvent}s to the bank's audit sink.
 *
 * <p>Concrete implementations reside in the consuming service, not in this lib:
 * <ul>
 *   <li>DB adapter: INSERT into {@code audit_event} table (append-only).</li>
 *   <li>Kafka adapter: produce to the bank's compliance Kafka topic.</li>
 *   <li>Composite: DB + Kafka for dual-sink deployments.</li>
 * </ul>
 *
 * <p>Implementations must be idempotent on {@code eventId} — duplicate events
 * (e.g. from retry storms) should be discarded, not double-counted.
 */
public interface AuditEventPublisher {

    /**
     * Publish a single audit event to the sink.
     *
     * @param event fully-populated audit event; must not be null
     */
    void publish(AuditEvent event);
}
