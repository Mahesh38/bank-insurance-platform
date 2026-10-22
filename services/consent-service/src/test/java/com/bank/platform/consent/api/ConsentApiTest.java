package com.bank.platform.consent.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.bank.platform.consent.adapter.security.LoggingSecurityEventSink;
import com.bank.platform.consent.domain.OtpDispatch;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.sql.DataSource;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@Tag("integration")
@SpringBootTest(
    properties =
        "spring.datasource.url=jdbc:h2:mem:consent_api;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
@AutoConfigureMockMvc
class ConsentApiTest {

  private static final String CIF = "CIF9000111";
  private static final String IP = "203.0.113.44";
  private static final String CONTACT = "contact-ref-9001";

  @Autowired private MockMvc mvc;
  @Autowired private CapturingOtpDispatch dispatch;
  @Autowired private DataSource dataSource;

  @org.junit.jupiter.api.BeforeEach
  void resetDispatch() {
    dispatch.reset();
  }

  @Test
  void smoke_requestVerifyEvaluateWithdrawReconsentAndReadTheChain() throws Exception {
    ListAppender<ILoggingEvent> logs = attach(CapturingOtpDispatch.class);
    publish("CNS-DP");
    String grantBody = grantBody("cust-smoke", "CNS-DP", null);
    MvcResult granted =
        mvc.perform(
                post("/internal/v1/consent/grants")
                    .headers(headers("grant-1"))
                    .content(grantBody)
                    .with(this::ip))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.challengeIssued").value(true))
            .andExpect(jsonPath("$.challenge.challengeId").exists())
            .andReturn();
    String challengeId = json(granted, "$.challenge.challengeId");
    String otp = dispatch.codes.get(challengeId);
    MvcResult verified =
        mvc.perform(
                post("/internal/v1/consent/challenges/" + challengeId + "/verify")
                    .headers(headers("verify-1"))
                    .content("{\"otp\":\"" + otp + "\",\"actorRole\":\"CUSTOMER_DEVICE\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.recordedState").value("GRANTED"))
            .andExpect(jsonPath("$.acknowledgementMethod").value("OTP_SMS_CUSTOMER_DEVICE"))
            .andExpect(jsonPath("$.recordVersion").value(1))
            .andReturn();
    String evidenceId = json(verified, "$.evidenceId");
    String root = json(verified, "$.rootConsentId");
    mvc.perform(
            post("/internal/v1/consent/grants")
                .headers(headers("grant-reuse"))
                .content(grantBody)
                .with(this::ip))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.challengeIssued").value(false))
        .andExpect(jsonPath("$.current.evidenceId").value(evidenceId));
    assertThat(verified.getResponse().getContentAsString())
        .doesNotContain(CIF, otp, IP, CONTACT, "dist-local");
    byte[] stored =
        new JdbcTemplate(dataSource)
            .queryForObject(
                "select cif_enc from consent_evidence where evidence_id = ?",
                byte[].class,
                evidenceId);
    assertThat(new String(stored)).doesNotContain(CIF);
    assertThat(
            new JdbcTemplate(dataSource)
                .queryForObject(
                    "select source_ip from consent_evidence where evidence_id = ?",
                    String.class,
                    evidenceId))
        .isEqualTo(IP);

    mvc.perform(
            post("/internal/v1/consent/challenges/" + challengeId + "/verify")
                .headers(headers("verify-1"))
                .content("{\"otp\":\"" + otp + "\",\"actorRole\":\"CUSTOMER_DEVICE\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.evidenceId").value(evidenceId));

    mvc.perform(
            post("/internal/v1/consent/evaluations")
                .header("X-Actor-Id", "rm-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(evaluateBody("cust-smoke", "CNS-DP", null, root)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.allowed").value(true))
        .andExpect(jsonPath("$.effect").value("PROCEED"));

    MvcResult withdrawal =
        mvc.perform(
                post("/internal/v1/consent/grants/" + root + "/withdrawals")
                    .headers(headers("withdraw-1"))
                    .content(
                        "{\"registeredContactRef\":\""
                            + CONTACT
                            + "\",\"reasonCode\":\"USER_REQUEST\",\"proposalAlreadySubmitted\":false}")
                    .with(this::ip))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.effect").value("HALT_JOURNEY"))
            .andReturn();
    String withdrawChallenge = json(withdrawal, "$.challenge.challengeId");
    mvc.perform(
            post("/internal/v1/consent/challenges/" + withdrawChallenge + "/verify")
                .headers(headers("verify-2"))
                .content(
                    "{\"otp\":\""
                        + dispatch.codes.get(withdrawChallenge)
                        + "\",\"actorRole\":\"CUSTOMER_DEVICE\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.recordedState").value("WITHDRAWN"))
        .andExpect(jsonPath("$.recordVersion").value(2));

    mvc.perform(get("/internal/v1/consent/grants/" + root).header("X-Actor-Id", "rm-1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.records[0].recordedState").value("GRANTED"))
        .andExpect(jsonPath("$.records[0].effectiveStatus").value("SUPERSEDED"))
        .andExpect(jsonPath("$.records[1].recordedState").value("WITHDRAWN"))
        .andExpect(jsonPath("$.records[1].effectiveStatus").value("WITHDRAWN"));

    mvc.perform(
            post("/internal/v1/consent/evaluations")
                .header("X-Actor-Id", "rm-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(evaluateBody("cust-smoke", "CNS-DP", null, root)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("JOURNEY_HALTED_CONSENT_WITHDRAWN"));

    MvcResult regrant =
        mvc.perform(
                post("/internal/v1/consent/grants")
                    .headers(headers("grant-2"))
                    .content(grantBody)
                    .with(this::ip))
            .andExpect(status().isCreated())
            .andReturn();
    String reChallenge = json(regrant, "$.challenge.challengeId");
    mvc.perform(
            post("/internal/v1/consent/challenges/" + reChallenge + "/verify")
                .headers(headers("verify-3"))
                .content(
                    "{\"otp\":\""
                        + dispatch.codes.get(reChallenge)
                        + "\",\"actorRole\":\"CUSTOMER_DEVICE\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.recordVersion").value(3))
        .andExpect(jsonPath("$.rootConsentId").value(root));

    mvc.perform(
            post("/internal/v1/consent/grants/" + root + "/retention")
                .headers(headers("retain-1"))
                .content("{\"terminalAt\":\"2024-02-29T00:00:00Z\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.retainUntil").value("2031-02-28T00:00:00Z"));

    assertThat(logs.list).isNotEmpty();
    assertThat(logs.list)
        .allSatisfy(
            event -> {
              assertThat(event.getFormattedMessage()).doesNotContain(CIF, IP, CONTACT);
              assertThat(event.getFormattedMessage()).startsWith("otp dispatched challenge=");
            });
  }

  @Test
  void refusals_coverBadInputIdempotencyDeviceIsolationAndTheIssueLock() throws Exception {
    publish("CNS-DP");
    String body = grantBody("cust-edge", "CNS-DP", null);
    mvc.perform(
            post("/internal/v1/consent/statements")
                .header("X-Actor-Id", "rm-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(statement("CNS-DP", "Edge notice.")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("MISSING_IDEMPOTENCY_KEY"));
    MvcResult created =
        mvc.perform(
                post("/internal/v1/consent/grants")
                    .headers(headers("edge-grant"))
                    .content(body)
                    .with(this::ip))
            .andExpect(status().isCreated())
            .andReturn();
    String challengeId = json(created, "$.challenge.challengeId");
    mvc.perform(
            post("/internal/v1/consent/grants")
                .headers(headers("edge-grant"))
                .content(body)
                .with(this::ip))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.challenge.challengeId").value(challengeId));
    String conflict = body.replace("cust-edge", "cust-other");
    mvc.perform(
            post("/internal/v1/consent/grants")
                .headers(headers("edge-grant"))
                .content(conflict)
                .with(this::ip))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("IDEMPOTENCY_CONFLICT"));
    mvc.perform(
            post("/internal/v1/consent/grants")
                .headers(headers("edge-bad"))
                .content(body.replace(CONTACT, "9876543210"))
                .with(this::ip))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    mvc.perform(
            post("/internal/v1/consent/grants")
                .headers(headers("edge-diy"))
                .content(body.replace("ASSISTED", "DIY"))
                .with(this::ip))
        .andExpect(status().isUnprocessableEntity());
    mvc.perform(
            post("/internal/v1/consent/grants")
                .headers(headers("edge-rdr"))
                .content(body.replace("CNS-DP", "CNS-RDR"))
                .with(this::ip))
        .andExpect(status().isUnprocessableEntity());
    mvc.perform(
            post("/internal/v1/consent/grants")
                .headers(headers("edge-extra"))
                .content(body.replace("}", ",\"extra\":\"no\"}"))
                .with(this::ip))
        .andExpect(status().isUnprocessableEntity());
    mvc.perform(
            post("/internal/v1/consent/grants")
                .header("Idempotency-Key", "edge-rm")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

    mvc.perform(
            post("/internal/v1/consent/challenges/" + challengeId + "/verify")
                .headers(headers("edge-rm-verify"))
                .content("{\"otp\":\"000000\",\"actorRole\":\"RM\"}"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("CONSENT_DEVICE_ISOLATION"));
    for (int attempt = 1; attempt <= 2; attempt++) {
      mvc.perform(
              post("/internal/v1/consent/challenges/" + challengeId + "/verify")
                  .headers(headers("edge-miss-" + attempt))
                  .content("{\"otp\":\"000000\",\"actorRole\":\"CUSTOMER_DEVICE\"}"))
          .andExpect(status().isConflict())
          .andExpect(jsonPath("$.code").value("CONSENT_OTP_NOT_VERIFIED"));
    }
    mvc.perform(
            post("/internal/v1/consent/challenges/" + challengeId + "/verify")
                .headers(headers("edge-miss-3"))
                .content("{\"otp\":\"000000\",\"actorRole\":\"CUSTOMER_DEVICE\"}"))
        .andExpect(status().isTooManyRequests())
        .andExpect(jsonPath("$.code").value("OTP_ATTEMPTS_EXCEEDED"));
    mvc.perform(get("/internal/v1/consent/info"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("ACTIVE"));
  }

  @Test
  void dispatchFailure_rollsTheChallengeBack() throws Exception {
    publish("CNS-DP");
    int before =
        new JdbcTemplate(dataSource)
            .queryForObject("select count(*) from otp_challenge", Integer.class);
    dispatch.failNext();
    mvc.perform(
            post("/internal/v1/consent/grants")
                .headers(headers("roll-1"))
                .content(grantBody("cust-roll", "CNS-DP", null))
                .with(this::ip))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.code").value("CONSENT_EVIDENCE_WRITE_FAILED"));
    int after =
        new JdbcTemplate(dataSource)
            .queryForObject("select count(*) from otp_challenge", Integer.class);
    assertThat(after).isEqualTo(before);
  }

  @Test
  void communicationsAbsence_doesNotBlockAndAForeignCifIsASecurityEvent() throws Exception {
    ListAppender<ILoggingEvent> security = attach(LoggingSecurityEventSink.class);
    publish("CNS-COM");
    mvc.perform(
            post("/internal/v1/consent/evaluations")
                .header("X-Actor-Id", "rm-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(evaluateBody("cust-com", "CNS-COM", null, null)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.allowed").value(false))
        .andExpect(jsonPath("$.journeyBlocked").value(false))
        .andExpect(jsonPath("$.effect").value("SUPPRESS_OPTIONAL_COMMUNICATIONS"));

    publish("CNS-SHR");
    String shr = grantBody("cust-sec", "CNS-SHR", "ins-a");
    MvcResult open =
        mvc.perform(
                post("/internal/v1/consent/grants")
                    .headers(headers("sec-grant"))
                    .content(shr)
                    .with(this::ip))
            .andExpect(status().isCreated())
            .andReturn();
    String challengeId = json(open, "$.challenge.challengeId");
    MvcResult verified =
        mvc.perform(
                post("/internal/v1/consent/challenges/" + challengeId + "/verify")
                    .headers(headers("sec-verify"))
                    .content(
                        "{\"otp\":\""
                            + dispatch.codes.get(challengeId)
                            + "\",\"actorRole\":\"CUSTOMER_DEVICE\"}"))
            .andExpect(status().isCreated())
            .andReturn();
    String root = json(verified, "$.rootConsentId");
    MvcResult mismatch =
        mvc.perform(
                post("/internal/v1/consent/evaluations")
                    .header("X-Actor-Id", "rm-9")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        evaluateBody("cust-other", "CNS-SHR", "ins-a", root)
                            .replace(CIF, "CIF-OTHER")))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("CONSENT_SUBJECT_MISMATCH"))
            .andReturn();
    assertThat(mismatch.getResponse().getContentAsString()).doesNotContain(CIF, "CIF-OTHER");
    assertThat(security.list).isNotEmpty();
    assertThat(security.list.get(0).getFormattedMessage())
        .isEqualTo(
            "security_event=CONSENT_SUBJECT_MISMATCH actor=rm-9 journey=journey-1 consent=" + root)
        .doesNotContain(CIF);
  }

  private void publish(String id) throws Exception {
    String text =
        switch (id) {
          case "CNS-COM" -> "Optional servicing messages.";
          case "CNS-SHR" -> "Share data with one named insurer.";
          default -> "We process data to prefill the proposal.";
        };
    mvc.perform(
            post("/internal/v1/consent/statements")
                .headers(headers("pub-" + id))
                .content(statement(id, text)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.textHash").exists());
  }

  private static String statement(String id, String text) {
    return "{\"statementId\":\""
        + id
        + "\",\"version\":\"1.0\",\"text\":\""
        + text
        + "\",\"language\":\"en-IN\",\"changeClass\":\"MINOR\",\"classifiedBy\":\"legal\"}";
  }

  private static String grantBody(String customerId, String code, String counterparty) {
    String party = counterparty == null ? "" : ",\"counterpartyId\":\"" + counterparty + "\"";
    return "{\"leadId\":\"lead-1\",\"journeyId\":\"journey-1\",\"customerId\":\""
        + customerId
        + "\",\"customerCif\":\""
        + CIF
        + "\",\"consentCode\":\""
        + code
        + "\",\"statementId\":\""
        + code
        + "\",\"statementVersion\":\"1.0\",\"language\":\"en-IN\",\"channel\":\"ASSISTED\","
        + "\"registeredContactRef\":\""
        + CONTACT
        + "\",\"capturedByActorId\":\"rm-1\",\"capturedByAgentId\":\"agent-1\",\"lob\":\"LIFE\","
        + "\"configVersionRef\":\"cfg-1\""
        + party
        + "}";
  }

  private static String evaluateBody(
      String customerId, String code, String counterparty, String presented) {
    String party = counterparty == null ? "" : ",\"counterpartyId\":\"" + counterparty + "\"";
    String consent = presented == null ? "" : ",\"presentedConsentId\":\"" + presented + "\"";
    return "{\"customerId\":\""
        + customerId
        + "\",\"consentCode\":\""
        + code
        + "\",\"currentStatementVersion\":\"1.0\",\"customerCif\":\""
        + CIF
        + "\",\"journeyId\":\"journey-1\""
        + party
        + consent
        + "}";
  }

  private static org.springframework.http.HttpHeaders headers(String key) {
    org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
    headers.add("X-Actor-Id", "rm-1");
    headers.add("Idempotency-Key", key);
    headers.setContentType(MediaType.APPLICATION_JSON);
    return headers;
  }

  private org.springframework.mock.web.MockHttpServletRequest ip(
      org.springframework.mock.web.MockHttpServletRequest request) {
    request.setRemoteAddr(IP);
    return request;
  }

  private static String json(MvcResult result, String path) throws Exception {
    return com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), path);
  }

  private static ListAppender<ILoggingEvent> attach(Class<?> type) {
    Logger logger = (Logger) LoggerFactory.getLogger(type);
    ListAppender<ILoggingEvent> appender = new ListAppender<>();
    appender.start();
    logger.addAppender(appender);
    return appender;
  }

  @TestConfiguration
  static class DispatchConfig {
    @Bean
    @Primary
    CapturingOtpDispatch capturingOtpDispatch() {
      return new CapturingOtpDispatch();
    }
  }

  static final class CapturingOtpDispatch implements OtpDispatch {
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(CapturingOtpDispatch.class);
    final Map<String, String> codes = new ConcurrentHashMap<>();
    private final AtomicBoolean fail = new AtomicBoolean();

    void failNext() {
      fail.set(true);
    }

    void reset() {
      fail.set(false);
    }

    @Override
    public void send(String challengeId, byte[] registeredContactRefEnc, String otp) {
      Objects.requireNonNull(registeredContactRefEnc, "contact reference");
      Objects.requireNonNull(otp, "otp");
      if (fail.compareAndSet(true, false)) {
        throw new IllegalStateException("dispatch unavailable");
      }
      codes.put(challengeId, otp);
      log.info("otp dispatched challenge={}", challengeId);
    }
  }
}
