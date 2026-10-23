package com.bank.platform.adminconfig.application;

import com.bank.platform.adminconfig.domain.OutboxPort;
import com.bank.platform.adminconfig.domain.RuleSetVersion;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Instant;

/**
 * {@code RuleSetPublished} in the event-envelope/v1 shape (ADR-025 point 1). Deciding services
 * invalidate their L1 cache on it (ADR-024 section 5 point 4). Ids and states only.
 */
final class RuleSetPublishedEvent {

  static final String EVENT_TYPE = "RuleSetPublished";
  static final String AGGREGATE_TYPE = "RuleSet";

  private RuleSetPublishedEvent() {}

  static OutboxPort.OutboxEvent of(
      String eventId,
      RuleSetVersion.Snapshot v,
      String checker,
      Instant occurredAt,
      String correlationId) {
    ObjectNode env = RuleSetAdministrationService.object();
    env.put("eventId", eventId);
    env.put("eventType", EVENT_TYPE);
    env.put("schemaVersion", 1);
    env.put("source", "admin-config");
    env.put("aggregateType", AGGREGATE_TYPE);
    env.put("aggregateId", v.ruleSetId());
    env.put("occurredAt", occurredAt.toString());
    env.putNull("journeyId");
    env.putNull("leadId");
    ObjectNode actor = env.putObject("actor");
    actor.put("id", checker);
    actor.put("type", "WORKFORCE");
    actor.put("actingCapacity", "CHECKER");
    env.putNull("traceId");
    env.put("correlationId", correlationId);
    env.put("configVersionRef", v.ruleSetId() + "@" + v.version());
    ObjectNode data = env.putObject("data");
    data.put("ruleSetId", v.ruleSetId());
    data.put("version", v.version());
    data.put("versionId", v.id());
    data.put("domain", v.domain());
    data.put("scopeLob", v.scopeLob());
    data.put("scopeProductClass", v.scopeProductClass());
    data.put("classification", v.classification().name());
    data.put("effectiveFrom", v.effectiveFrom().toString());
    data.put("effectiveTo", v.effectiveTo() == null ? null : v.effectiveTo().toString());
    data.put("documentSha256", v.documentSha256());
    return new OutboxPort.OutboxEvent(
        eventId,
        AGGREGATE_TYPE,
        v.ruleSetId(),
        EVENT_TYPE,
        RuleSetAdministrationService.write(env),
        occurredAt);
  }
}
