package com.bank.platform.consent.application;

import com.bank.common.events.Actor;
import com.bank.common.events.EventEnvelope;
import com.bank.common.events.EventJsonCodec;
import com.bank.platform.consent.domain.ChallengeRow;
import com.bank.platform.consent.domain.ConsentPorts;
import com.bank.platform.consent.domain.ConsentRefusal;
import com.bank.platform.consent.domain.ConsentRow;
import com.bank.platform.consent.domain.FieldCipher;
import com.bank.platform.consent.domain.OtpDispatch;
import com.bank.platform.consent.domain.OtpHasher;
import com.bank.platform.consent.domain.PolicySnapshot;
import com.bank.platform.consent.domain.SecurityEventSink;
import com.bank.platform.consent.domain.StatementDoc;
import com.bank.platform.consent.domain.Ulids;
import com.bank.rules.engine.Values;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The eight consent operations. Checkpoint behaviour comes from the resolved policy version. */
@Service
public class ConsentApplicationService {

  private static final Set<String> PURPOSES =
      Set.of("CNS-DP", "CNS-SOL", "CNS-QSH", "CNS-SHR", "CNS-COM");
  private static final Set<String> CHANNELS = Set.of("RM_ASSISTED", "SELF_SERVICE", "HYBRID");
  private static final Set<String> LOBS = Set.of("LIFE", "HEALTH", "GENERAL");
  private static final String ACK = "OTP_SMS_CUSTOMER_DEVICE";

  private final ConsentApplicationService self;
  private final ConsentPorts.PolicySource policies;
  private final ConsentPorts.StatementSource statements;
  private final ConsentPorts.RegisteredContact contacts;
  private final ConsentPorts.ConsentStore consents;
  private final ConsentPorts.ChallengeStore challenges;
  private final ConsentPorts.IssueLedger issues;
  private final ConsentPorts.Outbox outbox;
  private final OtpAttemptRecorder attempts;
  private final FieldCipher cipher;
  private final OtpHasher hasher;
  private final OtpDispatch dispatch;
  private final SecurityEventSink security;
  private final EventJsonCodec codec = new EventJsonCodec();
  private final Clock clock;

  public ConsentApplicationService(
      @Lazy ConsentApplicationService self,
      ConsentPorts.PolicySource policies,
      ConsentPorts.StatementSource statements,
      ConsentPorts.RegisteredContact contacts,
      ConsentPorts.ConsentStore consents,
      ConsentPorts.ChallengeStore challenges,
      ConsentPorts.IssueLedger issues,
      ConsentPorts.Outbox outbox,
      OtpAttemptRecorder attempts,
      FieldCipher cipher,
      OtpHasher hasher,
      OtpDispatch dispatch,
      SecurityEventSink security,
      Clock clock) {
    this.self = self;
    this.policies = policies;
    this.statements = statements;
    this.contacts = contacts;
    this.consents = consents;
    this.challenges = challenges;
    this.issues = issues;
    this.outbox = outbox;
    this.attempts = attempts;
    this.cipher = cipher;
    this.hasher = hasher;
    this.dispatch = dispatch;
    this.security = security;
    this.clock = clock;
  }

