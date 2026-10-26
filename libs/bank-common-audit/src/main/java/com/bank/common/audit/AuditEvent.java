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
 * <p>Instances are created via {@link #builder()}.
 *
 * <p>Design notes:
 *
 * <ul>
 *   <li>All events must have {@code eventId}, {@code timestamp}, {@code actorId}, {@code action},
 *       and {@code outcome}.
 *   <li>{@code metadata} is a free-form map for action-specific fields (e.g. {@code jobId}, {@code
 *       applicationNumber}); values must not contain raw PII — use masked/hashed forms if required.
 *   <li>Audit records are append-only — never mutate a published event.
 * </ul>
 */
public final class AuditEvent {
  private final String eventId;
  private final Instant timestamp;

  private final String actorId;
  private final String actorType;

  private final String action;
  private final String resourceType;
  private final String resourceId;

  private final String outcome;
  private final String lob;
  private final String journeyId;
  private final String distributorId;
  private final String agentId;
  private final String traceId;
  private final Map<String, String> metadata;

  public static class AuditEventBuilder {
    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    private String eventId;

    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    private Instant timestamp;

    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    private String actorId;

    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    private String actorType;

    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    private String action;

    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    private String resourceType;

    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    private String resourceId;

    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    private String outcome;

    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    private String lob;

    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    private String journeyId;

    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    private String distributorId;

    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    private String agentId;

    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    private String traceId;

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
          resolvedEventId,
          resolvedTimestamp,
          actorId,
          actorType,
          action,
          resourceType,
          resourceId,
          outcome,
          lob,
          journeyId,
          distributorId,
          agentId,
          traceId,
          resolvedMetadata);
    }

    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    AuditEventBuilder() {}

    /**
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    public AuditEvent.AuditEventBuilder eventId(final String eventId) {
      this.eventId = eventId;
      return this;
    }

    /**
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    public AuditEvent.AuditEventBuilder timestamp(final Instant timestamp) {
      this.timestamp = timestamp;
      return this;
    }

    /**
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    public AuditEvent.AuditEventBuilder actorId(final String actorId) {
      if (actorId == null) {
        throw new java.lang.NullPointerException("actorId is marked non-null but is null");
      }
      this.actorId = actorId;
      return this;
    }

    /**
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    public AuditEvent.AuditEventBuilder actorType(final String actorType) {
      this.actorType = actorType;
      return this;
    }

    /**
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    public AuditEvent.AuditEventBuilder action(final String action) {
      if (action == null) {
        throw new java.lang.NullPointerException("action is marked non-null but is null");
      }
      this.action = action;
      return this;
    }

    /**
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    public AuditEvent.AuditEventBuilder resourceType(final String resourceType) {
      this.resourceType = resourceType;
      return this;
    }

    /**
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    public AuditEvent.AuditEventBuilder resourceId(final String resourceId) {
      this.resourceId = resourceId;
      return this;
    }

    /**
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    public AuditEvent.AuditEventBuilder outcome(final String outcome) {
      if (outcome == null) {
        throw new java.lang.NullPointerException("outcome is marked non-null but is null");
      }
      this.outcome = outcome;
      return this;
    }

    /**
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    public AuditEvent.AuditEventBuilder lob(final String lob) {
      this.lob = lob;
      return this;
    }

    /**
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    public AuditEvent.AuditEventBuilder journeyId(final String journeyId) {
      this.journeyId = journeyId;
      return this;
    }

    /**
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    public AuditEvent.AuditEventBuilder distributorId(final String distributorId) {
      this.distributorId = distributorId;
      return this;
    }

    /**
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    public AuditEvent.AuditEventBuilder agentId(final String agentId) {
      this.agentId = agentId;
      return this;
    }

    /**
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    public AuditEvent.AuditEventBuilder traceId(final String traceId) {
      this.traceId = traceId;
      return this;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    public java.lang.String toString() {
      return "AuditEvent.AuditEventBuilder(eventId="
          + this.eventId
          + ", timestamp="
          + this.timestamp
          + ", actorId="
          + this.actorId
          + ", actorType="
          + this.actorType
          + ", action="
          + this.action
          + ", resourceType="
          + this.resourceType
          + ", resourceId="
          + this.resourceId
          + ", outcome="
          + this.outcome
          + ", lob="
          + this.lob
          + ", journeyId="
          + this.journeyId
          + ", distributorId="
          + this.distributorId
          + ", agentId="
          + this.agentId
          + ", traceId="
          + this.traceId
          + ", metadata="
          + this.metadata
          + ")";
    }
  }

  @java.lang.SuppressWarnings("all")
  @MechanicallyGenerated
  AuditEvent(
      final String eventId,
      final Instant timestamp,
      final String actorId,
      final String actorType,
      final String action,
      final String resourceType,
      final String resourceId,
      final String outcome,
      final String lob,
      final String journeyId,
      final String distributorId,
      final String agentId,
      final String traceId,
      final Map<String, String> metadata) {
    if (actorId == null) {
      throw new java.lang.NullPointerException("actorId is marked non-null but is null");
    }
    if (action == null) {
      throw new java.lang.NullPointerException("action is marked non-null but is null");
    }
    if (outcome == null) {
      throw new java.lang.NullPointerException("outcome is marked non-null but is null");
    }
    this.eventId = eventId;
    this.timestamp = timestamp;
    this.actorId = actorId;
    this.actorType = actorType;
    this.action = action;
    this.resourceType = resourceType;
    this.resourceId = resourceId;
    this.outcome = outcome;
    this.lob = lob;
    this.journeyId = journeyId;
    this.distributorId = distributorId;
    this.agentId = agentId;
    this.traceId = traceId;
    this.metadata = metadata;
  }

  @java.lang.SuppressWarnings("all")
  @MechanicallyGenerated
  public static AuditEvent.AuditEventBuilder builder() {
    return new AuditEvent.AuditEventBuilder();
  }

  @java.lang.SuppressWarnings("all")
  @MechanicallyGenerated
  public String getEventId() {
    return this.eventId;
  }

  @java.lang.SuppressWarnings("all")
  @MechanicallyGenerated
  public Instant getTimestamp() {
    return this.timestamp;
  }

  @java.lang.SuppressWarnings("all")
  @MechanicallyGenerated
  public String getActorId() {
    return this.actorId;
  }

  @java.lang.SuppressWarnings("all")
  @MechanicallyGenerated
  public String getActorType() {
    return this.actorType;
  }

  @java.lang.SuppressWarnings("all")
  @MechanicallyGenerated
  public String getAction() {
    return this.action;
  }

  @java.lang.SuppressWarnings("all")
  @MechanicallyGenerated
  public String getResourceType() {
    return this.resourceType;
  }

  @java.lang.SuppressWarnings("all")
  @MechanicallyGenerated
  public String getResourceId() {
    return this.resourceId;
  }

  @java.lang.SuppressWarnings("all")
  @MechanicallyGenerated
  public String getOutcome() {
    return this.outcome;
  }

  @java.lang.SuppressWarnings("all")
  @MechanicallyGenerated
  public String getLob() {
    return this.lob;
  }

  @java.lang.SuppressWarnings("all")
  @MechanicallyGenerated
  public String getJourneyId() {
    return this.journeyId;
  }

  @java.lang.SuppressWarnings("all")
  @MechanicallyGenerated
  public String getDistributorId() {
    return this.distributorId;
  }

  @java.lang.SuppressWarnings("all")
  @MechanicallyGenerated
  public String getAgentId() {
    return this.agentId;
  }

  @java.lang.SuppressWarnings("all")
  @MechanicallyGenerated
  public String getTraceId() {
    return this.traceId;
  }

  @java.lang.SuppressWarnings("all")
  @MechanicallyGenerated
  public Map<String, String> getMetadata() {
    return this.metadata;
  }

  @java.lang.Override
  @java.lang.SuppressWarnings("all")
  @MechanicallyGenerated
  public boolean equals(final java.lang.Object o) {
    if (o == this) {
      return true;
    }
    if (!(o instanceof AuditEvent)) {
      return false;
    }
    final AuditEvent other = (AuditEvent) o;
    final java.lang.Object this$eventId = this.getEventId();
    final java.lang.Object other$eventId = other.getEventId();
    if (this$eventId == null ? other$eventId != null : !this$eventId.equals(other$eventId)) {
      return false;
    }
    final java.lang.Object this$timestamp = this.getTimestamp();
    final java.lang.Object other$timestamp = other.getTimestamp();
    if (this$timestamp == null
        ? other$timestamp != null
        : !this$timestamp.equals(other$timestamp)) {
      return false;
    }
    final java.lang.Object this$actorId = this.getActorId();
    final java.lang.Object other$actorId = other.getActorId();
    if (this$actorId == null ? other$actorId != null : !this$actorId.equals(other$actorId)) {
      return false;
    }
    final java.lang.Object this$actorType = this.getActorType();
    final java.lang.Object other$actorType = other.getActorType();
    if (this$actorType == null
        ? other$actorType != null
        : !this$actorType.equals(other$actorType)) {
      return false;
    }
    final java.lang.Object this$action = this.getAction();
    final java.lang.Object other$action = other.getAction();
    if (this$action == null ? other$action != null : !this$action.equals(other$action)) {
      return false;
    }
    final java.lang.Object this$resourceType = this.getResourceType();
    final java.lang.Object other$resourceType = other.getResourceType();
    if (this$resourceType == null
        ? other$resourceType != null
        : !this$resourceType.equals(other$resourceType)) {
      return false;
    }
    final java.lang.Object this$resourceId = this.getResourceId();
    final java.lang.Object other$resourceId = other.getResourceId();
    if (this$resourceId == null
        ? other$resourceId != null
        : !this$resourceId.equals(other$resourceId)) {
      return false;
    }
    final java.lang.Object this$outcome = this.getOutcome();
    final java.lang.Object other$outcome = other.getOutcome();
    if (this$outcome == null ? other$outcome != null : !this$outcome.equals(other$outcome)) {
      return false;
    }
    final java.lang.Object this$lob = this.getLob();
    final java.lang.Object other$lob = other.getLob();
    if (this$lob == null ? other$lob != null : !this$lob.equals(other$lob)) {
      return false;
    }
    final java.lang.Object this$journeyId = this.getJourneyId();
    final java.lang.Object other$journeyId = other.getJourneyId();
    if (this$journeyId == null
        ? other$journeyId != null
        : !this$journeyId.equals(other$journeyId)) {
      return false;
    }
    final java.lang.Object this$distributorId = this.getDistributorId();
    final java.lang.Object other$distributorId = other.getDistributorId();
    if (this$distributorId == null
        ? other$distributorId != null
        : !this$distributorId.equals(other$distributorId)) {
      return false;
    }
    final java.lang.Object this$agentId = this.getAgentId();
    final java.lang.Object other$agentId = other.getAgentId();
    if (this$agentId == null ? other$agentId != null : !this$agentId.equals(other$agentId)) {
      return false;
    }
    final java.lang.Object this$traceId = this.getTraceId();
    final java.lang.Object other$traceId = other.getTraceId();
    if (this$traceId == null ? other$traceId != null : !this$traceId.equals(other$traceId)) {
      return false;
    }
    final java.lang.Object this$metadata = this.getMetadata();
    final java.lang.Object other$metadata = other.getMetadata();
    if (this$metadata == null ? other$metadata != null : !this$metadata.equals(other$metadata)) {
      return false;
    }
    return true;
  }

  @java.lang.Override
  @java.lang.SuppressWarnings("all")
  @MechanicallyGenerated
  public int hashCode() {
    final int PRIME = 59;
    int result = 1;
    final java.lang.Object $eventId = this.getEventId();
    result = result * PRIME + ($eventId == null ? 43 : $eventId.hashCode());
    final java.lang.Object $timestamp = this.getTimestamp();
    result = result * PRIME + ($timestamp == null ? 43 : $timestamp.hashCode());
    final java.lang.Object $actorId = this.getActorId();
    result = result * PRIME + ($actorId == null ? 43 : $actorId.hashCode());
    final java.lang.Object $actorType = this.getActorType();
    result = result * PRIME + ($actorType == null ? 43 : $actorType.hashCode());
    final java.lang.Object $action = this.getAction();
    result = result * PRIME + ($action == null ? 43 : $action.hashCode());
    final java.lang.Object $resourceType = this.getResourceType();
    result = result * PRIME + ($resourceType == null ? 43 : $resourceType.hashCode());
    final java.lang.Object $resourceId = this.getResourceId();
    result = result * PRIME + ($resourceId == null ? 43 : $resourceId.hashCode());
    final java.lang.Object $outcome = this.getOutcome();
    result = result * PRIME + ($outcome == null ? 43 : $outcome.hashCode());
    final java.lang.Object $lob = this.getLob();
    result = result * PRIME + ($lob == null ? 43 : $lob.hashCode());
    final java.lang.Object $journeyId = this.getJourneyId();
    result = result * PRIME + ($journeyId == null ? 43 : $journeyId.hashCode());
    final java.lang.Object $distributorId = this.getDistributorId();
    result = result * PRIME + ($distributorId == null ? 43 : $distributorId.hashCode());
    final java.lang.Object $agentId = this.getAgentId();
    result = result * PRIME + ($agentId == null ? 43 : $agentId.hashCode());
    final java.lang.Object $traceId = this.getTraceId();
    result = result * PRIME + ($traceId == null ? 43 : $traceId.hashCode());
    final java.lang.Object $metadata = this.getMetadata();
    result = result * PRIME + ($metadata == null ? 43 : $metadata.hashCode());
    return result;
  }

  @java.lang.Override
  @java.lang.SuppressWarnings("all")
  @MechanicallyGenerated
  public java.lang.String toString() {
    return "AuditEvent(eventId="
        + this.getEventId()
        + ", actorId="
        + this.getActorId()
        + ", action="
        + this.getAction()
        + ", outcome="
        + this.getOutcome()
        + ")";
  }
}
