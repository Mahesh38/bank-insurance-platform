package com.bank.platform.consent.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bank.platform.consent.adapter.crypto.AesFieldCipher;
import com.bank.platform.consent.adapter.crypto.HmacOtpHasher;
import com.bank.platform.consent.domain.ConsentChains;
import com.bank.platform.consent.domain.ConsentCode;
import com.bank.platform.consent.domain.ConsentCodes;
import com.bank.platform.consent.domain.ConsentEvaluation;
import com.bank.platform.consent.domain.ConsentEvidence;
import com.bank.platform.consent.domain.ConsentEvidenceStore;
import com.bank.platform.consent.domain.ConsentPolicyRegistry;
import com.bank.platform.consent.domain.ConsentRefusal;
import com.bank.platform.consent.domain.ConsentState;
import com.bank.platform.consent.domain.JourneyEffect;
import com.bank.platform.consent.domain.OtpChallenge;
import com.bank.platform.consent.domain.OtpChallengeStore;
import com.bank.platform.consent.domain.PublishedStatement;
import com.bank.platform.consent.domain.RetentionStore;
import com.bank.platform.consent.domain.StatementStore;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class ConsentApplicationServiceTest {

  @Test
  void capture_reusesALiveGrantAndChainsWithdrawalOnTheSameRoot() {
    World world = new World();
    world.publish("CNS-DP", "1.0", "en-IN", "MINOR");
    var first = world.grant("CNS-DP", "1.0", null);
    assertThat(first.challengeIssued()).isTrue();
    ConsentEvidence granted = world.verify(first.challenge().challengeId());
    assertThat(granted.recordedState()).isEqualTo(ConsentState.GRANTED);
    assertThat(granted.acknowledgementMethod()).isEqualTo(ConsentApplicationService.ACK_OTP);
    assertThat(granted.recordVersion()).isEqualTo(1);
    assertThat(world.cipher.decrypt(granted.cifEnc())).isEqualTo("CIF9000111");

    var again = world.grant("CNS-DP", "1.0", null);
    assertThat(again.challengeIssued()).isFalse();
    assertThat(again.reused().evidenceId()).isEqualTo(granted.evidenceId());

    var withdrawal =
        world.service.openWithdrawal(
            granted.rootConsentId(),
            "contact-ref-1",
            "USER_REQUEST",
            false,
            "cust-device",
            "203.0.113.44");
    ConsentEvidence withdrawn = world.verify(withdrawal.challenge().challengeId());
    assertThat(withdrawn.recordedState()).isEqualTo(ConsentState.WITHDRAWN);
    assertThat(withdrawn.rootConsentId()).isEqualTo(granted.rootConsentId());
    assertThat(withdrawn.recordVersion()).isEqualTo(2);
    assertThat(withdrawn.capturedByActorId()).isEqualTo("cust-device");
    assertThat(withdrawn.sourceIp()).isEqualTo("203.0.113.44");
    assertThat(withdrawn.supersedesEvidenceId()).isEqualTo(granted.evidenceId());

    List<ConsentApplicationService.ChainRow> chain = world.service.chain(granted.rootConsentId());
    assertThat(chain).hasSize(2);
    assertThat(chain.get(0).evidence().recordedState()).isEqualTo(ConsentState.GRANTED);
    assertThat(chain.get(0).effectiveStatus()).isEqualTo(ConsentState.SUPERSEDED);
    assertThat(chain.get(1).effectiveStatus()).isEqualTo(ConsentState.WITHDRAWN);

    var decision =
        world.evaluate("CNS-DP", null, "1.0", false, "CIF9000111", granted.rootConsentId());
    assertThat(decision.errorCode()).isEqualTo(ConsentCodes.JOURNEY_HALTED_CONSENT_WITHDRAWN);
    assertThat(decision.journeyBlocked()).isTrue();
  }

  @Test
  void types_divergeOnExpiryScopeCommunicationsAndSubject() {
    World world = new World();
    world.publish("CNS-DP", "1.0", "en-IN", "MINOR");
    world.publish("CNS-DP", "2.0", "en-IN", "MAJOR");
    world.publish("CNS-SHR", "1.0", "en-IN", "MINOR");
    world.publish("CNS-COM", "1.0", "en-IN", "MINOR");
    world.publish("CNS-SOL", "1.0", "en-IN", "MINOR");

    ConsentEvidence dp = world.verify(world.grant("CNS-DP", "1.0", null).challenge().challengeId());
    world.clock.set(dp.validUntil());
    assertThat(world.evaluate("CNS-DP", null, "1.0", false, "CIF9000111", null).allowed()).isTrue();
    world.clock.set(dp.validUntil().plusSeconds(1));
    assertThat(world.evaluate("CNS-DP", null, "1.0", false, "CIF9000111", null).errorCode())
        .isEqualTo(ConsentCodes.CONSENT_EXPIRED);

    world.clock.set(Instant.parse("2026-01-01T00:00:00Z"));
    ConsentEvidence sharing =
        world.verify(world.grant("CNS-SHR", "1.0", "ins-a").challenge().challengeId());
    assertThat(
            world
                .evaluate("CNS-SHR", "ins-b", "1.0", false, "CIF9000111", sharing.rootConsentId())
                .errorCode())
        .isEqualTo(ConsentCodes.CONSENT_SCOPE_MISMATCH);
    assertThat(world.evaluate("CNS-DP", null, "1.0", false, "CIF9000111", null).allowed()).isTrue();

    var com = world.evaluate("CNS-COM", null, "1.0", false, "CIF9000111", null);
    assertThat(com.allowed()).isFalse();
    assertThat(com.journeyBlocked()).isFalse();
    assertThat(com.effect()).isEqualTo(JourneyEffect.SUPPRESS_OPTIONAL_COMMUNICATIONS);

    var mismatch =
        world.evaluate("CNS-SHR", "ins-a", "1.0", false, "CIF-OTHER", sharing.rootConsentId());
    assertThat(mismatch.errorCode()).isEqualTo(ConsentCodes.CONSENT_SUBJECT_MISMATCH);
    assertThat(world.events).isNotEmpty();
    assertThat(world.events.get(0)[2]).isEqualTo(sharing.rootConsentId());
    assertThat(String.join(",", world.events.get(0))).doesNotContain("CIF");

    world.clock.set(Instant.parse("2026-01-01T00:00:00Z"));
    ConsentEvidence fresh =
        world.verify(world.grant("CNS-SOL", "1.0", null).challenge().challengeId());
    assertThat(world.evaluate("CNS-SOL", null, "2.0", false, "CIF9000111", null).errorCode())
        .isEqualTo(ConsentCodes.CONSENT_REQUIRED);
    assertThat(
            world
                .evaluate("CNS-SOL", null, "2.0", true, "CIF9000111", fresh.rootConsentId())
                .allowed())
        .isTrue();
  }

  @Test
  void otp_locksAfterThreeMissesAndExpiresOneSecondAfterTheWindow() {
    World world = new World();
    world.publish("CNS-DP", "1.0", "en-IN", "MINOR");
    var opened = world.grant("CNS-DP", "1.0", null);
    String id = opened.challenge().challengeId();
    Instant expiry = opened.challenge().expiresAt();
    world.clock.set(expiry);
    assertThatThrownBy(() -> world.service.verify(id, "000000", "CUSTOMER_DEVICE"))
        .extracting(ex -> ((ConsentRefusal) ex).code())
        .isEqualTo(ConsentCodes.CONSENT_OTP_NOT_VERIFIED);
    world.clock.set(expiry.plusSeconds(1));
    assertThatThrownBy(() -> world.service.verify(id, world.otp(id), "CUSTOMER_DEVICE"))
        .extracting(ex -> ((ConsentRefusal) ex).code())
        .isEqualTo(ConsentCodes.OTP_EXPIRED);

    world.clock.set(Instant.parse("2026-01-02T00:00:00Z"));
    var second = world.grant("CNS-DP", "1.0", null);
    for (int i = 0; i < 2; i++) {
      assertThatThrownBy(
              () ->
                  world.service.verify(
                      second.challenge().challengeId(), "111111", "CUSTOMER_DEVICE"))
          .extracting(ex -> ((ConsentRefusal) ex).code())
          .isEqualTo(ConsentCodes.CONSENT_OTP_NOT_VERIFIED);
    }
    assertThatThrownBy(
            () ->
                world.service.verify(second.challenge().challengeId(), "111111", "CUSTOMER_DEVICE"))
        .extracting(ex -> ((ConsentRefusal) ex).code())
        .isEqualTo(ConsentCodes.OTP_ATTEMPTS_EXCEEDED);
    assertThatThrownBy(
            () ->
                world.service.verify(
                    second.challenge().challengeId(),
                    world.otp(second.challenge().challengeId()),
                    "CUSTOMER_DEVICE"))
        .extracting(ex -> ((ConsentRefusal) ex).code())
        .isEqualTo(ConsentCodes.OTP_ATTEMPTS_EXCEEDED);
    assertThatThrownBy(
            () ->
                world.service.verify(
                    second.challenge().challengeId(),
                    world.otp(second.challenge().challengeId()),
                    "RM"))
        .extracting(ex -> ((ConsentRefusal) ex).code())
        .isEqualTo(ConsentCodes.CONSENT_DEVICE_ISOLATION);
  }

  @Test
  void guards_refuseRawMobileDiyRiderBadLobAndASixthCode() {
    World world = new World();
    world.publish("CNS-DP", "1.0", "en-IN", "MINOR");
    assertThatThrownBy(() -> world.publish("CNS-DP", "1.0", "en-IN", "MINOR"))
        .extracting(ex -> ((ConsentRefusal) ex).code())
        .isEqualTo(ConsentCodes.STATEMENT_VERSION_IMMUTABLE);
    assertThatThrownBy(() -> world.publish("CNS-NEW", "1.0", "hi-IN", "MINOR"))
        .extracting(ex -> ((ConsentRefusal) ex).code())
        .isEqualTo(ConsentCodes.STATEMENT_NOT_PUBLISHED);
    world.publish("CNS-HI", "1.0", "en-IN", "MINOR");
    world.publish("CNS-HI", "1.0", "hi-IN", "MINOR");

    assertThatThrownBy(() -> world.grantRaw("9876543210"))
        .extracting(ex -> ((ConsentRefusal) ex).code())
        .isEqualTo(ConsentCodes.INVALID_REQUEST);
    assertThatThrownBy(
            () ->
                world.service.openGrant(
                    world.command("CNS-DP", "1.0", null, "DIY", "contact-ref-1", "LIFE")))
        .extracting(ex -> ((ConsentRefusal) ex).code())
        .isEqualTo(ConsentCodes.INVALID_REQUEST);
    assertThatThrownBy(
            () ->
                world.service.openGrant(
                    world.command("CNS-RDR", "1.0", null, "ASSISTED", "contact-ref-1", "LIFE")))
        .extracting(ex -> ((ConsentRefusal) ex).code())
        .isEqualTo(ConsentCodes.INVALID_REQUEST);
    assertThatThrownBy(
            () ->
                world.service.openGrant(
                    world.command("CNS-DP", "1.0", null, "ASSISTED", "contact-ref-1", "MOTOR")))
        .extracting(ex -> ((ConsentRefusal) ex).code())
        .isEqualTo(ConsentCodes.UNSUPPORTED_LOB);
    assertThatThrownBy(
            () ->
                world.service.openGrant(
                    world.command("CNS-SHR", "1.0", null, "ASSISTED", "contact-ref-1", "LIFE")))
        .extracting(ex -> ((ConsentRefusal) ex).code())
        .isEqualTo(ConsentCodes.INVALID_REQUEST);

    for (int i = 0; i < 5; i++) {
      world.clock.set(Instant.parse("2026-01-03T00:00:00Z").plusSeconds(i));
      world.grant("CNS-DP", "1.0", null);
    }
    assertThatThrownBy(() -> world.grant("CNS-DP", "1.0", null))
        .extracting(ex -> ((ConsentRefusal) ex).code())
        .isEqualTo(ConsentCodes.OTP_ISSUE_RATE_EXCEEDED);
  }

  @Test
  void sharingWithdrawalAfterSubmission_setsTheOperationsHintAndRetentionIsSevenCalendarYears() {
    World world = new World();
    world.publish("CNS-SHR", "1.0", "en-IN", "MINOR");
    ConsentEvidence granted =
        world.verify(world.grant("CNS-SHR", "1.0", "ins-a").challenge().challengeId());
    var withdrawal =
        world.service.openWithdrawal(
            granted.rootConsentId(),
            "contact-ref-1",
            "USER_REQUEST",
            true,
            "cust-device",
            "203.0.113.44");
    assertThat(withdrawal.effect()).isEqualTo(JourneyEffect.HALT_FURTHER_SHARING);
    ConsentEvidence row = world.verify(withdrawal.challenge().challengeId());
    assertThat(row.opsTaskHint()).isEqualTo(ConsentApplicationService.OPS_POST_SUBMISSION);

    Instant terminal = Instant.parse("2024-02-29T00:00:00Z");
    var anchor = world.service.anchorRetention(granted.rootConsentId(), terminal);
    assertThat(anchor.retainUntil()).isEqualTo(Instant.parse("2031-02-28T00:00:00Z"));
    world.service.anchorRetention(granted.rootConsentId(), terminal);
    assertThatThrownBy(
            () -> world.service.anchorRetention(granted.rootConsentId(), terminal.plusSeconds(1)))
        .extracting(ex -> ((ConsentRefusal) ex).code())
        .isEqualTo(ConsentCodes.CONFLICT);
  }

  @Test
  void edges_refuseMissingFactsAndABrokenEvidenceWrite() {
    World world = new World();
    world.publish("CNS-DP", "1.0", "en-IN", "MINOR");
    assertThatThrownBy(() -> world.service.verify("missing", " ", "CUSTOMER_DEVICE"))
        .extracting(ex -> ((ConsentRefusal) ex).code())
        .isEqualTo(ConsentCodes.CONSENT_OTP_REQUIRED);
    assertThatThrownBy(() -> world.service.verify("missing", "000000", "CUSTOMER_DEVICE"))
        .extracting(ex -> ((ConsentRefusal) ex).code())
        .isEqualTo(ConsentCodes.RESOURCE_NOT_FOUND);
    assertThatThrownBy(() -> world.service.chain("missing"))
        .extracting(ex -> ((ConsentRefusal) ex).code())
        .isEqualTo(ConsentCodes.RESOURCE_NOT_FOUND);
    assertThatThrownBy(
            () ->
                world.service.openWithdrawal(
                    "missing", "contact-ref-1", "USER", false, "actor", null))
        .extracting(ex -> ((ConsentRefusal) ex).code())
        .isEqualTo(ConsentCodes.RESOURCE_NOT_FOUND);
    assertThatThrownBy(
            () -> world.service.anchorRetention("missing", Instant.parse("2026-01-01T00:00:00Z")))
        .extracting(ex -> ((ConsentRefusal) ex).code())
        .isEqualTo(ConsentCodes.RESOURCE_NOT_FOUND);
    assertThatThrownBy(() -> world.grant("CNS-SOL", "9.9", null))
        .extracting(ex -> ((ConsentRefusal) ex).code())
        .isEqualTo(ConsentCodes.STATEMENT_NOT_PUBLISHED);
    assertThatThrownBy(
            () ->
                world.service.openGrant(
                    world.command("CNS-DP", "1.0", null, "ASSISTED", " ", "LIFE")))
        .extracting(ex -> ((ConsentRefusal) ex).code())
        .isEqualTo(ConsentCodes.INVALID_REQUEST);
    OpenGrantCommand blankCustomer =
        new OpenGrantCommand(
            "lead-1",
            "journey-1",
            " ",
            "CIF9000111",
            "CNS-DP",
            "CNS-DP",
            "1.0",
            "en-IN",
            "ASSISTED",
            "contact-ref-1",
            "rm-1",
            "agent-1",
            "HEALTH",
            null,
            "cfg-1",
            null);
    assertThatThrownBy(() -> world.service.openGrant(blankCustomer))
        .extracting(ex -> ((ConsentRefusal) ex).code())
        .isEqualTo(ConsentCodes.INVALID_REQUEST);

    OpenGrantCommand noLanguage =
        world.command("CNS-DP", "1.0", null, "ASSISTED", "contact-ref-1", "GENERAL");
    noLanguage =
        new OpenGrantCommand(
            noLanguage.leadId(),
            noLanguage.journeyId(),
            noLanguage.customerId(),
            noLanguage.customerCif(),
            noLanguage.consentCode(),
            noLanguage.statementId(),
            noLanguage.statementVersion(),
            " ",
            noLanguage.channel(),
            noLanguage.registeredContactRef(),
            noLanguage.capturedByActorId(),
            noLanguage.capturedByAgentId(),
            noLanguage.lob(),
            noLanguage.counterpartyId(),
            noLanguage.configVersionRef(),
            noLanguage.sourceIp());
    ConsentEvidence granted =
        world.verify(world.service.openGrant(noLanguage).challenge().challengeId());
    assertThat(world.service.byJourney("journey-1")).contains(granted);
    assertThat(world.service.byCustomer("cust-1")).contains(granted);
    assertThat(world.service.decryptCif(granted)).isEqualTo("CIF9000111");
    assertThat(ConsentApplicationService.safe("mail a@b.co")).doesNotContain("a@b.co");

    world.clock.set(granted.validUntil().plusSeconds(1));
    assertThatThrownBy(
            () ->
                world.service.openWithdrawal(
                    granted.rootConsentId(), "contact-ref-1", "USER", false, "actor", null))
        .extracting(ex -> ((ConsentRefusal) ex).code())
        .isEqualTo(ConsentCodes.ILLEGAL_TRANSITION);

    var mismatch = world.evaluate("CNS-DP", null, "1.0", false, "CIF-OTHER", null);
    assertThat(mismatch.errorCode()).isEqualTo(ConsentCodes.CONSENT_SUBJECT_MISMATCH);

    var opened = world.grant("CNS-DP", "1.0", null);
    world.evidence.fail = new ConsentRefusal(ConsentCodes.CONFLICT, "duplicate evidence");
    assertThatThrownBy(() -> world.verify(opened.challenge().challengeId()))
        .extracting(ex -> ((ConsentRefusal) ex).code())
        .isEqualTo(ConsentCodes.CONFLICT);
    var again = world.grant("CNS-DP", "1.0", null);
    world.evidence.fail = new IllegalStateException("disk");
    assertThatThrownBy(() -> world.verify(again.challenge().challengeId()))
        .extracting(ex -> ((ConsentRefusal) ex).code())
        .isEqualTo(ConsentCodes.CONSENT_EVIDENCE_WRITE_FAILED);
    assertThatThrownBy(() -> world.service.anchorRetention(granted.rootConsentId(), null))
        .extracting(ex -> ((ConsentRefusal) ex).code())
        .isEqualTo(ConsentCodes.INVALID_REQUEST);
  }

  @Test
  void dispatchFailure_refusesWithoutReturningTheCode() {
    World world = new World();
    world.publish("CNS-DP", "1.0", "en-IN", "MINOR");
    world.failDispatch = true;
    assertThatThrownBy(() -> world.grant("CNS-DP", "1.0", null))
        .extracting(ex -> ((ConsentRefusal) ex).code())
        .isEqualTo(ConsentCodes.CONSENT_EVIDENCE_WRITE_FAILED);
    assertThat(world.otps).isEmpty();
  }

  private static final class World {
    final MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
    final AesFieldCipher cipher = new AesFieldCipher(AesFieldCipher.DEV_KEY, "local-dev");
    final HmacOtpHasher hasher = new HmacOtpHasher("pepper");
    final Map<String, String> otps = new HashMap<>();
    final List<String[]> events = new ArrayList<>();
    final MemoryEvidence evidence = new MemoryEvidence();
    final MemoryStatements statements = new MemoryStatements();
    final MemoryChallenges challenges = new MemoryChallenges();
    final MemoryRetention retention = new MemoryRetention();
    final ConsentApplicationService service;
    boolean failDispatch;

    World() {
      ConsentPolicyRegistry registry = ConsentPolicyRegistry.r0();
      service =
          new ConsentApplicationService(
              evidence,
              statements,
              challenges,
              new OtpAttemptRecorder(challenges),
              retention,
              registry,
              new ConsentEvaluation(registry),
              cipher,
              hasher,
              (id, contact, otp) -> {
                if (failDispatch) {
                  throw new IllegalStateException("dispatch unavailable");
                }
                otps.put(id, otp);
              },
              (actor, journey, consent) -> events.add(new String[] {actor, journey, consent}),
              clock,
              "dist-local");
    }

    void publish(String id, String version, String language, String changeClass) {
      service.publish(
          id,
          version,
          "Notice " + id + " " + version + " " + language,
          language,
          changeClass,
          "legal");
    }

    ConsentApplicationService.GrantOutcome grant(String code, String version, String party) {
      return service.openGrant(command(code, version, party, "ASSISTED", "contact-ref-1", "LIFE"));
    }

    ConsentApplicationService.GrantOutcome grantRaw(String contact) {
      return service.openGrant(command("CNS-DP", "1.0", null, "ASSISTED", contact, "LIFE"));
    }

    OpenGrantCommand command(
        String code, String version, String party, String channel, String contact, String lob) {
      String statementId = "CNS-RDR".equals(code) ? "CNS-DP" : code;
      return new OpenGrantCommand(
          "lead-1",
          "journey-1",
          "cust-1",
          "CIF9000111",
          code,
          statementId,
          version,
          "en-IN",
          channel,
          contact,
          "rm-1",
          "agent-1",
          lob,
          party,
          "cfg-1",
          "203.0.113.44");
    }

    ConsentEvidence verify(String challengeId) {
      return service.verify(challengeId, otp(challengeId), "CUSTOMER_DEVICE");
    }

    com.bank.platform.consent.domain.EvaluationDecision evaluate(
        String code, String party, String version, boolean past, String cif, String presented) {
      return service.evaluate(
          new EvaluationCommand(
              "cust-1", code, party, version, past, false, cif, "journey-1", presented, "rm-1"));
    }

    String otp(String challengeId) {
      return otps.get(challengeId);
    }
  }

  private static final class MutableClock extends Clock {
    private Instant now;

    MutableClock(Instant now) {
      this.now = now;
    }

    void set(Instant now) {
      this.now = now;
    }

    @Override
    public ZoneOffset getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(java.time.ZoneId zone) {
      return this;
    }

    @Override
    public Instant instant() {
      return now;
    }
  }

  private static final class MemoryEvidence implements ConsentEvidenceStore {
    private final List<ConsentEvidence> rows = new ArrayList<>();
    RuntimeException fail;

    @Override
    public void append(ConsentEvidence row) {
      if (fail != null) {
        RuntimeException ex = fail;
        fail = null;
        throw ex;
      }
      boolean duplicate =
          rows.stream()
              .anyMatch(
                  existing ->
                      existing.evidenceId().equals(row.evidenceId())
                          || (existing.rootConsentId().equals(row.rootConsentId())
                              && existing.recordVersion() == row.recordVersion()));
      if (duplicate) {
        throw new ConsentRefusal(
            ConsentCodes.CONSENT_EVIDENCE_WRITE_FAILED, "evidence write failed");
      }
      rows.add(row);
    }

    @Override
    public List<ConsentEvidence> chain(String rootConsentId) {
      return rows.stream().filter(row -> row.rootConsentId().equals(rootConsentId)).toList();
    }

    @Override
    public List<ConsentEvidence> byJourney(String journeyId) {
      return rows.stream().filter(row -> journeyId.equals(row.journeyId())).toList();
    }

    @Override
    public List<ConsentEvidence> byCustomer(String customerId) {
      return rows.stream().filter(row -> customerId.equals(row.customerId())).toList();
    }

    @Override
    public Optional<ConsentEvidence> head(
        String customerId, ConsentCode code, String counterpartyId) {
      return Optional.ofNullable(
          ConsentChains.selectHead(byCustomer(customerId), code, counterpartyId));
    }
  }

  private static final class MemoryStatements implements StatementStore {
    private final Map<String, PublishedStatement> rows = new HashMap<>();

    @Override
    public void insert(PublishedStatement statement) {
      String key = key(statement.statementId(), statement.version(), statement.language());
      if (rows.containsKey(key)) {
        throw new ConsentRefusal(
            ConsentCodes.STATEMENT_VERSION_IMMUTABLE, "statement version is immutable");
      }
      rows.put(key, statement);
    }

    @Override
    public Optional<PublishedStatement> find(String statementId, String version, String language) {
      return Optional.ofNullable(rows.get(key(statementId, version, language)));
    }

    @Override
    public boolean englishSourceExists(String statementId, String version) {
      return find(statementId, version, "en-IN").isPresent();
    }

    private static String key(String id, String version, String language) {
      return id + "|" + version + "|" + language;
    }
  }

  private static final class MemoryChallenges implements OtpChallengeStore {
    private final Map<String, OtpChallenge> rows = new HashMap<>();

    @Override
    public void save(OtpChallenge challenge) {
      rows.put(challenge.challengeId(), challenge);
    }

    @Override
    public Optional<OtpChallenge> find(String challengeId) {
      return Optional.ofNullable(rows.get(challengeId));
    }

    @Override
    public int issuesSince(String customerId, ConsentCode code, Instant since) {
      return (int)
          rows.values().stream()
              .filter(row -> row.customerId().equals(customerId) && row.code() == code)
              .filter(row -> !row.createdAt().isBefore(since))
              .count();
    }
  }

  private static final class MemoryRetention implements RetentionStore {
    private final Map<String, Instant[]> rows = new HashMap<>();

    @Override
    public void anchor(String rootConsentId, Instant terminalAt, Instant retainUntil) {
      Instant[] existing = rows.get(rootConsentId);
      if (existing != null) {
        if (existing[0].equals(terminalAt) && existing[1].equals(retainUntil)) {
          return;
        }
        throw new ConsentRefusal(ConsentCodes.CONFLICT, "retention anchor is immutable");
      }
      rows.put(rootConsentId, new Instant[] {terminalAt, retainUntil});
    }
  }
}
