package com.bank.insurance.onesb.adapter.persistence;

import com.bank.common.audit.AuditEvent;
import com.bank.common.audit.AuditEventPublisher;
import com.bank.insurance.onesb.adapter.persistence.dto.PersistenceApiDtos.CreateAuditEventRequest;
import com.bank.insurance.onesb.observability.PiiMasker;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Durable audit sink — appends to {@code audit_event} in bank-persistence-service
 * (RISK-012 / SUG-20260813-a1c).
 *
 * <p>Until this existed, audit events reached the application log only. That satisfied neither
 * the IRDAI record-retention regime (a 10-year minimum on insurance records) nor, in practice,
 * the 180-day rolling ICT-log requirement, because log rotation is usually far shorter. See
 * {@code compliance/REGULATORY-RETENTION-FINDINGS.md}.
 *
 * <p><b>Best-effort, deliberately.</b> A persistence outage must not fail a customer's quote or
 * proposal — the same trade-off already accepted for raw payload capture (NFR-002). The cost is
 * that a persistence outage loses audit events silently, which is why this adapter is normally
 * paired with the log sink rather than replacing it: see {@code CompositeAuditEventPublisher}.
 *
 * <p><b>Metadata is masked before transport.</b> {@code AuditEvent.metadata} is a free-form map
 * and the schema says it must never carry PII, but "must never" is a rule, not a mechanism. The
 * mechanism is here: values are run through {@link PiiMasker} on the way out, so a caller that
 * puts a mobile number in metadata produces a masked row rather than a compliance incident.
 */
public class HttpAuditEventStoreAdapter implements AuditEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(HttpAuditEventStoreAdapter.class);

    private final RestClient persistenceRestClient;
    private final ObjectMapper objectMapper;

    public HttpAuditEventStoreAdapter(
            @Qualifier("persistenceRestClient") RestClient persistenceRestClient,
            ObjectMapper objectMapper) {
        this.persistenceRestClient = persistenceRestClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publish(AuditEvent event) {
        if (event == null) {
            return;
        }
        try {
            persistenceRestClient.post()
                    .uri("/internal/v1/audit-events")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(toRequest(event))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            // Never rethrow: an audit sink that can fail a business transaction is worse than
            // one that can lose a row, and the log sink still has the event.
            log.warn("Failed to persist audit event (eventId={}, action={}): {}",
                    event.getEventId(), event.getAction(), e.toString());
        }
    }

    private CreateAuditEventRequest toRequest(AuditEvent event) {
        return new CreateAuditEventRequest(
                event.getEventId(),
                event.getTimestamp(),
                event.getAction(),
                event.getActorId(),
                event.getActorType() == null ? "SYSTEM" : event.getActorType(),
                event.getResourceType() == null ? "UNKNOWN" : event.getResourceType(),
                event.getResourceId() == null ? "UNKNOWN" : event.getResourceId(),
                event.getOutcome(),
                event.getLob(),
                event.getJourneyId(),
                event.getDistributorId(),
                event.getAgentId(),
                maskedMetadataJson(event.getMetadata()),
                event.getTraceId());
    }

    /**
     * Serialises metadata to JSON with every value masked. Falls back to an empty object rather
     * than to unmasked text — if we cannot guarantee the masking, we do not send the field.
     */
    private String maskedMetadataJson(Map<String, String> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(PiiMasker.maskMap(metadata));
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialise audit metadata; dropping the field rather than "
                    + "sending it unmasked: {}", e.toString());
            return "{}";
        }
    }
}