  public CheckView check(
      String customerId, String checkpoint, String lob, String insurerCode, String journeyId) {
    requireUlid("customerId", customerId);
    if (journeyId != null) {
      requireUlid("journeyId", journeyId);
    }
    if (!LOBS.contains(lob)) {
      throw new ConsentRefusal("VALIDATION_ERROR", "lob is not supported");
    }
    PolicySnapshot policy = policies.current();
    PolicySnapshot.CheckpointRule rule = policy.checkpoint(checkpoint);
    if (rule.namedInsurer() && (insurerCode == null || insurerCode.isBlank())) {
      throw new ConsentRefusal("MISSING_REQUIRED_FIELD", "insurer code is required");
    }
    Instant now = clock.instant();
    ObjectNode inputs = Values.mapper().createObjectNode();
    inputs.put("asOf", LocalDate.ofInstant(now, ZoneOffset.UTC).toString());
    inputs.put("checkpoint", checkpoint);
    List<PurposeResult> results = new ArrayList<>();
    boolean blocked = false;
    for (String purpose : rule.purposes()) {
      StatementDoc statement = statements.current(purpose, "en-IN");
      String scopeKey = rule.namedInsurer() ? insurerCode : null;
      ConsentRow head = consents.head(customerId, purpose, scopeKey).orElse(null);
      inputs.put(statusKey(purpose), statement.status());
      if (head != null && "GRANTED".equals(head.state()) && !expired(head, rule, now)) {
        inputs.put(
            grantedKey(purpose), LocalDate.ofInstant(head.validFrom(), ZoneOffset.UTC).toString());
      } else {
        inputs.putNull(grantedKey(purpose));
      }
      String reason = reasonFor(head, statement, policy, rule, insurerCode, now);
      if (reason == null) {
        results.add(PurposeResult.valid(head));
      } else {
        if ("STATEMENT_NOT_APPROVED".equals(reason)) {
          blocked = true;
        }
        results.add(PurposeResult.missing(purpose, reason, statement, head));
      }
    }
    Map<String, String> facts = new LinkedHashMap<>();
    inputs
        .fields()
        .forEachRemaining(
            entry ->
                facts.put(
                    entry.getKey(), entry.getValue().isNull() ? null : entry.getValue().asText()));
    if ("CAPTURE_BLOCKED".equals(policies.evaluateOutcome(facts, now))) {
      blocked = true;
    }
    List<PurposeResult> missing = results.stream().filter(PurposeResult::missing).toList();
    List<PurposeResult> valid = results.stream().filter(item -> !item.missing()).toList();
    return new CheckView(
        customerId,
        checkpoint,
        missing.isEmpty(),
        blocked,
        policy.ruleSetId(),
        policy.version(),
        valid,
        missing);
  }

  @Transactional
  public ChallengeView capture(CaptureCommand command, String actorId, String correlationId) {
    validateCapture(command);
    PolicySnapshot policy = policies.current();
    PolicySnapshot.CheckpointRule rule = policy.checkpoint(command.checkpoint());
    if (policy.separateActions() && command.purposes().size() > 1) {
      throw new ConsentRefusal("VALIDATION_ERROR", "each purpose needs its own acknowledgement");
    }
    for (String purpose : command.purposes()) {
      if (!rule.purposes().contains(purpose)) {
        throw new ConsentRefusal("VALIDATION_ERROR", "purpose is not required at this checkpoint");
      }
    }
    if (command.purposes().contains("CNS-SHR") && blank(command.insurerCode())) {
      throw new ConsentRefusal("MISSING_REQUIRED_FIELD", "insurer code is required");
    }
    List<StatementDoc> docs = new ArrayList<>();
    for (String purpose : command.purposes()) {
      StatementDoc doc = statements.current(purpose, command.language());
      if (!policy.requiredStatementStatus().equals(doc.status())) {
        throw new ConsentRefusal("CONSENT_STATEMENT_NOT_APPROVED", "statement is not approved");
      }
      docs.add(doc);
    }
    String digits = contacts.digits(command.customerId());
    Instant now = clock.instant();
    for (String purpose : command.purposes()) {
      int used = issues.countSince(command.customerId(), purpose, now.minus(Duration.ofHours(24)));
      if (used >= policy.maxIssuesPer24h()) {
        throw new ConsentRefusal("OTP_ISSUE_RATE_EXCEEDED", "code issue limit reached");
      }
    }
    String otp = hasher.mintCode();
    String challengeId = Ulids.mint(clock);
    String txn = Ulids.mint(clock);
    byte[] contact = cipher.encrypt(digits);
    for (String purpose : command.purposes()) {
      issues.record(command.customerId(), purpose, now);
    }
    ChallengeRow row =
        new ChallengeRow(
            challengeId,
            "CAPTURE",
            "OTP_PENDING",
            0,
            1,
            now.plus(Duration.ofMinutes(policy.otpValidityMinutes())),
            txn,
            hasher.hash(otp),
            List.copyOf(command.purposes()),
            refs(docs),
            contact,
            cipher.keyId(),
            command.customerId(),
            command.journeyId(),
            command.leadId(),
            command.lob(),
            command.checkpoint(),
            blank(command.insurerCode()) ? null : command.insurerCode(),
            command.channel(),
            command.language(),
            null,
            actorId,
            policy.versionRef(),
            now);
    challenges.insert(row);
    dispatch.send(challengeId, contact, otp);
    audit(
        row,
        "CAPTURE",
        "CHALLENGE",
        challengeId,
        "SUCCESS",
        "OTP_PENDING",
        actorId,
        correlationId,
        policy);
    return challengeView(row, policy.maxAttempts(), List.of());
  }

