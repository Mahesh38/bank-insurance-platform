package com.bank.platform.consent.application;

import com.bank.platform.consent.domain.ActorRole;
import com.bank.platform.consent.domain.ChallengePurpose;
import com.bank.platform.consent.domain.ChallengeStatus;
import com.bank.platform.consent.domain.ConsentChains;
import com.bank.platform.consent.domain.ConsentChannel;
import com.bank.platform.consent.domain.ConsentCode;
import com.bank.platform.consent.domain.ConsentCodes;
import com.bank.platform.consent.domain.ConsentEvaluation;
import com.bank.platform.consent.domain.ConsentEvidence;
import com.bank.platform.consent.domain.ConsentEvidenceStore;
import com.bank.platform.consent.domain.ConsentPolicyRegistry;
import com.bank.platform.consent.domain.ConsentRefusal;
import com.bank.platform.consent.domain.ConsentState;
import com.bank.platform.consent.domain.ConsentTypePolicy;
import com.bank.platform.consent.domain.EvaluationDecision;
import com.bank.platform.consent.domain.EvaluationRequest;
import com.bank.platform.consent.domain.FieldCipher;
import com.bank.platform.consent.domain.JourneyEffect;
import com.bank.platform.consent.domain.OtpChallenge;
import com.bank.platform.consent.domain.OtpChallengeStore;
import com.bank.platform.consent.domain.OtpDispatch;
import com.bank.platform.consent.domain.OtpHasher;
import com.bank.platform.consent.domain.OtpRules;
import com.bank.platform.consent.domain.PendingCapture;
import com.bank.platform.consent.domain.PublishedStatement;
import com.bank.platform.consent.domain.RetentionStore;
import com.bank.platform.consent.domain.SecurityEventSink;
import com.bank.platform.consent.domain.SensitiveText;
import com.bank.platform.consent.domain.StatementRules;
import com.bank.platform.consent.domain.StatementStore;
import com.bank.platform.consent.domain.Ulids;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.Period;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Use cases for capture, verification, withdrawal, evaluation and retention. */
@Service
public class ConsentApplicationService {

  static final String ACK_OTP = "OTP_SMS_CUSTOMER_DEVICE";
  static final String OPS_POST_SUBMISSION = "CONSENT_WITHDRAWN_POST_SUBMISSION";
  private static final Pattern RAW_MOBILE = Pattern.compile("^[6-9]\\d{9}$");
  private static final Period RETENTION = Period.ofYears(7);

  private final ConsentEvidenceStore evidence;
  private final StatementStore statements;
  private final OtpChallengeStore challenges;
  private final OtpAttemptRecorder attempts;
  private final RetentionStore retention;
  private final ConsentPolicyRegistry policies;
  private final ConsentEvaluation evaluation;
  private final FieldCipher cipher;
  private final OtpHasher hasher;
  private final OtpDispatch dispatch;
  private final SecurityEventSink securityEvents;
  private final Clock clock;
  private final String distributorId;

  public ConsentApplicationService(
      ConsentEvidenceStore evidence,
      StatementStore statements,
      OtpChallengeStore challenges,
      OtpAttemptRecorder attempts,
      RetentionStore retention,
      ConsentPolicyRegistry policies,
      ConsentEvaluation evaluation,
      FieldCipher cipher,
      OtpHasher hasher,
      OtpDispatch dispatch,
      SecurityEventSink securityEvents,
      Clock clock,
      @Value("${consent.distributor-id}") String distributorId) {
    this.evidence = evidence;
    this.statements = statements;
    this.challenges = challenges;
    this.attempts = attempts;
    this.retention = retention;
    this.policies = policies;
    this.evaluation = evaluation;
    this.cipher = cipher;
    this.hasher = hasher;
    this.dispatch = dispatch;
    this.securityEvents = securityEvents;
    this.clock = clock;
    this.distributorId = distributorId;
  }

  @Transactional
  public PublishedStatement publish(
      String statementId,
      String version,
      String text,
      String language,
      String changeClass,
      String classifiedBy) {
    boolean englishExists = statements.englishSourceExists(statementId, version);
    StatementRules.requireLanguage(
        language, englishExists || StatementRules.R0_LANGUAGE.equals(language));
    if (!StatementRules.R0_LANGUAGE.equals(language)) {
      StatementRules.requireLanguage(language, englishExists);
    }
    if (statements.find(statementId, version, language).isPresent()) {
      throw new ConsentRefusal(
          ConsentCodes.STATEMENT_VERSION_IMMUTABLE, "statement version is immutable");
    }
    PublishedStatement statement =
        new PublishedStatement(
            statementId,
            version,
            text,
            StatementRules.sha256(text),
            language,
            com.bank.platform.consent.domain.ChangeClass.parse(changeClass),
            classifiedBy,
            clock.instant());
    statements.insert(statement);
    return statement;
  }

