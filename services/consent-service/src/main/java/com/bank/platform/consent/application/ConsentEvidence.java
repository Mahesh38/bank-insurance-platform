package com.bank.platform.consent.application;

import com.bank.common.events.Actor;
import com.bank.common.events.EventEnvelope;
import com.bank.common.events.EventJsonCodec;
import com.bank.platform.consent.application.ConsentTypes.ChallengeView;
import com.bank.platform.consent.application.ConsentTypes.ConsentView;
import com.bank.platform.consent.application.ConsentTypes.VerifyCommand;
import com.bank.platform.consent.domain.ChallengeRow;
import com.bank.platform.consent.domain.ConsentPorts;
import com.bank.platform.consent.domain.ConsentRefusal;
import com.bank.platform.consent.domain.ConsentRow;
import com.bank.platform.consent.domain.FieldCipher;
import com.bank.platform.consent.domain.PolicySnapshot;
import com.bank.platform.consent.domain.StatementDoc;
import com.bank.platform.consent.domain.Ulids;
import com.bank.rules.engine.Values;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.stereotype.Component;

/** Append-only evidence writes, outbox events, and the views those writes return. */
@Component
class ConsentEvidence {

  private final ConsentPorts.ConsentStore consents;
  private final ConsentPorts.Outbox outbox;
  private final ConsentPorts.PolicySource policies;
  private final FieldCipher cipher;
  private final EventJsonCodec codec = new EventJsonCodec();
  private final Clock clock;

  ConsentEvidence(
      ConsentPorts.ConsentStore consents,
      ConsentPorts.Outbox outbox,
      ConsentPorts.PolicySource policies,
      FieldCipher cipher,
      Clock clock) {
    this.consents = consents;
    this.outbox = outbox;
    this.policies = policies;
    this.cipher = cipher;
    this.clock = clock;
  }

  String grantPurpose(
      ChallengeRow row,
      String purpose,
      JsonNode ref,
      String sourceIp,
      Instant now,
      PolicySnapshot policy) {
    PolicySnapshot.CheckpointRule rule = policy.checkpoint(row.checkpoint());
    String insurer = "CNS-SHR".equals(purpose) ? row.insurerCode() : null;
    ConsentRow previous = consents.head(row.customerId(), purpose, insurer).orElse(null);
    String supersedes = null;
    if (previous != null && "GRANTED".equals(previous.state())) {
      consents.insert(
          copyState(
              previous, previous.recordVersion() + 1, "SUPERSEDED", previous.consentId(), now));
      supersedes = previous.consentId();
    }
    String consentId = Ulids.mint(clock);
    Instant until =
        rule.validityDays() == null
            ? null
            : LocalDate.ofInstant(now, ZoneOffset.UTC)
                .plusDays(rule.validityDays() + 1L)
                .atStartOfDay()
                .toInstant(ZoneOffset.UTC);
    ConsentRow granted =
        new ConsentRow(
            consentId,
            1,
            row.customerId(),
            row.leadId(),
            row.journeyId(),
            row.lob(),
            purpose,
            insurer,
            ref.path("statementId").asText(),
            ref.path("statementVersion").asText(),
            ref.path("text").asText(),
            ref.path("statementTextHash").asText(),
            row.language(),
            row.channel(),
            ConsentInput.ACK,
            row.checkpoint(),
            "GRANTED",
            now,
            now,
            row.otpTxnId(),
            sourceIp,
            row.contactUsedEnc(),
            row.encryptionKeyId(),
            row.actorId(),
            now,
            until,
            supersedes,
            null,
            null,
            null,
            null,
            null,
            policy.versionRef(),
            now.plusMillis(1));
    consents.insert(granted);
    emitGranted(granted, "request", "request");
    return consentId;
  }