  public ChallengeView verify(VerifyCommand command, String actorId, String correlationId) {
    requireUlid("challengeId", command.challengeId());
    ChallengeRow row =
        challenges
            .find(command.challengeId())
            .orElseThrow(() -> new ConsentRefusal("RESOURCE_NOT_FOUND", "challenge was not found"));
    PolicySnapshot policy = policies.current();
    if ("LOCKED".equals(row.state()) || row.attempts() >= policy.maxAttempts()) {
      attempts.lock(row.challengeId());
      throw new ConsentRefusal("OTP_ATTEMPTS_EXCEEDED", "code is locked");
    }
    if (!"OTP_PENDING".equals(row.state())) {
      throw new ConsentRefusal("ILLEGAL_TRANSITION", "challenge cannot be verified");
    }
    if (!clock.instant().isBefore(row.otpExpiresAt())) {
      throw new ConsentRefusal("OTP_EXPIRED", "code window has elapsed");
    }
    if (!policy.verificationModes().contains(command.verificationMode())) {
      throw new ConsentRefusal("FORBIDDEN", "verification mode is not permitted");
    }
    if (!"CUSTOMER_DEVICE".equals(command.verificationMode())) {
      throw new ConsentRefusal("FORBIDDEN", "verification mode is not permitted");
    }
    if (command.otp() == null || !command.otp().matches("^[0-9]{4,8}$")) {
      throw new ConsentRefusal("VALIDATION_ERROR", "code format is not valid");
    }
    if (blank(command.sourceIp())) {
      throw new ConsentRefusal("VALIDATION_ERROR", "source address is required");
    }
    if (!row.otpTxnId().equals(command.otpTxnId())) {
      throw new ConsentRefusal("VALIDATION_ERROR", "code reference does not match");
    }
    requireAffirmations(row, command.affirmations());
    if (!hasher.matches(command.otp(), row.otpHash())) {
      ChallengeRow updated = attempts.recordFailure(row.challengeId(), policy.maxAttempts());
      if (updated.attempts() > policy.maxAttempts()) {
        throw new ConsentRefusal("OTP_ATTEMPTS_EXCEEDED", "code is locked");
      }
      throw new ConsentRefusal(
          "VALIDATION_ERROR",
          "code did not match",
          Math.max(0, policy.maxAttempts() - updated.attempts()));
    }
    return self.completeVerify(command, actorId, correlationId);
  }

