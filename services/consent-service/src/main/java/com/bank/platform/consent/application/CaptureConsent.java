package com.bank.platform.consent.application;

import com.bank.platform.consent.application.ConsentTypes.CaptureCommand;
import com.bank.platform.consent.application.ConsentTypes.ChallengeView;
import com.bank.platform.consent.domain.ChallengeRow;
import com.bank.platform.consent.domain.ConsentPorts;
import com.bank.platform.consent.domain.ConsentRefusal;
import com.bank.platform.consent.domain.FieldCipher;
import com.bank.platform.consent.domain.OtpDispatch;
import com.bank.platform.consent.domain.OtpHasher;
import com.bank.platform.consent.domain.PolicySnapshot;
import com.bank.platform.consent.domain.StatementDoc;
import com.bank.platform.consent.domain.Ulids;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Opens one OTP challenge for the purposes named on a capture. */
@Service
public class CaptureConsent {

  private final ConsentPorts.PolicySource policies;
  private final ConsentPorts.StatementSource statements;
  private final ConsentPorts.RegisteredContact contacts;
  private final ConsentPorts.ChallengeStore challenges;
  private final ConsentPorts.IssueLedger issues;
  private final ConsentEvidence evidence;
  private final FieldCipher cipher;
  private final OtpHasher hasher;
  private final OtpDispatch dispatch;
  private final Clock clock;

  public CaptureConsent(
      ConsentPorts.PolicySource policies,
      ConsentPorts.StatementSource statements,
      ConsentPorts.RegisteredContact contacts,
      ConsentPorts.ChallengeStore challenges,
      ConsentPorts.IssueLedger issues,
      ConsentEvidence evidence,
      FieldCipher cipher,
      OtpHasher hasher,
      OtpDispatch dispatch,
      Clock clock) {
    this.policies = policies;
    this.statements = statements;
    this.contacts = contacts;
    this.challenges = challenges;
    this.issues = issues;
    this.evidence = evidence;
    this.cipher = cipher;
    this.hasher = hasher;
    this.dispatch = dispatch;
    this.clock = clock;
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
    if (command.purposes().contains("CNS-SHR") && ConsentInput.blank(command.insurerCode())) {
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
            evidence.refs(docs),
            contact,
            cipher.keyId(),
            command.customerId(),
            command.journeyId(),
            command.leadId(),
            command.lob(),
            command.checkpoint(),
            ConsentInput.blank(command.insurerCode()) ? null : command.insurerCode(),
            command.channel(),
            command.language(),
            null,
            actorId,
            policy.versionRef(),
            now);
    challenges.insert(row);
    dispatch.send(challengeId, contact, otp);
    evidence.audit(
        row,
        "CAPTURE",
        "CHALLENGE",
        challengeId,
        "SUCCESS",
        "OTP_PENDING",
        actorId,
        correlationId,
        policy);
    return evidence.challengeView(row, policy.maxAttempts(), List.of());
  }

  private static void validateCapture(CaptureCommand command) {
    ConsentInput.requireUlid("customerId", command.customerId());
    ConsentInput.requireUlid("leadId", command.leadId());
    if (command.journeyId() != null) {
      ConsentInput.requireUlid("journeyId", command.journeyId());
    }
    if (!ConsentInput.LOBS.contains(command.lob())) {
      throw new ConsentRefusal("VALIDATION_ERROR", "lob is not supported");
    }
    if (!ConsentInput.CHANNELS.contains(command.channel())) {
      throw new ConsentRefusal("VALIDATION_ERROR", "channel is not supported");
    }
    if (command.purposes() == null || command.purposes().isEmpty()) {
      throw new ConsentRefusal("VALIDATION_ERROR", "at least one purpose is required");
    }
    if (command.purposes().stream().distinct().count() != command.purposes().size()) {
      throw new ConsentRefusal("VALIDATION_ERROR", "purposes must be unique");
    }
    for (String purpose : command.purposes()) {
      if (!ConsentInput.PURPOSES.contains(purpose)) {
        throw new ConsentRefusal("VALIDATION_ERROR", "purpose is unknown");
      }
    }
  }
}