  String withdraw(
      ChallengeRow row,
      VerifyCommand command,
      String actorId,
      Instant now,
      PolicySnapshot policy,
      String correlationId) {
    ConsentRow head =
        consents
            .headOf(row.targetConsentId())
            .orElseThrow(() -> new ConsentRefusal("RESOURCE_NOT_FOUND", "consent was not found"));
    if (!"GRANTED".equals(head.state())) {
      throw new ConsentRefusal("ILLEGAL_TRANSITION", "only a granted consent can be withdrawn");
    }
    String reason = reasonOn(row.statementRefs());
    ConsentRow withdrawn =
        new ConsentRow(
            head.consentId(),
            head.recordVersion() + 1,
            head.customerId(),
            head.leadId(),
            head.journeyId(),
            head.lob(),
            head.purpose(),
            head.insurerCode(),
            head.statementId(),
            head.statementVersion(),
            head.statementText(),
            head.statementTextHash(),
            head.language(),
            head.channel(),
            ConsentInput.ACK,
            head.checkpoint(),
            "WITHDRAWN",
            head.capturedAt(),
            now,
            row.otpTxnId(),
            command.sourceIp(),
            head.contactUsedEnc(),
            head.encryptionKeyId(),
            head.capturedByActorId(),
            head.validFrom(),
            head.validUntil(),
            head.consentId(),
            now,
            actorId,
            row.channel(),
            reason,
            row.otpTxnId(),
            policy.versionRef(),
            now);
    consents.insert(withdrawn);
    EventEnvelope event =
        base("ConsentWithdrawn", withdrawn.consentId(), withdrawn, actorId, correlationId, policy)
            .data("consentId", withdrawn.consentId())
            .data("customerId", withdrawn.customerId())
            .data("purpose", withdrawn.purpose())
            .data("reasonCode", reason)
            .data("withdrawnAt", now.truncatedTo(ChronoUnit.MILLIS).toString())
            .build();
    outbox.append(
        event.eventId(),
        event.aggregateType(),
        event.aggregateId(),
        event.eventType(),
        codec.encode(event),
        now);
    return withdrawn.consentId();
  }

  void emitExpired(ConsentRow expired, PolicySnapshot policy) {
    emit(
        "ConsentExpired",
        expired.consentId(),
        expired,
        null,
        "consent-sweep",
        "consent-sweep",
        policy);
  }

  ConsentRow copyState(ConsentRow head, int version, String state, String supersedes, Instant now) {
    return new ConsentRow(
        head.consentId(),
        version,
        head.customerId(),
        head.leadId(),
        head.journeyId(),
        head.lob(),
        head.purpose(),
        head.insurerCode(),
        head.statementId(),
        head.statementVersion(),
        head.statementText(),
        head.statementTextHash(),
        head.language(),
        head.channel(),
        head.acknowledgementMethod(),
        head.checkpoint(),
        state,
        head.capturedAt(),
        head.otpVerifiedAt(),
        head.otpTxnId(),
        head.sourceIp(),
        head.contactUsedEnc(),
        head.encryptionKeyId(),
        head.capturedByActorId(),
        head.validFrom(),
        head.validUntil(),
        supersedes,
        head.withdrawnAt(),
        head.withdrawnBy(),
        head.withdrawalChannel(),
        head.withdrawalReasonCode(),
        head.withdrawalOtpTxnId(),
        head.configVersionRef(),
        now);
  }

  void audit(
      ChallengeRow row,
      String action,
      String resourceType,
      String resourceId,
      String outcome,
      String newState,
      String actorId,
      String correlationId,
      PolicySnapshot policy) {
    EventEnvelope event =
        EventEnvelope.builder()
            .clock(clock)
            .eventType("AuditRecorded")
            .source("consent")
            .aggregate("CONSENT", resourceId)
            .journeyId(row.journeyId())
            .leadId(row.leadId())
            .actor(Actor.of(actorId, "BANK_RM", "SP_ACCOUNTABLE"))
            .traceId(blank(correlationId) ? "consent-trace" : correlationId)
            .correlationId(blank(correlationId) ? "consent-correlation" : correlationId)
            .configVersionRef(policy.versionRef())
            .data("action", action)
            .data("resourceType", resourceType)
            .data("resourceId", resourceId)
            .data("outcome", outcome)
            .data("newState", newState)
            .build();
    outbox.append(
        event.eventId(),
        "CONSENT",
        resourceId,
        event.eventType(),
        codec.encode(event),
        clock.instant());
  }

  String refs(List<StatementDoc> docs) {
    ArrayNode array = Values.mapper().createArrayNode();
    for (StatementDoc doc : docs) {
      ObjectNode node = array.addObject();
      node.put("purpose", doc.purpose());
      node.put("statementId", doc.statementId());
      node.put("statementVersion", doc.version());
      node.put("statementTextHash", doc.textHash());
      node.put("text", doc.text());
    }
    return array.toString();
  }

  JsonNode readRefs(String json) {
    try {
      return Values.mapper().readTree(json);
    } catch (Exception ex) {
      throw new ConsentRefusal("INTERNAL_ERROR", "statement reference could not be read");
    }
  }

  static JsonNode refFor(JsonNode refs, String purpose) {
    for (JsonNode node : refs) {
      if (purpose.equals(node.path("purpose").asText())) {
        return node;
      }
    }
    throw new ConsentRefusal("VALIDATION_ERROR", "statement reference is missing");
  }

  ChallengeView challengeView(ChallengeRow row, int maxAttempts, List<String> consentIds) {
    String masked = mask(cipher.decrypt(row.contactUsedEnc()));
    int remaining = Math.max(0, maxAttempts - row.attempts());
    return new ChallengeView(
        row.challengeId(),
        row.kind(),
        row.state(),
        row.customerId(),
        row.purposes(),
        row.otpExpiresAt(),
        masked,
        remaining,
        consentIds);
  }

