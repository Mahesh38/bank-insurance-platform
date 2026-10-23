package com.bank.common.events;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * {@code event-envelope/v1} (ADR-025 §1). Schema: {@code
 * docs/platform/contracts/events/schemas/event-envelope.v1.json}. {@code data} holds ids and states
 * only — never customer PII; the per-event schema is {@code schemas/<eventType>.v<n>.json}.
 */
public record EventEnvelope(
    String eventId,
    String eventType,
    int schemaVersion,
    String source,
    String aggregateType,
    String aggregateId,
    Instant occurredAt,
    String journeyId,
    String leadId,
    Actor actor,
    String traceId,
    String correlationId,
    String configVersionRef,
    Map<String, Object> data) {

  private static final Pattern EVENT_TYPE = Pattern.compile("^[A-Z][A-Za-z0-9]{2,95}$");
  private static final Pattern SOURCE = Pattern.compile("^[a-z][a-z0-9-]{1,47}$");
  private static final Pattern AGGREGATE_TYPE = Pattern.compile("^[A-Z][A-Z0-9_]{1,63}$");

  public EventEnvelope {
    requireUlid("eventId", eventId);
    require("eventType", eventType, EVENT_TYPE);
    if (schemaVersion < 1) {
      throw new IllegalArgumentException("schemaVersion must be >= 1");
    }
    require("source", source, SOURCE);
    require("aggregateType", aggregateType, AGGREGATE_TYPE);
    if (aggregateId == null || aggregateId.isBlank() || aggregateId.length() > 64) {
      throw new IllegalArgumentException("aggregateId is required (<= 64 chars)");
    }
    Objects.requireNonNull(occurredAt, "occurredAt");
    // The JSON form is millisecond RFC 3339; truncating here keeps a round trip lossless.
    occurredAt = occurredAt.truncatedTo(ChronoUnit.MILLIS);
    if (journeyId != null) {
      requireUlid("journeyId", journeyId);
    }
    if (leadId != null) {
      requireUlid("leadId", leadId);
    }
    Objects.requireNonNull(actor, "actor");
    requireText("traceId", traceId);
    requireText("correlationId", correlationId);
    Objects.requireNonNull(data, "data");
    data = Collections.unmodifiableMap(new LinkedHashMap<>(data));
  }

  public static Builder builder() {
    return new Builder();
  }

  private static void require(String field, String value, Pattern pattern) {
    if (value == null || !pattern.matcher(value).matches()) {
      throw new IllegalArgumentException(field + " is missing or malformed");
    }
  }

  private static void requireUlid(String field, String value) {
    if (!Ulid.isValid(value)) {
      throw new IllegalArgumentException(field + " must be a ULID");
    }
  }

  private static void requireText(String field, String value) {
    if (value == null || value.isBlank() || value.length() > 128) {
      throw new IllegalArgumentException(field + " is required (<= 128 chars)");
    }
  }

  /** Mints {@code eventId} and {@code occurredAt} from the clock when not set explicitly. */
  public static final class Builder {
    private Clock clock = Clock.systemUTC();
    private String eventId;
    private String eventType;
    private int schemaVersion = 1;
    private String source;
    private String aggregateType;
    private String aggregateId;
    private Instant occurredAt;
    private String journeyId;
    private String leadId;
    private Actor actor;
    private String traceId;
    private String correlationId;
    private String configVersionRef;
    private final Map<String, Object> data = new LinkedHashMap<>();

    private Builder() {}

    public Builder clock(Clock value) {
      this.clock = Objects.requireNonNull(value);
      return this;
    }

    public Builder eventId(String value) {
      this.eventId = value;
      return this;
    }

    public Builder eventType(String value) {
      this.eventType = value;
      return this;
    }

    public Builder schemaVersion(int value) {
      this.schemaVersion = value;
      return this;
    }

    public Builder source(String value) {
      this.source = value;
      return this;
    }

    public Builder aggregate(String type, String id) {
      this.aggregateType = type;
      this.aggregateId = id;
      return this;
    }

    public Builder occurredAt(Instant value) {
      this.occurredAt = value;
      return this;
    }

    public Builder journeyId(String value) {
      this.journeyId = value;
      return this;
    }

    public Builder leadId(String value) {
      this.leadId = value;
      return this;
    }

    public Builder actor(Actor value) {
      this.actor = value;
      return this;
    }

    public Builder traceId(String value) {
      this.traceId = value;
      return this;
    }

    public Builder correlationId(String value) {
      this.correlationId = value;
      return this;
    }

    public Builder configVersionRef(String value) {
      this.configVersionRef = value;
      return this;
    }

    /** Null values are kept: a nullable field in the event schema is present as JSON null. */
    public Builder data(String key, Object value) {
      this.data.put(Objects.requireNonNull(key), value);
      return this;
    }

    public Builder data(Map<String, ?> values) {
      values.forEach(this::data);
      return this;
    }

    public EventEnvelope build() {
      return new EventEnvelope(
          eventId == null ? Ulid.mint(clock) : eventId,
          eventType,
          schemaVersion,
          source,
          aggregateType,
          aggregateId,
          occurredAt == null ? clock.instant() : occurredAt,
          journeyId,
          leadId,
          actor,
          traceId,
          correlationId,
          configVersionRef,
          data);
    }
  }
}