  @Transactional
  public GrantOutcome openGrant(OpenGrantCommand command) {
    ConsentCode code = ConsentCode.parse(command.consentCode());
    ConsentChannel channel = requireAssisted(command.channel());
    requireLob(command.lob());
    requireText(command.customerId(), "customer id is required");
    requireText(command.customerCif(), "customer cif is required");
    requireText(command.leadId(), "lead and journey are required");
    requireText(command.journeyId(), "lead and journey are required");
    requireText(command.capturedByActorId(), "actor is required");
    requireText(command.configVersionRef(), "config version is required");
    rejectRawMobile(command.registeredContactRef());
    ConsentTypePolicy policy = policies.require(code);
    if (policy.requiresNamedCounterparty() && blankToNull(command.counterpartyId()) == null) {
      throw new ConsentRefusal(
          ConsentCodes.INVALID_REQUEST, "named insurer is required for this consent");
    }
    String language = languageOf(command.language());
    PublishedStatement statement =
        requireStatement(command.statementId(), command.statementVersion(), language);

    ConsentEvidence existing = currentHead(command.customerId(), code, command.counterpartyId());
    if (existing != null) {
      EvaluationDecision decision =
          evaluation.evaluate(
              new EvaluationRequest(
                  command.customerId(),
                  code,
                  command.counterpartyId(),
                  statement.version(),
                  false,
                  false),
              existing,
              clock.instant());
      if (decision.allowed()) {
        return new GrantOutcome(false, null, existing, decision.effect());
      }
    }

    OtpRules.checkIssueAllowed(issuesInLastDay(command.customerId(), code));
    String root = existing == null ? Ulids.mint(clock) : existing.rootConsentId();
    PendingCapture pending =
        pendingFrom(
            command,
            statement,
            channel,
            cipher.encrypt(command.customerCif()),
            cipher.encrypt(command.registeredContactRef()));
    OtpChallenge challenge =
        issue(ChallengePurpose.GRANT, root, command.customerId(), code, pending);
    return new GrantOutcome(true, challenge, null, JourneyEffect.BLOCK_STEP);
  }

  @Transactional
  public ConsentEvidence verify(String challengeId, String otp, String actorRole) {
    if (ActorRole.parse(actorRole) != ActorRole.CUSTOMER_DEVICE) {
      throw new ConsentRefusal(
          ConsentCodes.CONSENT_DEVICE_ISOLATION, "otp must be confirmed on the customer device");
    }
    if (otp == null || otp.isBlank()) {
      throw new ConsentRefusal(ConsentCodes.CONSENT_OTP_REQUIRED, "otp is required");
    }
    OtpChallenge challenge =
        challenges
            .find(challengeId)
            .orElseThrow(
                () ->
                    new ConsentRefusal(
                        ConsentCodes.RESOURCE_NOT_FOUND, "otp transaction was not found"));
    try {
      OtpRules.checkStillOpen(challenge, clock.instant());
    } catch (ConsentRefusal refusal) {
      if (ConsentCodes.OTP_EXPIRED.equals(refusal.code())
          && challenge.status() == ChallengeStatus.OPEN) {
        attempts.commit(challenge.expired());
      }
      throw refusal;
    }
    if (!hasher.matches(otp, challenge.otpHash())) {
      OtpChallenge failed = challenge.failedAttempt();
      attempts.commit(failed);
      if (failed.status() == ChallengeStatus.LOCKED) {
        throw new ConsentRefusal(ConsentCodes.OTP_ATTEMPTS_EXCEEDED, "otp verify cap reached");
      }
      throw new ConsentRefusal(ConsentCodes.CONSENT_OTP_NOT_VERIFIED, "otp did not match");
    }
    challenges.save(challenge.verified());
    return appendVerified(challenge.verified());
  }

