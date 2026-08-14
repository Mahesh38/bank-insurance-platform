package com.bank.insurance.onesb.observability;

import com.bank.common.audit.AuditEvent;
import com.bank.common.audit.AuditEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Fans one audit event out to every configured sink, and refuses to let a failing sink take the
 * others — or the caller's transaction — down with it.
 *
 * <p>This is the seam that keeps the production transport decision open. Call sites publish to
 * {@link AuditEventPublisher} and know nothing about where events land; today that is the
 * application log plus the persistence API, and a Kafka sink can be added later by
 * implementing the same interface and naming it in configuration. No service code changes.
 *
 * <p><b>Why both sinks rather than only the durable one.</b> The log line is what an operator
 * greps at 2am during an incident, and it is available even when persistence is the thing that
 * is broken. The persisted row is what an auditor reads two years later. They answer different
 * questions, so replacing one with the other would lose something.
 */
public class CompositeAuditEventPublisher implements AuditEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(CompositeAuditEventPublisher.class);

    private final List<AuditEventPublisher> sinks;

    public CompositeAuditEventPublisher(List<AuditEventPublisher> sinks) {
        this.sinks = List.copyOf(sinks);
    }

    @Override
    public void publish(AuditEvent event) {
        if (event == null) {
            return;
        }
        for (AuditEventPublisher sink : sinks) {
            try {
                sink.publish(event);
            } catch (Exception e) {
                // A sink that throws is a defect in that sink. Isolating it here means one
                // broken sink cannot suppress the others, which is the whole point of having
                // more than one.
                log.warn("Audit sink {} failed for eventId={} action={}: {}",
                        sink.getClass().getSimpleName(), event.getEventId(), event.getAction(),
                        e.toString());
            }
        }
    }

    /** Exposed for the wiring test — the set of sinks actually in use. */
    public List<AuditEventPublisher> sinks() {
        return sinks;
    }
}
