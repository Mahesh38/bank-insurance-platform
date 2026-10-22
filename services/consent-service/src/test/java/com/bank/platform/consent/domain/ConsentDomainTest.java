package com.bank.platform.consent.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class ConsentDomainTest {

  private static final Instant T0 = Instant.parse("2026-06-01T00:00:00Z");
  private final ConsentPolicyRegistry policies = ConsentPolicyRegistry.r0();
  private final ConsentEvaluation evaluation = new ConsentEvaluation(policies);

  @Test
  void policies_adaptValidityScopeAndWithdrawal() {
    assertThat(policies.require(ConsentCode.CNS_DP).validity()).contains(Duration.ofDays(90));
    assertThat(policies.require(ConsentCode.CNS_SOL).validity()).contains(Duration.ofDays(90));
    assertThat(policies.require(ConsentCode.CNS_SHR).validity()).contains(Duration.ofDays(30));
    assertThat(policies.require(ConsentCode.CNS_SHR).requiresNamedCounterparty()).isTrue();
    assertThat(policies.require(ConsentCode.CNS_COM).validity()).isEmpty();
    assertThat(policies.require(ConsentCode.CNS_COM).absenceBlocksJourney()).isFalse();
    assertThat(policies.require(ConsentCode.CNS_DP).whenWithdrawn(false))
        .isEqualTo(JourneyEffect.HALT_JOURNEY);
    assertThat(policies.require(ConsentCode.CNS_SHR).whenWithdrawn(true))
        .isEqualTo(JourneyEffect.HALT_FURTHER_SHARING);
    assertThat(policies.require(ConsentCode.CNS_SHR).whenWithdrawn(false))
        .isEqualTo(JourneyEffect.HALT_JOURNEY);
    assertThat(policies.require(ConsentCode.CNS_COM).whenWithdrawn(true))
        .isEqualTo(JourneyEffect.SUPPRESS_OPTIONAL_COMMUNICATIONS);
    assertThat(policies.require(ConsentCode.CNS_COM).whenAbsent())
        .isEqualTo(JourneyEffect.SUPPRESS_OPTIONAL_COMMUNICATIONS);
    assertThat(policies.require(ConsentCode.CNS_SHR).absenceBlocksJourney()).isTrue();
    assertThat(policies.require(ConsentCode.CNS_SHR).whenAbsent())
        .isEqualTo(JourneyEffect.BLOCK_STEP);
    assertThat(policies.require(ConsentCode.CNS_SOL).whenWithdrawn(false))
        .isEqualTo(JourneyEffect.HALT_JOURNEY);
    assertThat(policies.require(ConsentCode.CNS_COM).requiresNamedCounterparty()).isFalse();
    assertThat(policies.require(ConsentCode.CNS_DP).majorVersionForcesReconsent(true)).isFalse();
    assertThat(policies.require(ConsentCode.CNS_DP).majorVersionForcesReconsent(false)).isTrue();
  }

  @Test
  void registry_rejectsAMissingOrDuplicatePolicy() {
    assertThatThrownBy(() -> new ConsentPolicyRegistry(List.of()))
        .isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(
            () ->
                new ConsentPolicyRegistry(
                    List.of(new DataProcessingPolicy(), new DataProcessingPolicy())))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void codesAndChannels_refuseOutsideTheRelease() {
    assertThat(ConsentCode.parse("CNS-DP")).isEqualTo(ConsentCode.CNS_DP);
    assertThatThrownBy(() -> ConsentCode.parse("CNS-RDR"))
        .isInstanceOf(ConsentRefusal.class)
        .extracting(ex -> ((ConsentRefusal) ex).code())
        .isEqualTo(ConsentCodes.INVALID_REQUEST);
    assertThat(ConsentChannel.parse("DIY")).isEqualTo(ConsentChannel.DIY);
    assertThatThrownBy(() -> ConsentChannel.parse("BRANCH")).isInstanceOf(ConsentRefusal.class);
    assertThat(ConsentChannel.parse("ASSISTED")).isEqualTo(ConsentChannel.ASSISTED);
    assertThatThrownBy(() -> ActorRole.parse("CLERK")).isInstanceOf(ConsentRefusal.class);
    assertThat(ChangeClass.parse("MINOR")).isEqualTo(ChangeClass.MINOR);
  }

  @Test
  void statements_freezeTextAndCompareVersions() {
    assertThat(StatementRules.sha256("alpha")).hasSize(64);
    assertThat(StatementRules.isMajorUpgrade("1.0", "1.9")).isFalse();
    assertThat(StatementRules.isMajorUpgrade("1.4", "2.0")).isTrue();
    assertThatThrownBy(() -> StatementRules.major("v1"))
        .isInstanceOf(ConsentRefusal.class)
        .extracting(ex -> ((ConsentRefusal) ex).code())
        .isEqualTo(ConsentCodes.INVALID_REQUEST);
    StatementRules.requireLanguage("en-IN", false);
    StatementRules.requireLanguage("hi-IN", true);
    assertThatThrownBy(() -> StatementRules.requireLanguage("hi-IN", false))
        .extracting(ex -> ((ConsentRefusal) ex).code())
        .isEqualTo(ConsentCodes.STATEMENT_NOT_PUBLISHED);
    assertThatThrownBy(() -> StatementRules.requireLanguage("fr-FR", true))
        .extracting(ex -> ((ConsentRefusal) ex).code())
        .isEqualTo(ConsentCodes.INVALID_REQUEST);
    assertThatThrownBy(
            () ->
                new PublishedStatement(
                    "s", "1.0", "alpha", "deadbeef", "en-IN", ChangeClass.MINOR, "legal", T0))
        .extracting(ex -> ((ConsentRefusal) ex).code())
        .isEqualTo(ConsentCodes.CONFLICT);
    assertThatThrownBy(
            () ->
                new PublishedStatement(
                    "s", "1.0", "alpha", null, "en-IN", ChangeClass.MINOR, " ", T0))
        .extracting(ex -> ((ConsentRefusal) ex).code())
        .isEqualTo(ConsentCodes.INVALID_REQUEST);
  }

  @Test
  void otpRules_honourTheWindowTheAttemptCapAndTheIssueCap() {
    PendingCapture pending = pending();
    OtpChallenge open =
        new OtpChallenge(
            "c1",
            ChallengePurpose.GRANT,
            "r1",
            "cust",
            ConsentCode.CNS_DP,
            "hash",
            T0.plus(OtpRules.VALIDITY),
            0,
            ChallengeStatus.OPEN,
            pending,
            T0);
    OtpRules.checkStillOpen(open, T0.plus(OtpRules.VALIDITY));
    assertThatThrownBy(
            () -> OtpRules.checkStillOpen(open, T0.plus(OtpRules.VALIDITY).plusSeconds(1)))
        .extracting(ex -> ((ConsentRefusal) ex).code())
        .isEqualTo(ConsentCodes.OTP_EXPIRED);
    OtpChallenge locked = open.failedAttempt().failedAttempt().failedAttempt();
    assertThat(locked.status()).isEqualTo(ChallengeStatus.LOCKED);
    assertThat(locked.attempts()).isEqualTo(3);
    assertThatThrownBy(() -> OtpRules.checkStillOpen(locked, T0))
        .extracting(ex -> ((ConsentRefusal) ex).code())
        .isEqualTo(ConsentCodes.OTP_ATTEMPTS_EXCEEDED);
    assertThatThrownBy(() -> OtpRules.checkStillOpen(open.verified(), T0))
        .extracting(ex -> ((ConsentRefusal) ex).code())
        .isEqualTo(ConsentCodes.ILLEGAL_TRANSITION);
    OtpRules.checkIssueAllowed(4);
    assertThatThrownBy(() -> OtpRules.checkIssueAllowed(5))
        .extracting(ex -> ((ConsentRefusal) ex).code())
        .isEqualTo(ConsentCodes.OTP_ISSUE_RATE_EXCEEDED);
    assertThat(open.toString()).doesNotContain("hash");
  }

  @Test
  void evaluation_appliesFixedPrecedenceThenTheTypePolicy() {
    ConsentEvidence dp =
        evidence(ConsentCode.CNS_DP, T0, T0.plus(Duration.ofDays(90)), null, "1.0");
    EvaluationDecision allowed =
        evaluation.evaluate(
            request(ConsentCode.CNS_DP, null, "1.1", false, false),
            dp,
            T0.plus(Duration.ofDays(90)));
    assertThat(allowed.allowed()).isTrue();
    assertThat(allowed.effect()).isEqualTo(JourneyEffect.PROCEED);

    EvaluationDecision expired =
        evaluation.evaluate(
            request(ConsentCode.CNS_DP, null, "1.0", false, false),
            dp,
            T0.plus(Duration.ofDays(90)).plusSeconds(1));
    assertThat(expired.errorCode()).isEqualTo(ConsentCodes.CONSENT_EXPIRED);
    assertThat(expired.journeyBlocked()).isTrue();

    EvaluationDecision major =
        evaluation.evaluate(
            request(ConsentCode.CNS_DP, null, "2.0", false, false), dp, T0.plusSeconds(1));
    assertThat(major.errorCode()).isEqualTo(ConsentCodes.CONSENT_REQUIRED);
    EvaluationDecision pastBlock =
        evaluation.evaluate(
            request(ConsentCode.CNS_DP, null, "2.0", true, false), dp, T0.plusSeconds(1));
    assertThat(pastBlock.allowed()).isTrue();

    EvaluationDecision absent =
        evaluation.evaluate(request(ConsentCode.CNS_DP, null, "1.0", false, false), null, T0);
    assertThat(absent.errorCode()).isEqualTo(ConsentCodes.CONSENT_REQUIRED);
    assertThat(absent.journeyBlocked()).isTrue();
    assertThat(absent.effect()).isEqualTo(JourneyEffect.BLOCK_STEP);

    EvaluationDecision comAbsent =
        evaluation.evaluate(request(ConsentCode.CNS_COM, null, "1.0", false, false), null, T0);
    assertThat(comAbsent.allowed()).isFalse();
    assertThat(comAbsent.journeyBlocked()).isFalse();
    assertThat(comAbsent.effect()).isEqualTo(JourneyEffect.SUPPRESS_OPTIONAL_COMMUNICATIONS);

    ConsentEvidence withdrawn =
        evidence(ConsentCode.CNS_COM, T0, null, null, "1.0", ConsentState.WITHDRAWN);
    EvaluationDecision comWithdrawn =
        evaluation.evaluate(
            request(ConsentCode.CNS_COM, null, "1.0", false, false), withdrawn, T0.plusSeconds(5));
    assertThat(comWithdrawn.journeyBlocked()).isFalse();
    assertThat(comWithdrawn.effect()).isEqualTo(JourneyEffect.SUPPRESS_OPTIONAL_COMMUNICATIONS);

    ConsentEvidence sharing =
        evidence(ConsentCode.CNS_SHR, T0, T0.plus(Duration.ofDays(30)), "ins-a", "1.0");
    EvaluationDecision scope =
        evaluation.evaluate(
            request(ConsentCode.CNS_SHR, "ins-b", "1.0", false, false), sharing, T0.plusSeconds(1));
    assertThat(scope.errorCode()).isEqualTo(ConsentCodes.CONSENT_SCOPE_MISMATCH);
    assertThat(scope.effect()).isEqualTo(JourneyEffect.BLOCK_STEP);

    ConsentEvidence otherCustomer =
        evidence(
            ConsentCode.CNS_DP,
            T0,
            T0.plus(Duration.ofDays(90)),
            null,
            "1.0",
            ConsentState.GRANTED,
            "other");
    EvaluationDecision subject =
        evaluation.evaluate(
            request(ConsentCode.CNS_DP, null, "1.0", false, false), otherCustomer, T0);
    assertThat(subject.errorCode()).isEqualTo(ConsentCodes.CONSENT_SUBJECT_MISMATCH);
    assertThat(subject.journeyBlocked()).isTrue();

    ConsentEvidence dpWithdrawn =
        evidence(
            ConsentCode.CNS_DP,
            T0,
            T0.plus(Duration.ofDays(90)),
            null,
            "1.0",
            ConsentState.WITHDRAWN);
    EvaluationDecision halted =
        evaluation.evaluate(
            request(ConsentCode.CNS_DP, null, "1.0", false, true), dpWithdrawn, T0.plusSeconds(1));
    assertThat(halted.errorCode()).isEqualTo(ConsentCodes.JOURNEY_HALTED_CONSENT_WITHDRAWN);
    assertThat(halted.effect()).isEqualTo(JourneyEffect.HALT_JOURNEY);

    EvaluationDecision superseded =
        evaluation.evaluate(
            request(ConsentCode.CNS_DP, null, "1.0", false, false),
            evidence(
                ConsentCode.CNS_DP,
                T0,
                T0.plus(Duration.ofDays(90)),
                null,
                "1.0",
                ConsentState.SUPERSEDED),
            T0);
    assertThat(superseded.errorCode()).isEqualTo(ConsentCodes.CONSENT_REQUIRED);
    EvaluationDecision abandoned =
        evaluation.evaluate(
            request(ConsentCode.CNS_DP, null, "1.0", false, false),
            evidence(
                ConsentCode.CNS_DP,
                T0,
                T0.plus(Duration.ofDays(90)),
                null,
                "1.0",
                ConsentState.ABANDONED),
            T0);
    assertThat(abandoned.errorCode()).isEqualTo(ConsentCodes.CONSENT_EXPIRED);
    assertThat(ConsentState.values()).contains(ConsentState.OTP_PENDING, ConsentState.EXPIRED);
  }

  @Test
  void chains_keepOlderRowsAndSelectTheMatchingHead() {
    ConsentEvidence first =
        evidence(ConsentCode.CNS_DP, T0, T0.plus(Duration.ofDays(90)), null, "1.0");
    ConsentEvidence second =
        new ConsentEvidence(
            "01ARZ3NDEKTSV4RRFFQ69G5FBV",
            first.rootConsentId(),
            2,
            first.evidenceId(),
            "cust-1",
            ConsentCode.CNS_DP,
            ConsentState.WITHDRAWN,
            first.validFrom(),
            first.validUntil(),
            null,
            "stmt",
            "1.0",
            "text",
            StatementRules.sha256("text"),
            "en-IN",
            new byte[] {1},
            "k",
            new byte[] {2},
            "lead",
            "journey",
            ConsentChannel.ASSISTED,
            "otp2",
            T0.plusSeconds(10),
            T0.plusSeconds(10),
            null,
            "actor",
            null,
            "dist",
            "LIFE",
            "cfg",
            "USER",
            null,
            "OTP_SMS_CUSTOMER_DEVICE");
    ConsentEvidence sharing =
        evidence(
            ConsentCode.CNS_SHR,
            T0,
            T0.plus(Duration.ofDays(30)),
            "ins-a",
            "1.0",
            ConsentState.GRANTED,
            "cust-1");
    assertThat(ConsentChains.head(List.of(first, second)).evidenceId())
        .isEqualTo(second.evidenceId());
    assertThat(ConsentChains.isHead(first, List.of(first, second))).isFalse();
    assertThat(
            ConsentChains.selectHead(List.of(first, second, sharing), ConsentCode.CNS_DP, null)
                .evidenceId())
        .isEqualTo(second.evidenceId());
    assertThat(
            ConsentChains.selectHead(List.of(first, sharing), ConsentCode.CNS_SHR, "ins-a").code())
        .isEqualTo(ConsentCode.CNS_SHR);
    assertThat(ConsentChains.selectHead(List.of(sharing), ConsentCode.CNS_DP, null)).isNull();
    assertThat(first.toString()).doesNotContain("cust");
  }

  @Test
  void scrubberAndUlids_doNotKeepACustomerAttribute() {
    String raw =
        "mail a@b.co alias user%name@example.com pan ABCDE1234F aadhaar 1234 5678 9012 mobile 9876543210 ip 203.0.113.10";
    String scrubbed = SensitiveText.scrub(raw);
    assertThat(scrubbed)
        .doesNotContain(
            "a@b.co",
            "user%name@example.com",
            "ABCDE1234F",
            "1234 5678 9012",
            "9876543210",
            "203.0.113.10");
    assertThat(SensitiveText.scrub(null)).isNull();
    assertThat(SensitiveText.scrub("")).isEmpty();
    assertThat(
            SensitiveText.scrub(
                "see @ and a@b.c and a+b_c@d.co and n@example.c1 and n@example.abcdefghijklmnopqrstuvwxy"))
        .contains("a@b.c", "n@example.c1", "n@example.abcdefghijklmnopqrstuvwxy")
        .doesNotContain("a+b_c@d.co");
    String ulid = Ulids.mint(java.time.Clock.fixed(T0, java.time.ZoneOffset.UTC));
    assertThat(ulid).hasSize(26).matches("[0-9A-HJKMNP-TV-Z]{26}");
    assertThat(ulid).doesNotContain("9876543210");
  }

  private static EvaluationRequest request(
      ConsentCode code, String party, String version, boolean pastBlocking, boolean proposal) {
    return new EvaluationRequest("cust-1", code, party, version, pastBlocking, proposal);
  }

  private static PendingCapture pending() {
    return new PendingCapture(
        new byte[] {1},
        "k",
        new byte[] {2},
        "lead",
        "journey",
        "stmt",
        "1.0",
        "text",
        StatementRules.sha256("text"),
        "en-IN",
        ConsentChannel.ASSISTED,
        "actor",
        null,
        "dist",
        null,
        "LIFE",
        "cfg",
        null,
        false,
        null);
  }

  private static ConsentEvidence evidence(
      ConsentCode code, Instant from, Instant until, String party, String version) {
    return evidence(code, from, until, party, version, ConsentState.GRANTED, "cust-1");
  }

  private static ConsentEvidence evidence(
      ConsentCode code,
      Instant from,
      Instant until,
      String party,
      String version,
      ConsentState state) {
    return evidence(code, from, until, party, version, state, "cust-1");
  }

  private static ConsentEvidence evidence(
      ConsentCode code,
      Instant from,
      Instant until,
      String party,
      String version,
      ConsentState state,
      String customerId) {
    return new ConsentEvidence(
        "01ARZ3NDEKTSV4RRFFQ69G5FAV",
        "01ARZ3NDEKTSV4RRFFQ69G5FAV",
        1,
        null,
        customerId,
        code,
        state,
        from,
        until,
        party,
        "stmt",
        version,
        "text",
        StatementRules.sha256("text"),
        "en-IN",
        new byte[] {1},
        "k",
        new byte[] {2},
        "lead",
        "journey",
        ConsentChannel.ASSISTED,
        "otp",
        from,
        from,
        null,
        "actor",
        null,
        "dist",
        "LIFE",
        "cfg",
        state == ConsentState.WITHDRAWN ? "USER" : null,
        null,
        "OTP_SMS_CUSTOMER_DEVICE");
  }
}