  @Transactional
  public ChallengeView completeVerify(VerifyCommand command, String actorId, String correlationId) {
    ChallengeRow row = challenges.find(command.challengeId()).orElseThrow();
    PolicySnapshot policy = policies.current();
    Instant now = clock.instant();
    List<String> consentIds = new ArrayList<>();
    if ("CAPTURE".equals(row.kind())) {
      JsonNode refs = readRefs(row.statementRefs());
      for (String purpose : row.purposes()) {
        JsonNode ref = refFor(refs, purpose);
        consentIds.add(grantPurpose(row, purpose, ref, command.sourceIp(), now, policy));
      }
    } else {
      consentIds.add(withdraw(row, command, actorId, now, policy, correlationId));
    }
    ChallengeRow verified = row.withVerification("VERIFIED", row.attempts());
    challenges.update(verified);
    audit(
        row,
        "VERIFY",
        "CHALLENGE",
        row.challengeId(),
        "SUCCESS",
        "VERIFIED",
        actorId,
        correlationId,
        policy);
    return challengeView(verified, policy.maxAttempts(), consentIds);
  }

  @Transactional
  public ChallengeView resend(String challengeId, String actorId, String correlationId) {
    requireUlid("challengeId", challengeId);
    ChallengeRow row =
        challenges
            .find(challengeId)
            .orElseThrow(() -> new ConsentRefusal("RESOURCE_NOT_FOUND", "challenge was not found"));
    if (!"OTP_PENDING".equals(row.state()) && !"LOCKED".equals(row.state())) {
      throw new ConsentRefusal("ILLEGAL_TRANSITION", "challenge cannot be reissued");
    }
    PolicySnapshot policy = policies.current();
    Instant now = clock.instant();
    for (String purpose : row.purposes()) {
      int used = issues.countSince(row.customerId(), purpose, now.minus(Duration.ofHours(24)));
      if (used >= policy.maxIssuesPer24h()) {
        throw new ConsentRefusal("OTP_ISSUE_RATE_EXCEEDED", "code issue limit reached");
      }
    }
    String otp = hasher.mintCode();
    String txn = Ulids.mint(clock);
    for (String purpose : row.purposes()) {
      issues.record(row.customerId(), purpose, now);
    }
    ChallengeRow fresh =
        row.withFreshCode(
            "OTP_PENDING",
            now.plus(Duration.ofMinutes(policy.otpValidityMinutes())),
            txn,
            hasher.hash(otp),
            row.otpIssueCount() + 1);
    challenges.update(fresh);
    dispatch.send(fresh.challengeId(), fresh.contactUsedEnc(), otp);
    return challengeView(fresh, policy.maxAttempts(), List.of());
  }

  public ChallengeView getChallenge(String challengeId) {
    requireUlid("challengeId", challengeId);
    ChallengeRow row =
        challenges
            .find(challengeId)
            .orElseThrow(() -> new ConsentRefusal("RESOURCE_NOT_FOUND", "challenge was not found"));
    PolicySnapshot policy = policies.current();
    return challengeView(row, policy.maxAttempts(), List.of());
  }

  public ConsentView getConsent(
      String consentId, String customerId, String actorId, String journeyId) {
    requireUlid("consentId", consentId);
    if (customerId != null) {
      requireUlid("customerId", customerId);
    }
    ConsentRow head =
        consents
            .headOf(consentId)
            .orElseThrow(() -> new ConsentRefusal("RESOURCE_NOT_FOUND", "consent was not found"));
    if (customerId != null && !customerId.equals(head.customerId())) {
      security.subjectMismatch(
          actorId, journeyId == null ? "-" : journeyId, consentId, customerId, head.customerId());
      throw new ConsentRefusal("CONSENT_SUBJECT_MISMATCH", "consent subject does not match");
    }
    return consentView(head);
  }

  public List<ChainView> list(String customerId, String journeyId) {
    boolean byCustomer = customerId != null && !customerId.isBlank();
    boolean byJourney = journeyId != null && !journeyId.isBlank();
    if (byCustomer == byJourney) {
      throw new ConsentRefusal(
          "VALIDATION_ERROR", "exactly one of customer or journey is required");
    }
    List<String> ids =
        byCustomer
            ? consents.chainIdsForCustomer(customerId)
            : consents.chainIdsForJourney(journeyId);
    if (byCustomer) {
      requireUlid("customerId", customerId);
    } else {
      requireUlid("journeyId", journeyId);
    }
    List<ChainView> chains = new ArrayList<>();
    for (String id : ids) {
      chains.add(new ChainView(id, consents.chain(id).stream().map(this::consentView).toList()));
    }
    return chains;
  }

