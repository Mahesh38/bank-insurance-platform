package com.bank.platform.consent.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bank.platform.consent.ConsentApplication;
import com.bank.platform.consent.adapter.contact.StubRegisteredContact;
import com.bank.platform.consent.adapter.persistence.JpaOutbox;
import com.bank.platform.consent.adapter.policy.FixtureStatementSource;
import com.bank.platform.consent.application.ConsentApplicationService;
import com.bank.platform.consent.domain.ConsentPorts;
import com.bank.platform.consent.domain.Ulids;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(classes = {ConsentApplication.class, ConsentApiTest.ClockConfig.class})
@AutoConfigureMockMvc
class ConsentApiTest {

  private static final AtomicReference<Instant> NOW =
      new AtomicReference<>(Instant.parse("2026-11-01T00:00:00Z"));

  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired FixtureStatementSource statements;
  @Autowired StubRegisteredContact contacts;
  @Autowired ConsentPorts.ChallengeStore challenges;
  @Autowired ConsentPorts.ConsentStore consents;
  @Autowired JpaOutbox outbox;
  @Autowired ConsentApplicationService service;

  @BeforeEach
  void reset() {
    NOW.set(Instant.parse("2026-11-01T00:00:00Z"));
    statements.setStatus("CNS-DP", "APPROVED");
    statements.setStatus("CNS-SOL", "APPROVED");
    statements.setStatus("CNS-QSH", "APPROVED");
    statements.setStatus("CNS-SHR", "APPROVED");
    statements.setStatus("CNS-COM", "APPROVED");
    contacts.setUnavailable(false);
  }

