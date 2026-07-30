package com.bank.insurance.onesb.observability;

import com.bank.common.audit.AuditEvent;
import com.bank.common.audit.AuditEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Default {@link AuditEventPublisher} that writes structured audit lines to the app log.
 * Replace with DB/Kafka sinks in later phases.
 */
@Component
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