  @Transactional
  public ChallengeView startWithdrawal(
      String consentId, String channel, String reasonCode, String actorId, String correlationId) {
    requireUlid("consentId", consentId);
    if (!CHANNELS.contains(channel)) {
      throw new ConsentRefusal("VALIDATION_ERROR", "channel is not supported");
    }
    ConsentRow head =
        consents
            .headOf(consentId)
            .orElseThrow(() -> new ConsentRefusal("RESOURCE_NOT_FOUND", "consent was not found"));
    if (!"GRANTED".equals(head.state())) {
      throw new ConsentRefusal("ILLEGAL_TRANSITION", "only a granted consent can be withdrawn");
    }
    PolicySnapshot policy = policies.current();
    Instant now = clock.instant();
    int used =
        issues.countSince(head.customerId(), head.purpose(), now.minus(Duration.ofHours(24)));
    if (used >= policy.maxIssuesPer24h()) {
      throw new ConsentRefusal("OTP_ISSUE_RATE_EXCEEDED", "code issue limit reached");
    }
    String digits = contacts.digits(head.customerId());
    String otp = hasher.mintCode();
    String challengeId = Ulids.mint(clock);
    issues.record(head.customerId(), head.purpose(), now);
    StatementDoc doc =
        new StatementDoc(
            head.purpose(),
            head.statementId(),
            head.statementVersion(),
            head.statementText(),
            head.statementTextHash(),
            "APPROVED",
            head.language());
    ChallengeRow row =
        new ChallengeRow(
            challengeId,
            "WITHDRAWAL",
            "OTP_PENDING",
            0,
            1,
            now.plus(Duration.ofMinutes(policy.otpValidityMinutes())),
            Ulids.mint(clock),
            hasher.hash(otp),
            List.of(head.purpose()),
            refs(List.of(doc)),
            cipher.encrypt(digits),
            cipher.keyId(),
            head.customerId(),
            head.journeyId(),
            head.leadId(),
            head.lob(),
            head.checkpoint(),
            head.insurerCode(),
            channel,
            head.language(),
            consentId,
            actorId,
            policy.versionRef(),
            now);
    row = withReason(row, reasonCode);
    challenges.insert(row);
    dispatch.send(challengeId, row.contactUsedEnc(), otp);
    return challengeView(row, policy.maxAttempts(), List.of());
  }

  @Transactional
  public int sweep() {
    Instant now = clock.instant();
    int written = 0;
    for (ConsentRow head : consents.grantedPastValidity(now)) {
      ConsentRow expired =
          copyState(head, head.recordVersion() + 1, "EXPIRED", head.consentId(), now);
      consents.insert(expired);
      emit(
          "ConsentExpired",
          expired.consentId(),
          expired,
          null,
          "consent-sweep",
          "consent-sweep",
          policies.current());
      written++;
    }
    Instant abandonBefore = now.minus(Duration.ofHours(24));
    for (ChallengeRow stale : challenges.stale(abandonBefore)) {
      challenges.update(stale.withVerification("ABANDONED", stale.attempts()));
      written++;
    }
    return written;
  }

  public void onRuleSetPublished() {
    policies.invalidate();
  }

