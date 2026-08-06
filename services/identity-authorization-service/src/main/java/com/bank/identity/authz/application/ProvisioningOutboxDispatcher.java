package com.bank.identity.authz.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "identity.provisioning.enabled", havingValue = "true")
public class ProvisioningOutboxDispatcher {

    private static final Logger log = LoggerFactory.getLogger(ProvisioningOutboxDispatcher.class);

    private final JdbcClient jdbc;
    private final ProvisioningOutboxProcessor processor;

    public ProvisioningOutboxDispatcher(
        JdbcClient jdbc,
        ProvisioningOutboxProcessor processor
    ) {
        this.jdbc = jdbc;
        this.processor = processor;
    }

    @Scheduled(fixedDelayString = "${identity.provisioning.poll-interval:5000}")
    public void dispatch() {
        List<OutboxItem> items = jdbc.sql("""
                select id, aggregate_id
                  from outbox_event
                 where published_at is null
                   and event_type = 'IDENTITY_PROVISIONING_REQUESTED'
                   and attempts < 10
                 order by created_at
                 fetch first 20 rows only
                """)
            .query((rs, rowNum) -> new OutboxItem(
                rs.getObject("id", UUID.class), rs.getObject("aggregate_id", UUID.class)))
            .list();
        items.forEach(this::dispatchSafely);
    }

    private void dispatchSafely(OutboxItem item) {
        try {
            processor.provision(item.eventId(), item.userId());
        } catch (RuntimeException exception) {
            String safeError = exception.getClass().getSimpleName();
            jdbc.sql("""
                    update outbox_event
                       set attempts = attempts + 1, last_error = :error
                     where id = :id and published_at is null
                    """)
                .param("error", safeError)
                .param("id", item.eventId())
                .update();
            log.warn("Partner identity provisioning attempt failed; eventId={}, error={}", item.eventId(), safeError);
        }
    }

    record OutboxItem(UUID eventId, UUID userId) {}
}
