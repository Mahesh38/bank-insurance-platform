package com.bank.insurance.onesb.observability;

import com.bank.common.audit.AuditEvent;
import com.bank.common.audit.AuditOutcomes;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;

class LoggingAuditEventPublisherTest {

    @Test
    void publish_doesNotThrow_forValidEvent() {
        LoggingAuditEventPublisher publisher = new LoggingAuditEventPublisher();
        AuditEvent event = AuditEvent.builder()
                .actorId("test")
                .action("ONESB_OUTBOUND_CALL")
                .outcome(AuditOutcomes.SUCCESS)
                .metadata("latencyMs", "1")
                .build();

        assertThatCode(() -> publisher.publish(event)).doesNotThrowAnyException();
        assertThatCode(() -> publisher.publish(null)).doesNotThrowAnyException();
    }
}