  @Transactional
  public GrantOutcome openWithdrawal(
      String rootConsentId,
      String registeredContactRef,
      String reasonCode,
      boolean proposalSubmitted,
      String actorId,
      String sourceIp) {
    rejectRawMobile(registeredContactRef);
    requireText(reasonCode, "withdrawal reason is required");
    requireText(actorId, "actor is required");
    ConsentEvidence head = requireGrantedHead(rootConsentId);
    OtpRules.checkIssueAllowed(issuesInLastDay(head.customerId(), head.code()));
    PendingCapture pending =
        new PendingCapture(
            head.cifEnc(),
            head.encryptionKeyId(),
            cipher.encrypt(registeredContactRef),
            head.leadId(),
            head.journeyId(),
            head.statementId(),
            head.statementVersion(),
            head.statementText(),
            head.statementTextHash(),
            head.language(),
            head.channel(),
            actorId,
            head.capturedByAgentId(),
            head.distributorId(),
            head.counterpartyId(),
            head.lob(),
            head.configVersionRef(),
            reasonCode,
            proposalSubmitted,
            sourceIp);
    OtpChallenge challenge =
        issue(
            ChallengePurpose.WITHDRAW,
            head.rootConsentId(),
            head.customerId(),
            head.code(),
            pending);
    return new GrantOutcome(
        true, challenge, head, policies.require(head.code()).whenWithdrawn(proposalSubmitted));
  }

  @Transactional(readOnly = true)
  public EvaluationDecision evaluate(EvaluationCommand command) {
    requireText(command.customerId(), "customer id is required");
    requireText(command.presentedCif(), "customer cif is required");
    ConsentCode code = ConsentCode.parse(command.consentCode());
    ConsentEvidence presented = presentedRow(command.presentedConsentId());
    if (presented != null && !subjectMatches(presented, command)) {
      return subjectMismatch(command, code, presented);
    }
    if (presented != null
        && presented.code() == code
        && policies.require(code).requiresNamedCounterparty()
        && !Objects.equals(
            blankToNull(presented.counterpartyId()), blankToNull(command.counterpartyId()))) {
      return EvaluationDecision.refuse(
          code,
          ConsentCodes.CONSENT_SCOPE_MISMATCH,
          true,
          JourneyEffect.BLOCK_STEP,
          presented.rootConsentId());
    }
    ConsentEvidence head = currentHead(command.customerId(), code, command.counterpartyId());
    if (head != null && !cifMatches(head, command.presentedCif())) {
      return subjectMismatch(command, code, head);
    }
    EvaluationDecision decision =
        evaluation.evaluate(
            new EvaluationRequest(
                command.customerId(),
                code,
                command.counterpartyId(),
                command.currentStatementVersion(),
                command.pastBlockingPoint(),
                command.proposalAlreadySubmitted()),
            head,
            clock.instant());
    if (ConsentCodes.CONSENT_SUBJECT_MISMATCH.equals(decision.errorCode())) {
      securityEvents.subjectMismatch(
          command.actorId(),
          firstText(command.journeyId(), head == null ? null : head.journeyId()),
          decision.consentId());
    }
    return decision;
  }

  @Transactional(readOnly = true)
  public List<ChainRow> chain(String rootConsentId) {
    List<ConsentEvidence> rows = ConsentChains.ordered(evidence.chain(rootConsentId));
    if (rows.isEmpty()) {
      throw new ConsentRefusal(ConsentCodes.RESOURCE_NOT_FOUND, "consent was not found");
    }
    ConsentEvidence head = ConsentChains.head(rows);
    ConsentTypePolicy policy = policies.require(head.code());
    return rows.stream().map(row -> toRow(row, head, policy)).toList();
  }

  @Transactional(readOnly = true)
  public List<ConsentEvidence> byJourney(String journeyId) {
    return evidence.byJourney(journeyId);
  }

  @Transactional(readOnly = true)
  public List<ConsentEvidence> byCustomer(String customerId) {
    return evidence.byCustomer(customerId);
  }

  @Transactional
  public RetentionAnchor anchorRetention(String rootConsentId, Instant terminalAt) {
    if (evidence.chain(rootConsentId).isEmpty()) {
      throw new ConsentRefusal(ConsentCodes.RESOURCE_NOT_FOUND, "consent was not found");
    }
    if (terminalAt == null) {
      throw new ConsentRefusal(ConsentCodes.INVALID_REQUEST, "terminal instant is required");
    }
    Instant retainUntil =
        ZonedDateTime.ofInstant(terminalAt, ZoneOffset.UTC).plus(RETENTION).toInstant();
    retention.anchor(rootConsentId, terminalAt, retainUntil);
    return new RetentionAnchor(rootConsentId, terminalAt, retainUntil);
  }

  public String decryptCif(ConsentEvidence row) {
    return cipher.decrypt(row.cifEnc());
  }

  private ChainRow toRow(ConsentEvidence row, ConsentEvidence head, ConsentTypePolicy policy) {
    ConsentState effective;
    if (!row.evidenceId().equals(head.evidenceId())) {
      effective = ConsentState.SUPERSEDED;
    } else {
      effective = evaluation.effectiveState(row, policy, clock.instant());
    }
    return new ChainRow(row, effective);
  }