  ConsentView consentView(ConsentRow row) {
    return new ConsentView(
        row.consentId(),
        row.recordVersion(),
        row.customerId(),
        row.leadId(),
        row.journeyId(),
        row.lob(),
        row.purpose(),
        row.insurerCode(),
        row.state(),
        row.statementId(),
        row.statementVersion(),
        row.statementTextHash(),
        row.language(),
        row.channel(),
        row.acknowledgementMethod(),
        row.checkpoint(),
        row.capturedAt(),
        row.validFrom(),
        row.validUntil(),
        row.supersedesConsentId(),
        row.withdrawnAt(),
        row.configVersionRef());
  }

  static ChallengeRow withReason(ChallengeRow row, String reasonCode) {
    String reason = blank(reasonCode) ? "CUSTOMER_REQUEST" : reasonCode;
    String refs = row.statementRefs().substring(0, row.statementRefs().length() - 1);
    if (refs.endsWith("}")) {
      refs = refs.substring(0, refs.length() - 1) + ",\"reasonCode\":\"" + reason + "\"}]";
    }
    return new ChallengeRow(
        row.challengeId(),
        row.kind(),
        row.state(),
        row.attempts(),
        row.otpIssueCount(),
        row.otpExpiresAt(),
        row.otpTxnId(),
        row.otpHash(),
        row.purposes(),
        refs,
        row.contactUsedEnc(),
        row.encryptionKeyId(),
        row.customerId(),
        row.journeyId(),
        row.leadId(),
        row.lob(),
        row.checkpoint(),
        row.insurerCode(),
        row.channel(),
        row.language(),
        row.targetConsentId(),
        row.actorId(),
        row.configVersionRef(),
        row.createdAt());
  }

  private void emitGranted(ConsentRow row, String actorId, String correlationId) {
    EventEnvelope.Builder builder =
        base("ConsentGranted", row.consentId(), row, actorId, correlationId, policies.current())
            .data("consentId", row.consentId())
            .data("customerId", row.customerId())
            .data("purpose", row.purpose())
            .data("checkpoint", row.checkpoint())
            .data("statementVersion", row.statementVersion())
            .data(
                "validUntil",
                row.validUntil() == null
                    ? null
                    : row.validUntil().truncatedTo(ChronoUnit.MILLIS).toString());
    if (row.insurerCode() != null) {
      builder.data("insurerCode", row.insurerCode());
    }
    EventEnvelope built = builder.build();
    outbox.append(
        built.eventId(),
        "CONSENT",
        row.consentId(),
        built.eventType(),
        codec.encode(built),
        row.createdAt());
  }

  private void emit(
      String type,
      String consentId,
      ConsentRow row,
      String actorId,
      String correlationId,
      String trace,
      PolicySnapshot policy) {
    EventEnvelope built =
        base(type, consentId, row, actorId == null ? "consent" : actorId, correlationId, policy)
            .traceId(trace)
            .data("consentId", consentId)
            .data("customerId", row.customerId())
            .data("purpose", row.purpose())
            .build();
    outbox.append(
        built.eventId(), "CONSENT", consentId, type, codec.encode(built), clock.instant());
  }

  private EventEnvelope.Builder base(
      String type,
      String aggregateId,
      ConsentRow row,
      String actorId,
      String correlationId,
      PolicySnapshot policy) {
    return EventEnvelope.builder()
        .clock(clock)
        .eventType(type)
        .source("consent")
        .aggregate("CONSENT", aggregateId)
        .journeyId(row.journeyId())
        .leadId(row.leadId())
        .actor(Actor.of(actorId, "BANK_RM", "SP_ACCOUNTABLE"))
        .traceId(correlationId == null || correlationId.isBlank() ? "consent-trace" : correlationId)
        .correlationId(
            correlationId == null || correlationId.isBlank()
                ? "consent-correlation"
                : correlationId)
        .configVersionRef(policy.versionRef());
  }

  private static String reasonOn(String refs) {
    int marker = refs.indexOf("\"reasonCode\":\"");
    if (marker < 0) {
      return "CUSTOMER_REQUEST";
    }
    int start = marker + "\"reasonCode\":\"".length();
    int end = refs.indexOf('"', start);
    return refs.substring(start, end);
  }

  private static String mask(String digits) {
    String local = digits.length() > 10 ? digits.substring(digits.length() - 10) : digits;
    return "+91 " + local.substring(0, 3) + "****" + local.substring(7);
  }

  private static boolean blank(String value) {
    return ConsentInput.blank(value);
  }
}
