package com.bank.common.audit;

import lombok.Builder;
import lombok.NonNull;
import lombok.ToString;
import lombok.Value;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable audit event matching the bank-wide audit schema (architecture §8.3).
 *
 * <p>Instances are created via {@link #builder()}.
 *
 * <p>Design notes:
 * <ul>
 *   <li>All events must have {@code eventId}, {@code timestamp}, {@code actorId},
 *       {@code action}, and {@code outcome}.</li>
 *   <li>{@code metadata} is a free-form map for action-specific fields
 *       (e.g. {@code jobId}, {@code applicationNumber}); values must not contain
 *       raw PII — use masked/hashed forms if required.</li>
 *   <li>Audit records are append-only — never mutate a published event.</li>
 * </ul>
 */
@Value
@Builder
@ToString(of = {"eventId", "action", "actorId", "outcome"})
public class AuditEvent {

    String              eventId;
    Instant             timestamp;
    @NonNull String     actorId;
    String              actorType;
    @NonNull String     action;
    String              resourceType;
    String              resourceId;
    @NonNull String     outcome;
    String              lob;
    String              journeyId;
    String              distributorId;
    String              agentId;
    String              traceId;
    Map<String, String> metadata;

    public static class AuditEventBuilder {
        private Map<String, String> metadata = new HashMap<>();

        public AuditEventBuilder metadata(String key, String value) {
            this.metadata.put(key, value);
            return this;
        }

        public AuditEventBuilder metadataAll(Map<String, String> m) {
            this.metadata.putAll(m);
            return this;
        }

        public AuditEvent build() {
            String resolvedEventId = eventId != null ? eventId : UUID.randomUUID().toString();
            Instant resolvedTimestamp = timestamp != null ? timestamp : Instant.now();
            Objects.requireNonNull(actorId, "actorId must not be null");
            Objects.requireNonNull(action, "action must not be null");
            Objects.requireNonNull(outcome, "outcome must not be null");
            Map<String, String> resolvedMetadata =
                Collections.unmodifiableMap(new HashMap<>(metadata != null ? metadata : Map.of()));
            return new AuditEvent(
                resolvedEventId, resolvedTimestamp, actorId, actorType, action,
                resourceType, resourceId, outcome, lob, journeyId, distributorId,
                agentId, traceId, resolvedMetadata);
        }
    }
}