  private ConsentEvidence appendVerified(OtpChallenge challenge) {
    try {
      List<ConsentEvidence> chain = evidence.chain(challenge.rootConsentId());
      ConsentEvidence previous = ConsentChains.head(chain);
      int version = previous == null ? 1 : previous.recordVersion() + 1;
      Instant now = clock.instant();
      PendingCapture pending = challenge.pending();
      ConsentTypePolicy policy = policies.require(challenge.code());
      boolean withdrawal = challenge.purpose() == ChallengePurpose.WITHDRAW;
      String ops = null;
      if (withdrawal && pending.proposalSubmitted() && challenge.code() == ConsentCode.CNS_SHR) {
        ops = OPS_POST_SUBMISSION;
      }
      Instant validFrom = withdrawal && previous != null ? previous.validFrom() : now;
      Instant validUntil =
          withdrawal && previous != null ? previous.validUntil() : policy.validUntil(now);
      ConsentEvidence row =
          new ConsentEvidence(
              Ulids.mint(clock),
              challenge.rootConsentId(),
              version,
              previous == null ? null : previous.evidenceId(),
              challenge.customerId(),
              challenge.code(),
              withdrawal ? ConsentState.WITHDRAWN : ConsentState.GRANTED,
              validFrom,
              validUntil,
              pending.counterpartyId(),
              pending.statementId(),
              pending.statementVersion(),
              pending.statementText(),
              pending.statementTextHash(),
              pending.language(),
              pending.cifEnc(),
              pending.encryptionKeyId(),
              pending.contactRefEnc(),
              pending.leadId(),
              pending.journeyId(),
              pending.channel(),
              challenge.challengeId(),
              now,
              now,
              pending.sourceIp(),
              pending.capturedByActorId(),
              pending.capturedByAgentId(),
              pending.distributorId(),
              pending.lob(),
              pending.configVersionRef(),
              withdrawal ? pending.withdrawalReasonCode() : null,
              ops,
              ACK_OTP);
      if (!StatementRules.sha256(row.statementText()).equals(row.statementTextHash())) {
        throw new ConsentRefusal(
            ConsentCodes.CONSENT_EVIDENCE_WRITE_FAILED, "evidence hash check failed");
      }
      evidence.append(row);
      return row;
    } catch (ConsentRefusal refusal) {
      throw refusal;
    } catch (RuntimeException ex) {
      throw new ConsentRefusal(ConsentCodes.CONSENT_EVIDENCE_WRITE_FAILED, "evidence write failed");
    }
  }

  private OtpChallenge issue(
      ChallengePurpose purpose,
      String root,
      String customerId,
      ConsentCode code,
      PendingCapture pending) {
    String otp = hasher.mintCode();
    OtpChallenge challenge =
        new OtpChallenge(
            Ulids.mint(clock),
            purpose,
            root,
            customerId,
            code,
            hasher.hash(otp),
            clock.instant().plus(OtpRules.VALIDITY),
            0,
            ChallengeStatus.OPEN,
            pending,
            clock.instant());
    challenges.save(challenge);
    try {
      dispatch.send(challenge.challengeId(), pending.contactRefEnc(), otp);
    } catch (RuntimeException ex) {
      throw new ConsentRefusal(ConsentCodes.CONSENT_EVIDENCE_WRITE_FAILED, "otp dispatch failed");
    }
    return challenge;
  }

  private PendingCapture pendingFrom(
      OpenGrantCommand command,
      PublishedStatement statement,
      ConsentChannel channel,
      byte[] cifEnc,
      byte[] contactEnc) {
    return new PendingCapture(
        cifEnc,
        cipher.keyId(),
        contactEnc,
        command.leadId(),
        command.journeyId(),
        statement.statementId(),
        statement.version(),
        statement.text(),
        statement.textHash(),
        statement.language(),
        channel,
        command.capturedByActorId(),
        command.capturedByAgentId(),
        distributorId,
        blankToNull(command.counterpartyId()),
        command.lob(),
        command.configVersionRef(),
        null,
        false,
        command.sourceIp());
  }

  private ConsentEvidence requireGrantedHead(String rootConsentId) {
    List<ConsentEvidence> rows = evidence.chain(rootConsentId);
    ConsentEvidence head = ConsentChains.head(rows);
    if (head == null) {
      throw new ConsentRefusal(ConsentCodes.RESOURCE_NOT_FOUND, "consent was not found");
    }
    ConsentTypePolicy policy = policies.require(head.code());
    ConsentState effective = evaluation.effectiveState(head, policy, clock.instant());
    if (effective != ConsentState.GRANTED) {
      throw new ConsentRefusal(
          ConsentCodes.ILLEGAL_TRANSITION, "only a granted consent can be withdrawn");
    }
    return head;
  }

