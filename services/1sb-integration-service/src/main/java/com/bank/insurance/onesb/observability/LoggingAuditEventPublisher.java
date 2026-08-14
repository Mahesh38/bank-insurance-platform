package com.bank.insurance.onesb.observability;

import com.bank.common.audit.AuditEvent;
import com.bank.common.audit.AuditEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * Operator-facing audit sink: structured lines to the application log.
 *
 * <p>Not a bean by annotation — {@code AuditSinkConfig} constructs it, so that exactly one
 * {@link AuditEventPublisher} bean exists (the composite) and {@code @MockBean} in tests
 * resolves without ambiguity.
 *
 * <p>This is <strong>not</strong> durable evidence. Log retention is measured in days or weeks;
 * the audit trail that satisfies a retention regime is the persisted one
 * ({@code HttpAuditEventStoreAdapter}). Keep this sink for incident response, not for audit.
 */
public class LoggingAuditEventPublisher implements AuditEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(LoggingAuditEventPublisher.class);

    @Override
    public void publish(AuditEvent event) {
        if (event == null) {
            return;
        }
        log.info(
                "audit eventId={} action={} outcome={} resourceType={} resourceId={} metadata={}",
                event.getEventId(),
                event.getAction(),
                event.getOutcome(),
                event.getResourceType(),
                event.getResourceId(),
                event.getMetadata());
    }
}