  private String grantPurpose(
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
            ACK,
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

  private String withdraw(
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
            ACK,
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

  private void audit(
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

  private static String reasonFor(
      ConsentRow head,
      StatementDoc statement,
      PolicySnapshot policy,
      PolicySnapshot.CheckpointRule rule,
      String insurerCode,
      Instant now) {
    if (!policy.requiredStatementStatus().equals(statement.status())) {
      return "STATEMENT_NOT_APPROVED";
    }
    if (head == null) {
      return "ABSENT";
    }
    if ("WITHDRAWN".equals(head.state())) {
      return "WITHDRAWN";
    }
    if ("SUPERSEDED".equals(head.state())) {
      return "SUPERSEDED";
    }
    if ("EXPIRED".equals(head.state())
        || ("GRANTED".equals(head.state()) && expired(head, rule, now))) {
      return "EXPIRED";
    }
    if (rule.namedInsurer() && (insurerCode == null || !insurerCode.equals(head.insurerCode()))) {
      return "SCOPE_MISMATCH";
    }
    if (statement.major() != major(head.statementVersion())) {
      return "STATEMENT_MAJOR_CHANGE";
    }
    if (!"GRANTED".equals(head.state())) {
      return "ABSENT";
    }
    return null;
  }

  private static boolean expired(ConsentRow head, PolicySnapshot.CheckpointRule rule, Instant now) {
    if (rule.validityDays() == null || head.validFrom() == null) {
      return false;
    }
    long days =
        ChronoUnit.DAYS.between(
            LocalDate.ofInstant(head.validFrom(), ZoneOffset.UTC),
            LocalDate.ofInstant(now, ZoneOffset.UTC));
    return days > rule.validityDays();
  }

  private static int major(String version) {
    int dot = version.indexOf('.');
    return Integer.parseInt(dot < 0 ? version : version.substring(0, dot));
  }

  private void validateCapture(CaptureCommand command) {
    requireUlid("customerId", command.customerId());
    requireUlid("leadId", command.leadId());
    if (command.journeyId() != null) {
      requireUlid("journeyId", command.journeyId());
    }
    if (!LOBS.contains(command.lob())) {
      throw new ConsentRefusal("VALIDATION_ERROR", "lob is not supported");
    }
    if (!CHANNELS.contains(command.channel())) {
      throw new ConsentRefusal("VALIDATION_ERROR", "channel is not supported");
    }
    if (command.purposes() == null || command.purposes().isEmpty()) {
      throw new ConsentRefusal("VALIDATION_ERROR", "at least one purpose is required");
    }
    if (command.purposes().stream().distinct().count() != command.purposes().size()) {
      throw new ConsentRefusal("VALIDATION_ERROR", "purposes must be unique");
    }
    for (String purpose : command.purposes()) {
      if (!PURPOSES.contains(purpose)) {
        throw new ConsentRefusal("VALIDATION_ERROR", "purpose is unknown");
      }
    }
  }

  private void requireAffirmations(ChallengeRow row, List<Affirmation> affirmations) {
    if (affirmations == null || affirmations.size() != row.purposes().size()) {
      throw new ConsentRefusal("VALIDATION_ERROR", "each purpose needs its own affirmation");
    }
    JsonNode refs = readRefs(row.statementRefs());
    for (String purpose : row.purposes()) {
      Affirmation match =
          affirmations.stream()
              .filter(item -> purpose.equals(item.purpose()))
              .findFirst()
              .orElse(null);
      JsonNode ref = refFor(refs, purpose);
      if (match == null
          || !ref.path("statementId").asText().equals(match.statementId())
          || !ref.path("statementVersion").asText().equals(match.statementVersion())
          || match.affirmedAt() == null) {
        throw new ConsentRefusal("VALIDATION_ERROR", "affirmation does not match the statement");
      }
    }
  }

  private String refs(List<StatementDoc> docs) {
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

  private JsonNode readRefs(String json) {
    try {
      return Values.mapper().readTree(json);
    } catch (Exception ex) {
      throw new ConsentRefusal("INTERNAL_ERROR", "statement reference could not be read");
    }
  }

  private static JsonNode refFor(JsonNode refs, String purpose) {
    for (JsonNode node : refs) {
      if (purpose.equals(node.path("purpose").asText())) {
        return node;
      }
    }
    throw new ConsentRefusal("VALIDATION_ERROR", "statement reference is missing");
  }

  private ChallengeView challengeView(ChallengeRow row, int maxAttempts, List<String> consentIds) {
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

  private ConsentView consentView(ConsentRow row) {
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

  private static ConsentRow copyState(
      ConsentRow head, int version, String state, String supersedes, Instant now) {
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

  private static ChallengeRow withReason(ChallengeRow row, String reasonCode) {
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

  private static void requireUlid(String field, String value) {
    if (!Ulids.isValid(value)) {
      throw new ConsentRefusal("VALIDATION_ERROR", field + " must be a ULID");
    }
  }

  private static boolean blank(String value) {
    return value == null || value.isBlank();
  }

  private static String statusKey(String purpose) {
    return switch (purpose) {
      case "CNS-DP" -> "dpStatementStatus";
      case "CNS-SOL" -> "solStatementStatus";
      case "CNS-QSH" -> "qshStatementStatus";
      case "CNS-SHR" -> "shrStatementStatus";
      case "CNS-COM" -> "comStatementStatus";
      default -> throw new ConsentRefusal("VALIDATION_ERROR", "purpose is unknown");
    };
  }

  private static String grantedKey(String purpose) {
    return switch (purpose) {
      case "CNS-DP" -> "dpGrantedOn";
      case "CNS-SOL" -> "solGrantedOn";
      case "CNS-QSH" -> "qshGrantedOn";
      case "CNS-SHR" -> "shrGrantedOn";
      case "CNS-COM" -> "comGrantedOn";
      default -> throw new ConsentRefusal("VALIDATION_ERROR", "purpose is unknown");
    };
  }

  public record CaptureCommand(
      String customerId,
      String leadId,
      String journeyId,
      String lob,
      String checkpoint,
      List<String> purposes,
      String insurerCode,
      String channel,
      String language) {}

  public record Affirmation(
      String purpose, String statementId, String statementVersion, Instant affirmedAt) {}

  public record VerifyCommand(
      String challengeId,
      String otpTxnId,
      String otp,
      String verificationMode,
      String sourceIp,
      List<Affirmation> affirmations) {}

  public record CheckView(
      String customerId,
      String checkpoint,
      boolean satisfied,
      boolean captureBlocked,
      String policyRuleSetId,
      String policyVersion,
      List<PurposeResult> valid,
      List<PurposeResult> missing) {}

  public record PurposeResult(
      String consentId,
      String purpose,
      String statementId,
      String statementVersion,
      String insurerCode,
      Instant validUntil,
      String reason,
      boolean missing) {

    static PurposeResult valid(ConsentRow row) {
      return new PurposeResult(
          row.consentId(),
          row.purpose(),
          row.statementId(),
          row.statementVersion(),
          row.insurerCode(),
          row.validUntil(),
          null,
          false);
    }

    static PurposeResult missing(
        String purpose, String reason, StatementDoc statement, ConsentRow head) {
      return new PurposeResult(
          null, purpose, statement.statementId(), statement.version(), null, null, reason, true);
    }
  }

  public record ChallengeView(
      String challengeId,
      String kind,
      String state,
      String customerId,
      List<String> purposes,
      Instant otpExpiresAt,
      String maskedContact,
      int attemptsRemaining,
      List<String> consentIds) {}

  public record ConsentView(
      String consentId,
      int recordVersion,
      String customerId,
      String leadId,
      String journeyId,
      String lob,
      String purpose,
      String insurerCode,
      String state,
      String statementId,
      String statementVersion,
      String statementTextHash,
      String language,
      String channel,
      String acknowledgementMethod,
      String checkpoint,
      Instant capturedAt,
      Instant validFrom,
      Instant validUntil,
      String supersedesConsentId,
      Instant withdrawnAt,
      String configVersionRef) {}

  public record ChainView(String consentId, List<ConsentView> rows) {}
}