  private ConsentEvidence currentHead(String customerId, ConsentCode code, String counterpartyId) {
    return evidence.head(customerId, code, blankToNull(counterpartyId)).orElse(null);
  }

  private ConsentEvidence presentedRow(String rootConsentId) {
    if (rootConsentId == null || rootConsentId.isBlank()) {
      return null;
    }
    return ConsentChains.head(evidence.chain(rootConsentId));
  }

  private boolean subjectMatches(ConsentEvidence row, EvaluationCommand command) {
    return row.customerId().equals(command.customerId()) && cifMatches(row, command.presentedCif());
  }

  private boolean cifMatches(ConsentEvidence row, String presented) {
    try {
      return sameSecret(cipher.decrypt(row.cifEnc()), presented);
    } catch (ConsentRefusal refusal) {
      throw refusal;
    } catch (RuntimeException ex) {
      throw new ConsentRefusal(
          ConsentCodes.CONSENT_EVIDENCE_WRITE_FAILED, "stored subject could not be read");
    }
  }

  private EvaluationDecision subjectMismatch(
      EvaluationCommand command, ConsentCode code, ConsentEvidence row) {
    securityEvents.subjectMismatch(
        command.actorId(), firstText(command.journeyId(), row.journeyId()), row.rootConsentId());
    return EvaluationDecision.refuse(
        code,
        ConsentCodes.CONSENT_SUBJECT_MISMATCH,
        true,
        JourneyEffect.HALT_JOURNEY,
        row.rootConsentId());
  }

  private int issuesInLastDay(String customerId, ConsentCode code) {
    return challenges.issuesSince(customerId, code, clock.instant().minus(Duration.ofHours(24)));
  }

  private PublishedStatement requireStatement(String statementId, String version, String language) {
    return statements
        .find(statementId, version, language)
        .orElseThrow(
            () ->
                new ConsentRefusal(
                    ConsentCodes.STATEMENT_NOT_PUBLISHED, "statement version is not published"));
  }

  private static String languageOf(String language) {
    if (language == null || language.isBlank()) {
      return StatementRules.R0_LANGUAGE;
    }
    return language;
  }

  private static ConsentChannel requireAssisted(String channel) {
    ConsentChannel parsed = ConsentChannel.parse(channel);
    if (parsed != ConsentChannel.ASSISTED) {
      throw new ConsentRefusal(
          ConsentCodes.INVALID_REQUEST, "only assisted capture is in the current release");
    }
    return parsed;
  }

  private static void requireLob(String lob) {
    if (lob == null || !(lob.equals("LIFE") || lob.equals("HEALTH") || lob.equals("GENERAL"))) {
      throw new ConsentRefusal(ConsentCodes.UNSUPPORTED_LOB, "lob is not supported");
    }
  }

  private static void rejectRawMobile(String ref) {
    if (ref == null || ref.isBlank()) {
      throw new ConsentRefusal(
          ConsentCodes.INVALID_REQUEST, "registered contact reference is required");
    }
    if (RAW_MOBILE.matcher(ref.trim()).matches()) {
      throw new ConsentRefusal(
          ConsentCodes.INVALID_REQUEST,
          "otp target must be the directory reference, not a typed mobile");
    }
  }

  private static void requireText(String value, String message) {
    if (value == null || value.isBlank()) {
      throw new ConsentRefusal(ConsentCodes.INVALID_REQUEST, message);
    }
  }

  private static String blankToNull(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value;
  }

  private static String firstText(String preferred, String fallback) {
    if (preferred != null && !preferred.isBlank()) {
      return preferred;
    }
    return fallback;
  }

  private static boolean sameSecret(String left, String right) {
    if (left == null || right == null) {
      return false;
    }
    return MessageDigest.isEqual(
        left.getBytes(StandardCharsets.UTF_8), right.getBytes(StandardCharsets.UTF_8));
  }

  /** Safe log line. Never includes the customer, the code, or the contact. */
  public static String safe(String value) {
    return SensitiveText.scrub(value);
  }

  public record GrantOutcome(
      boolean challengeIssued,
      OtpChallenge challenge,
      ConsentEvidence reused,
      JourneyEffect effect) {}

  public record ChainRow(ConsentEvidence evidence, ConsentState effectiveStatus) {}

  public record RetentionAnchor(String rootConsentId, Instant terminalAt, Instant retainUntil) {}
}
