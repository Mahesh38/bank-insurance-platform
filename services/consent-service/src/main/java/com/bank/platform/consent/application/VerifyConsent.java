package com.bank.platform.consent.application;

import com.bank.platform.consent.application.ConsentTypes.Affirmation;
import com.bank.platform.consent.application.ConsentTypes.ChallengeView;
import com.bank.platform.consent.application.ConsentTypes.VerifyCommand;
import com.bank.platform.consent.domain.ChallengeRow;
import com.bank.platform.consent.domain.ConsentPorts;
import com.bank.platform.consent.domain.ConsentRefusal;
import com.bank.platform.consent.domain.OtpDispatch;
import com.bank.platform.consent.domain.OtpHasher;
import com.bank.platform.consent.domain.PolicySnapshot;
import com.bank.platform.consent.domain.Ulids;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Customer-device OTP check, resend, and the evidence write that follows a match. */
@Service
public class VerifyConsent {

  private final VerifyConsent self;
  private final ConsentPorts.PolicySource policies;
  private final ConsentPorts.ChallengeStore challenges;
  private final ConsentPorts.IssueLedger issues;
  private final ConsentEvidence evidence;
  private final OtpAttemptRecorder attempts;
  private final OtpHasher hasher;
  private final OtpDispatch dispatch;
  private final Clock clock;

  public VerifyConsent(
      @Lazy VerifyConsent self,
      ConsentPorts.PolicySource policies,
      ConsentPorts.ChallengeStore challenges,
      ConsentPorts.IssueLedger issues,
      ConsentEvidence evidence,
      OtpAttemptRecorder attempts,
      OtpHasher hasher,
      OtpDispatch dispatch,
      Clock clock) {
    this.self = self;
    this.policies = policies;
    this.challenges = challenges;
    this.issues = issues;
    this.evidence = evidence;
    this.attempts = attempts;
    this.hasher = hasher;
    this.dispatch = dispatch;
    this.clock = clock;
  }

  public ChallengeView verify(VerifyCommand command, String actorId, String correlationId) {
    ConsentInput.requireUlid("challengeId", command.challengeId());
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
    if (ConsentInput.blank(command.sourceIp())) {
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

  /** Public so the transactional proxy is the caller. A direct call would skip the transaction. */
  @Transactional
  public ChallengeView completeVerify(VerifyCommand command, String actorId, String correlationId) {
    ChallengeRow row = challenges.find(command.challengeId()).orElseThrow();
    PolicySnapshot policy = policies.current();
    Instant now = clock.instant();
    List<String> consentIds = new ArrayList<>();
    if ("CAPTURE".equals(row.kind())) {
      JsonNode refs = evidence.readRefs(row.statementRefs());
      for (String purpose : row.purposes()) {
        JsonNode ref = ConsentEvidence.refFor(refs, purpose);
        consentIds.add(evidence.grantPurpose(row, purpose, ref, command.sourceIp(), now, policy));
      }
    } else {
      consentIds.add(evidence.withdraw(row, command, actorId, now, policy, correlationId));
    }
    ChallengeRow verified = row.withVerification("VERIFIED", row.attempts());
    challenges.update(verified);
    evidence.audit(
        row,
        "VERIFY",
        "CHALLENGE",
        row.challengeId(),
        "SUCCESS",
        "VERIFIED",
        actorId,
        correlationId,
        policy);
    return evidence.challengeView(verified, policy.maxAttempts(), consentIds);
  }

  @Transactional
  public ChallengeView resend(String challengeId, String actorId, String correlationId) {
    ConsentInput.requireUlid("challengeId", challengeId);
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
    return evidence.challengeView(fresh, policy.maxAttempts(), List.of());
  }

  public ChallengeView getChallenge(String challengeId) {
    ConsentInput.requireUlid("challengeId", challengeId);
    ChallengeRow row =
        challenges
            .find(challengeId)
            .orElseThrow(() -> new ConsentRefusal("RESOURCE_NOT_FOUND", "challenge was not found"));
    PolicySnapshot policy = policies.current();
    return evidence.challengeView(row, policy.maxAttempts(), List.of());
  }

  private void requireAffirmations(ChallengeRow row, List<Affirmation> affirmations) {
    if (affirmations == null || affirmations.size() != row.purposes().size()) {
      throw new ConsentRefusal("VALIDATION_ERROR", "each purpose needs its own affirmation");
    }
    JsonNode refs = evidence.readRefs(row.statementRefs());
    for (String purpose : row.purposes()) {
      Affirmation match =
          affirmations.stream()
              .filter(item -> purpose.equals(item.purpose()))
              .findFirst()
              .orElse(null);
      JsonNode ref = ConsentEvidence.refFor(refs, purpose);
      if (match == null
          || !ref.path("statementId").asText().equals(match.statementId())
          || !ref.path("statementVersion").asText().equals(match.statementVersion())
          || match.affirmedAt() == null) {
        throw new ConsentRefusal("VALIDATION_ERROR", "affirmation does not match the statement");
      }
    }
  }
}