  @Test
  void atCns01_newCustomerMissesDataProcessingAndSolicitation() throws Exception {
    String customer = ulid();
    mvc.perform(
            get("/internal/v1/consents:check")
                .param("customerId", customer)
                .param("checkpoint", "BEFORE_SUITABILITY")
                .param("lob", "LIFE"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.satisfied").value(false))
        .andExpect(jsonPath("$.captureBlocked").value(false))
        .andExpect(jsonPath("$.missing[?(@.purpose=='CNS-DP')].reason").value("ABSENT"))
        .andExpect(jsonPath("$.missing[?(@.purpose=='CNS-SOL')].reason").value("ABSENT"));
  }

  @Test
  void atCns02_day90StillValidAndDay91Expired() throws Exception {
    String customer = ulid();
    grant(customer, "CNS-DP", "BEFORE_SUITABILITY", null);
    grant(customer, "CNS-SOL", "BEFORE_SUITABILITY", null);
    NOW.set(Instant.parse("2026-11-01T00:00:00Z").plus(90, ChronoUnit.DAYS));
    mvc.perform(check(customer, "BEFORE_MULTI_QUOTE", null))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.valid[?(@.purpose=='CNS-DP')]").isNotEmpty())
        .andExpect(jsonPath("$.valid[?(@.purpose=='CNS-SOL')]").isNotEmpty());
    NOW.set(Instant.parse("2026-11-01T00:00:00Z").plus(91, ChronoUnit.DAYS));
    mvc.perform(check(customer, "BEFORE_MULTI_QUOTE", null))
        .andExpect(jsonPath("$.missing[?(@.purpose=='CNS-DP')].reason").value("EXPIRED"))
        .andExpect(jsonPath("$.missing[?(@.purpose=='CNS-SOL')].reason").value("EXPIRED"));
  }

  @Test
  void atCns03_combinedAcknowledgementIsRefused() throws Exception {
    String customer = ulid();
    mvc.perform(
            capture(customer, java.util.List.of("CNS-DP", "CNS-SOL"), "BEFORE_SUITABILITY", null))
        .andExpect(status().isUnprocessableEntity());
    grant(customer, "CNS-DP", "BEFORE_SUITABILITY", null);
    grant(customer, "CNS-SOL", "BEFORE_SUITABILITY", null);
    mvc.perform(check(customer, "BEFORE_SUITABILITY", null))
        .andExpect(jsonPath("$.satisfied").value(true));
  }

  @Test
  void atCns04_relayedModeIsForbiddenAndCustomerDeviceGrants() throws Exception {
    String customer = ulid();
    MvcResult opened = open(customer, "CNS-DP", "BEFORE_SUITABILITY", null);
    String challengeId =
        json.readTree(opened.getResponse().getContentAsString()).path("challengeId").asText();
    String txn = challenges.find(challengeId).orElseThrow().otpTxnId();
    mvc.perform(verify(challengeId, txn, "000000", "RM_RELAYED")).andExpect(status().isForbidden());
    mvc.perform(verify(challengeId, txn, readOtp(challengeId), "CUSTOMER_DEVICE"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.state").value("VERIFIED"));
  }

  @Test
  void atCns05_draftStatementIsNotCaptured() throws Exception {
    statements.setStatus("CNS-DP", "DRAFT");
    String customer = ulid();
    mvc.perform(check(customer, "BEFORE_SUITABILITY", null))
        .andExpect(jsonPath("$.captureBlocked").value(true))
        .andExpect(
            jsonPath("$.missing[?(@.purpose=='CNS-DP')].reason").value("STATEMENT_NOT_APPROVED"));
    mvc.perform(capture(customer, java.util.List.of("CNS-DP"), "BEFORE_SUITABILITY", null))
        .andExpect(status().isForbidden());
    assertThat(consents.chainIdsForCustomer(customer)).isEmpty();
  }

  @Test
  void atCns06_attemptsLockAndExpiry() throws Exception {
    String customer = ulid();
    String challengeId = openId(customer, "CNS-COM", "BEFORE_FIRST_COMMUNICATION", null);
    String txn = challenges.find(challengeId).orElseThrow().otpTxnId();
    mvc.perform(verify(challengeId, txn, "000000", "CUSTOMER_DEVICE"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.errors[0].extensions.attemptsRemaining").value(2));
    mvc.perform(verify(challengeId, txn, "000000", "CUSTOMER_DEVICE"));
    mvc.perform(verify(challengeId, txn, "000000", "CUSTOMER_DEVICE"))
        .andExpect(jsonPath("$.errors[0].extensions.attemptsRemaining").value(0));
    mvc.perform(verify(challengeId, txn, "000000", "CUSTOMER_DEVICE"))
        .andExpect(status().isTooManyRequests());
    assertThat(challenges.find(challengeId).orElseThrow().state()).isEqualTo("LOCKED");

    String expired = openId(customer, "CNS-DP", "BEFORE_SUITABILITY", null);
    NOW.set(NOW.get().plus(11, ChronoUnit.MINUTES));
    String expiredTxn = challenges.find(expired).orElseThrow().otpTxnId();
    mvc.perform(verify(expired, expiredTxn, readOtp(expired), "CUSTOMER_DEVICE"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("OTP_EXPIRED"));
  }

  @Test
  void atCns07_sixthIssueIsRefused() throws Exception {
    String customer = ulid();
    for (int i = 0; i < 5; i++) {
      openId(customer, "CNS-DP", "BEFORE_SUITABILITY", null);
    }
    mvc.perform(capture(customer, java.util.List.of("CNS-DP"), "BEFORE_SUITABILITY", null))
        .andExpect(status().isTooManyRequests())
        .andExpect(jsonPath("$.code").value("OTP_ISSUE_RATE_EXCEEDED"));
  }

  @Test
  void atCns08_postgresRoleIsInsertSelectOnlyOnEvidence() {
    assertThat(db.migration.V3__AppConsentGrants.EVIDENCE_GRANT)
        .contains("INSERT, SELECT")
        .doesNotContain("UPDATE")
        .doesNotContain("DELETE");
    assertThat(db.migration.V3__AppConsentGrants.CHALLENGE_GRANT).contains("UPDATE");
  }

  @Test
  void atCns09_newGrantSupersedesTheLiveChain() throws Exception {
    String customer = ulid();
    grant(customer, "CNS-DP", "BEFORE_SUITABILITY", null);
    String first = consents.head(customer, "CNS-DP", null).orElseThrow().consentId();
    grant(customer, "CNS-DP", "BEFORE_SUITABILITY", null);
    var head = consents.head(customer, "CNS-DP", null).orElseThrow();
    assertThat(head.consentId()).isNotEqualTo(first);
    assertThat(head.supersedesConsentId()).isEqualTo(first);
    assertThat(consents.headOf(first).orElseThrow().state()).isEqualTo("SUPERSEDED");
  }

  @Test
  void atCns10_otherCustomerIsASecurityEvent() throws Exception {
    String customer = ulid();
    String consentId = grant(customer, "CNS-DP", "BEFORE_SUITABILITY", null);
    mvc.perform(
            get("/internal/v1/consents/" + consentId)
                .header("X-Actor-Id", "rm-1")
                .param("customerId", ulid()))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("CONSENT_SUBJECT_MISMATCH"));
  }

  @Test
  void atCns11_withdrawalEventOmitsTheCodeReference() throws Exception {
    String customer = ulid();
    String consentId = grant(customer, "CNS-DP", "BEFORE_SUITABILITY", null);
    MvcResult opened =
        mvc.perform(
                post("/internal/v1/consents/" + consentId + "/withdrawals")
                    .header("X-Actor-Id", "rm-1")
                    .header("Idempotency-Key", "withdraw-" + consentId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"channel\":\"RM_ASSISTED\",\"reasonCode\":\"CUSTOMER_REQUEST\"}"))
            .andExpect(status().isCreated())
            .andReturn();
    String challengeId =
        json.readTree(opened.getResponse().getContentAsString()).path("challengeId").asText();
    mvc.perform(
            verify(
                challengeId,
                challenges.find(challengeId).orElseThrow().otpTxnId(),
                readOtp(challengeId),
                "CUSTOMER_DEVICE"))
        .andExpect(status().isOk());
    String payload =
        outbox.forAggregate(consentId).stream()
            .filter(row -> "ConsentWithdrawn".equals(row.getEventType()))
            .findFirst()
            .orElseThrow()
            .getPayload();
    assertThat(payload).doesNotContain("otpTxnId").contains("CUSTOMER_REQUEST");
  }

  @Test
  void atCns12_contactOutageWritesNothing() throws Exception {
    contacts.setUnavailable(true);
    String customer = ulid();
    mvc.perform(capture(customer, java.util.List.of("CNS-DP"), "BEFORE_SUITABILITY", null))
        .andExpect(status().isServiceUnavailable());
    assertThat(consents.chainIdsForCustomer(customer)).isEmpty();
  }

  @Test
  void atCns13_unknownCheckpointIsUnresolvable() throws Exception {
    mvc.perform(check(ulid(), "NOT_A_CHECKPOINT", null))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("CONFIGURATION_UNRESOLVABLE"));
  }

  @Test
  void atCns14_sweepIsIdempotentAndCheckDoesNotNeedIt() throws Exception {
    String customer = ulid();
    grant(customer, "CNS-DP", "BEFORE_SUITABILITY", null);
    NOW.set(Instant.parse("2026-11-01T00:00:00Z").plus(91, ChronoUnit.DAYS));
    mvc.perform(check(customer, "BEFORE_SUITABILITY", null))
        .andExpect(jsonPath("$.missing[?(@.purpose=='CNS-DP')].reason").value("EXPIRED"));
    assertThat(service.sweep()).isPositive();
    int second = service.sweep();
    assertThat(second).isZero();
  }

  private String grant(String customer, String purpose, String checkpoint, String insurer)
      throws Exception {
    String challengeId = openId(customer, purpose, checkpoint, insurer);
    String txn = challenges.find(challengeId).orElseThrow().otpTxnId();
    MvcResult result =
        mvc.perform(verify(challengeId, txn, readOtp(challengeId), "CUSTOMER_DEVICE"))
            .andExpect(status().isOk())
            .andReturn();
    return json.readTree(result.getResponse().getContentAsString())
        .path("consentIds")
        .get(0)
        .asText();
  }

  private String openId(String customer, String purpose, String checkpoint, String insurer)
      throws Exception {
    return json.readTree(
            open(customer, purpose, checkpoint, insurer).getResponse().getContentAsString())
        .path("challengeId")
        .asText();
  }

  private MvcResult open(String customer, String purpose, String checkpoint, String insurer)
      throws Exception {
    return mvc.perform(capture(customer, java.util.List.of(purpose), checkpoint, insurer))
        .andExpect(status().isCreated())
        .andReturn();
  }

  private org.springframework.test.web.servlet.RequestBuilder capture(
      String customer, java.util.List<String> purposes, String checkpoint, String insurer)
      throws Exception {
    var body = new java.util.LinkedHashMap<String, Object>();
    body.put("customerId", customer);
    body.put("leadId", ulid());
    body.put("journeyId", ulid());
    body.put("lob", "LIFE");
    body.put("checkpoint", checkpoint);
    body.put("purposes", purposes);
    body.put("channel", "RM_ASSISTED");
    body.put("language", "en-IN");
    if (insurer != null) {
      body.put("insurerCode", insurer);
    }
    return post("/internal/v1/consents")
        .header("X-Actor-Id", "rm-1")
        .header("Idempotency-Key", "cap-" + ulid())
        .contentType(MediaType.APPLICATION_JSON)
        .content(json.writeValueAsString(body));
  }

  private org.springframework.test.web.servlet.RequestBuilder check(
      String customer, String checkpoint, String insurer) {
    var request =
        get("/internal/v1/consents:check")
            .param("customerId", customer)
            .param("checkpoint", checkpoint)
            .param("lob", "LIFE");
    if (insurer != null) {
      request.param("insurerCode", insurer);
    }
    return request;
  }

  private org.springframework.test.web.servlet.RequestBuilder verify(
      String challengeId, String txn, String otp, String mode) throws Exception {
    return post("/internal/v1/consent-challenges/" + challengeId + "/verify")
        .header("X-Actor-Id", "rm-1")
        .header("Idempotency-Key", "ver-" + ulid())
        .contentType(MediaType.APPLICATION_JSON)
        .content(
            json.writeValueAsString(
                Map.of(
                    "otpTxnId",
                    txn,
                    "otp",
                    otp,
                    "verificationMode",
                    mode,
                    "sourceIp",
                    "203.0.113.10",
                    "affirmations",
                    java.util.List.of(affirmation(challengeId)))));
  }

  private Map<String, String> affirmation(String challengeId) throws Exception {
    var refs = json.readTree(challenges.find(challengeId).orElseThrow().statementRefs());
    var first = refs.get(0);
    return Map.of(
        "purpose",
        first.path("purpose").asText(),
        "statementId",
        first.path("statementId").asText(),
        "statementVersion",
        first.path("statementVersion").asText(),
        "affirmedAt",
        NOW.get().toString());
  }

  private String readOtp(String challengeId) {
    String hash = challenges.find(challengeId).orElseThrow().otpHash();
    for (int i = 0; i <= 999999; i++) {
      String candidate = String.format("%06d", i);
      if (new com.bank.platform.consent.adapter.crypto.HmacOtpHasher("local-otp-pepper")
          .matches(candidate, hash)) {
        return candidate;
      }
    }
    throw new IllegalStateException("code not found");
  }

  private static String ulid() {
    return Ulids.mint(Clock.fixed(NOW.get(), ZoneOffset.UTC));
  }

  @TestConfiguration
  static class ClockConfig {
    @Bean
    @Primary
    Clock clock() {
      return new Clock() {
        @Override
        public ZoneId getZone() {
          return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
          return this;
        }

        @Override
        public Instant instant() {
          return NOW.get();
        }
      };
    }
  }
}
