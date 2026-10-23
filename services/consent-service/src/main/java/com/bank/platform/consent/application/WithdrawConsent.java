package com.bank.platform.consent.application;

import com.bank.platform.consent.application.ConsentTypes.ChallengeView;
import com.bank.platform.consent.domain.ChallengeRow;
import com.bank.platform.consent.domain.ConsentPorts;
import com.bank.platform.consent.domain.ConsentRefusal;
import com.bank.platform.consent.domain.ConsentRow;
import com.bank.platform.consent.domain.FieldCipher;
import com.bank.platform.consent.domain.OtpDispatch;
import com.bank.platform.consent.domain.OtpHasher;
import com.bank.platform.consent.domain.PolicySnapshot;
import com.bank.platform.consent.domain.StatementDoc;
import com.bank.platform.consent.domain.Ulids;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Opens the OTP challenge that a withdrawal must pass before a WITHDRAWN row is appended. */
@Service
public class WithdrawConsent {

  private final ConsentPorts.PolicySource policies;
  private final ConsentPorts.ConsentStore consents;
  private final ConsentPorts.ChallengeStore challenges;
  private final ConsentPorts.IssueLedger issues;
  private final ConsentPorts.RegisteredContact contacts;
  private final ConsentEvidence evidence;
  private final FieldCipher cipher;
  private final OtpHasher hasher;
  private final OtpDispatch dispatch;
  private final Clock clock;

  public WithdrawConsent(
      ConsentPorts.PolicySource policies,
      ConsentPorts.ConsentStore consents,
      ConsentPorts.ChallengeStore challenges,
      ConsentPorts.IssueLedger issues,
      ConsentPorts.RegisteredContact contacts,
      ConsentEvidence evidence,
      FieldCipher cipher,
      OtpHasher hasher,
      OtpDispatch dispatch,
      Clock clock) {
    this.policies = policies;
    this.consents = consents;
    this.challenges = challenges;
    this.issues = issues;
    this.contacts = contacts;
    this.evidence = evidence;
    this.cipher = cipher;
    this.hasher = hasher;
    this.dispatch = dispatch;
    this.clock = clock;
  }

  @Transactional
  public ChallengeView start(
      String consentId, String channel, String reasonCode, String actorId, String correlationId) {
    ConsentInput.requireUlid("consentId", consentId);
    if (!ConsentInput.CHANNELS.contains(channel)) {
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
            evidence.refs(List.of(doc)),
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
    row = ConsentEvidence.withReason(row, reasonCode);
    challenges.insert(row);
    dispatch.send(challengeId, row.contactUsedEnc(), otp);
    return evidence.challengeView(row, policy.maxAttempts(), List.of());
  }
}
