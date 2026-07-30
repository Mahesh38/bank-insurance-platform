package com.bank.common.audit;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable audit event matching the bank-wide audit schema (architecture §8.3).
 *
 * <p>Instances are created via the nested {@link Builder} or the
 * {@link #builder()} factory.
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
public final class AuditEvent {

    private final String              eventId;
    private final Instant             timestamp;
    private final String              actorId;
    private final String              actorType;
    private final String              action;
    private final String              resourceType;
    private final String              resourceId;
    private final String              outcome;
    private final String              lob;
    private final String              journeyId;
    private final String              distributorId;
    private final String              agentId;
    private final String              traceId;
    private final Map<String, String> metadata;

    private AuditEvent(Builder b) {
        this.eventId       = b.eventId != null ? b.eventId : UUID.randomUUID().toString();
        this.timestamp     = b.timestamp != null ? b.timestamp : Instant.now();
        this.actorId       = Objects.requireNonNull(b.actorId,  "actorId must not be null");
        this.actorType     = b.actorType;
        this.action        = Objects.requireNonNull(b.action,   "action must not be null");
        this.resourceType  = b.resourceType;
        this.resourceId    = b.resourceId;
        this.outcome       = Objects.requireNonNull(b.outcome,  "outcome must not be null");
        this.lob           = b.lob;
        this.journeyId     = b.journeyId;
        this.distributorId = b.distributorId;
        this.agentId       = b.agentId;
        this.traceId       = b.traceId;
        this.metadata      = Collections.unmodifiableMap(new HashMap<>(b.metadata));
    }

    public String              getEventId()       { return eventId; }
    public Instant             getTimestamp()     { return timestamp; }
    public String              getActorId()       { return actorId; }
    public String              getActorType()     { return actorType; }
    public String              getAction()        { return action; }
    public String              getResourceType()  { return resourceType; }
    public String              getResourceId()    { return resourceId; }
    public String              getOutcome()       { return outcome; }
    public String              getLob()           { return lob; }
    public String              getJourneyId()     { return journeyId; }
    public String              getDistributorId() { return distributorId; }
    public String              getAgentId()       { return agentId; }
    public String              getTraceId()       { return traceId; }
    public Map<String, String> getMetadata()      { return metadata; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String              eventId;
        private Instant             timestamp;
        private String              actorId;
        private String              actorType;
        private String              action;
        private String              resourceType;
        private String              resourceId;
        private String              outcome;
        private String              lob;
        private String              journeyId;
        private String              distributorId;
        private String              agentId;
        private String              traceId;
        private final Map<String, String> metadata = new HashMap<>();

        private Builder() {}

        public Builder eventId(String eventId)           { this.eventId = eventId;             return this; }
        public Builder timestamp(Instant timestamp)      { this.timestamp = timestamp;         return this; }
        public Builder actorId(String actorId)           { this.actorId = actorId;             return this; }
        public Builder actorType(String actorType)       { this.actorType = actorType;         return this; }
        public Builder action(String action)             { this.action = action;               return this; }
        public Builder resourceType(String resourceType) { this.resourceType = resourceType;   return this; }
        public Builder resourceId(String resourceId)     { this.resourceId = resourceId;       return this; }
        public Builder outcome(String outcome)           { this.outcome = outcome;             return this; }
        public Builder lob(String lob)                   { this.lob = lob;                     return this; }
        public Builder journeyId(String journeyId)       { this.journeyId = journeyId;         return this; }
        public Builder distributorId(String distId)      { this.distributorId = distId;        return this; }
        public Builder agentId(String agentId)           { this.agentId = agentId;             return this; }
        public Builder traceId(String traceId)           { this.traceId = traceId;             return this; }
        public Builder metadata(String key, String value){ this.metadata.put(key, value);      return this; }
        public Builder metadataAll(Map<String, String> m){ this.metadata.putAll(m);            return this; }

        public AuditEvent build() { return new AuditEvent(this); }
    }

    @Override
    public String toString() {
        return "AuditEvent{eventId='" + eventId + "', action='" + action
            + "', actorId='" + actorId + "', outcome='" + outcome + "'}";
    }
}
