package com.bank.common.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Envelope ⇄ JSON, field names and order per {@code event-envelope.v1.json}. Every envelope field
 * is written, nullable ones as JSON {@code null}, so the schema's {@code required} list holds.
 * Instants are written by hand (ISO-8601 UTC, millisecond) so callers need no JSR-310 module.
 */
public final class EventJsonCodec {

  private static final TypeReference<LinkedHashMap<String, Object>> MAP = new TypeReference<>() {};

  private final ObjectMapper mapper;

  public EventJsonCodec() {
    this(new ObjectMapper());
  }

  public EventJsonCodec(ObjectMapper mapper) {
    this.mapper = mapper;
  }

  public String encode(EventEnvelope e) {
    Map<String, Object> actor = new LinkedHashMap<>();
    actor.put("id", e.actor().id());
    actor.put("type", e.actor().type());
    actor.put("actingCapacity", e.actor().actingCapacity());
    if (e.actor().actorInsurerId() != null || e.actor().assistedActorId() != null) {
      actor.put("actorInsurerId", e.actor().actorInsurerId());
      actor.put("assistedActorId", e.actor().assistedActorId());
    }
    Map<String, Object> json = new LinkedHashMap<>();
    json.put("eventId", e.eventId());
    json.put("eventType", e.eventType());
    json.put("schemaVersion", e.schemaVersion());
    json.put("source", e.source());
    json.put("aggregateType", e.aggregateType());
    json.put("aggregateId", e.aggregateId());
    json.put("occurredAt", DateTimeFormatter.ISO_INSTANT.format(e.occurredAt()));
    json.put("journeyId", e.journeyId());
    json.put("leadId", e.leadId());
    json.put("actor", actor);
    json.put("traceId", e.traceId());
    json.put("correlationId", e.correlationId());
    json.put("configVersionRef", e.configVersionRef());
    json.put("data", e.data());
    try {
      return mapper.writeValueAsString(json);
    } catch (JsonProcessingException ex) {
      throw new EventCodecException("event " + e.eventId() + " is not serialisable", ex);
    }
  }

  public EventEnvelope decode(String text) {
    Map<String, Object> json;
    try {
      json = mapper.readValue(text, MAP);
    } catch (JsonProcessingException ex) {
      throw new EventCodecException("event JSON is not readable", ex);
    }
    try {
      Map<String, Object> actor = map(json.get("actor"));
      Object version = json.get("schemaVersion");
      return new EventEnvelope(
          str(json, "eventId"),
          str(json, "eventType"),
          version instanceof Number n ? n.intValue() : 0,
          str(json, "source"),
          str(json, "aggregateType"),
          str(json, "aggregateId"),
          Instant.parse(str(json, "occurredAt")),
          str(json, "journeyId"),
          str(json, "leadId"),
          new Actor(
              str(actor, "id"),
              str(actor, "type"),
              str(actor, "actingCapacity"),
              str(actor, "actorInsurerId"),
              str(actor, "assistedActorId")),
          str(json, "traceId"),
          str(json, "correlationId"),
          str(json, "configVersionRef"),
          map(json.get("data")));
    } catch (IllegalArgumentException | NullPointerException | DateTimeParseException ex) {
      throw new EventCodecException("event JSON violates event-envelope/v1", ex);
    }
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> map(Object value) {
    if (!(value instanceof Map<?, ?>)) {
      throw new IllegalArgumentException("expected a JSON object");
    }
    return (Map<String, Object>) value;
  }

  private static String str(Map<String, Object> json, String key) {
    Object value = json.get(key);
    return value == null ? null : value.toString();
  }
}
