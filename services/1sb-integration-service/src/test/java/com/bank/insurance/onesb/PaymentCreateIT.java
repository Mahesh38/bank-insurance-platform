package com.bank.insurance.onesb;

import com.bank.common.audit.AuditActions;
import com.bank.common.audit.AuditEvent;
import com.bank.common.audit.AuditEventPublisher;
import com.bank.common.audit.AuditOutcomes;
import com.bank.common.error.ErrorCodes;
import com.bank.insurance.onesb.application.PaymentService;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * FUNC-007 IT: {@code POST /v1/payments} — HTTPS URL, non-payable 409, audit, idempotency, no URL in logs.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Tag("FUNC-007")
@Tag("integration")
class PaymentCreateIT {

    private static final String PAYMENT_URL_PATH = "/v1/payment/url";
    private static final String SECRET_URL =
            "https://payments.wiremock.test/checkout?token=NEVER-LOG-THIS-TOKEN";

    private static final WireMockServer ONESB = new WireMockServer(wireMockConfig().dynamicPort());
    private static final WireMockServer PERSISTENCE = new WireMockServer(wireMockConfig().dynamicPort());

    static {
        ONESB.start();
        PERSISTENCE.start();
    }

    @AfterAll
    static void stopWireMocks() {
        ONESB.stop();
        PERSISTENCE.stop();
    }

    @DynamicPropertySource
    static void bindWireMockBaseUrls(DynamicPropertyRegistry registry) {
        registry.add("onesb.client.base-url", ONESB::baseUrl);
        registry.add("bank.persistence.base-url", PERSISTENCE::baseUrl);
        registry.add("insurance.payments.default-redirect-url",
                () -> "https://bank.test/insurance/payment-complete");
    }

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuditEventPublisher auditEventPublisher;

    private ListAppender<ILoggingEvent> paymentLogAppender;

    @BeforeEach
    void resetStubs() {
        ONESB.resetAll();
        PERSISTENCE.resetAll();

        Logger logger = (Logger) LoggerFactory.getLogger(PaymentService.class);
        paymentLogAppender = new ListAppender<>();
        paymentLogAppender.start();
        logger.addAppender(paymentLogAppender);
        logger.setLevel(ch.qos.logback.classic.Level.INFO);
    }

    @Test
    void ac1_payableApplication_returnsHttpsPaymentUrlExpiryAndRef() throws Exception {
        String sessionId = "pay-" + UUID.randomUUID();
        stubOneSbPayment(sessionId, SECRET_URL, "2026-07-30T21:00:00Z");
        stubPersistencePaymentSession(sessionId);

        mockMvc.perform(MockMvcRequestBuilders.post("/v1/payments")
                        .header("Idempotency-Key", "idem-pay-ac1-" + UUID.randomUUID())
                        .header("X-Actor-Id", "rm-pay-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "applicationNumber": "APP-PAY-1",
                                  "lob": "TERM",
                                  "journeyId": "j-pay-1"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentRef", is(sessionId)))
                .andExpect(jsonPath("$.paymentUrl", is(SECRET_URL)))
                .andExpect(jsonPath("$.expiresAt", is("2026-07-30T21:00:00Z")));

        ONESB.verify(exactly(1), postRequestedFor(urlEqualTo(PAYMENT_URL_PATH)));
        PERSISTENCE.verify(exactly(1),
                postRequestedFor(urlEqualTo("/internal/v1/payment-sessions")));
    }

    @Test
    void ac2_paymentUrlNeverWrittenToLogs_refOnly() throws Exception {
        String sessionId = "pay-log-" + UUID.randomUUID();
        stubOneSbPayment(sessionId, SECRET_URL, "2026-07-30T22:00:00Z");
        stubPersistencePaymentSession(sessionId);

        mockMvc.perform(MockMvcRequestBuilders.post("/v1/payments")
                        .header("Idempotency-Key", "idem-pay-ac2-" + UUID.randomUUID())
                        .header("X-Actor-Id", "rm-pay-2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "applicationNumber": "APP-PAY-LOG" }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentRef", is(sessionId)));

        assertThat(paymentLogAppender.list)
                .extracting(ILoggingEvent::getFormattedMessage)
                .anyMatch(m -> m.contains(sessionId) && m.contains("paymentRef"))
                .noneMatch(m -> m.contains(SECRET_URL) || m.contains("NEVER-LOG-THIS-TOKEN"));
    }

    @Test
    void ac3_nonPayableProposalJob_returns409() throws Exception {
        String jobId = "job-pending-" + UUID.randomUUID();
        PERSISTENCE.stubFor(get(urlEqualTo("/internal/v1/jobs/" + jobId))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "jobId": "%s",
                                  "jobType": "PROPOSAL",
                                  "lob": "TERM",
                                  "status": "PENDING",
                                  "journeyId": "j-1",
                                  "createdAt": "2026-07-30T12:00:00Z",
                                  "updatedAt": "2026-07-30T12:00:00Z",
                                  "version": 0,
                                  "createdByActor": "test"
                                }
                                """.formatted(jobId))));
        PERSISTENCE.stubFor(get(urlEqualTo("/internal/v1/jobs/" + jobId + "/offers"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("[]")));

        mockMvc.perform(MockMvcRequestBuilders.post("/v1/payments")
                        .header("Idempotency-Key", "idem-pay-ac3-" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "applicationNumber": "APP-PENDING",
                                  "proposalJobId": "%s"
                                }
                                """.formatted(jobId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is(ErrorCodes.PROPOSAL_NOT_PAYABLE)));

        ONESB.verify(0, postRequestedFor(urlEqualTo(PAYMENT_URL_PATH)));
    }

    @Test
    void ac4_auditsPaymentUrlRetrieved() throws Exception {
        String sessionId = "pay-audit-" + UUID.randomUUID();
        stubOneSbPayment(sessionId, SECRET_URL, "2026-07-30T23:00:00Z");
        stubPersistencePaymentSession(sessionId);

        mockMvc.perform(MockMvcRequestBuilders.post("/v1/payments")
                        .header("Idempotency-Key", "idem-pay-ac4-" + UUID.randomUUID())
                        .header("X-Actor-Id", "rm-audit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "applicationNumber": "APP-AUDIT" }
                                """))
                .andExpect(status().isCreated());

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditEventPublisher, atLeastOnce()).publish(captor.capture());
        assertThat(captor.getAllValues())
                .anyMatch(e -> AuditActions.PAYMENT_URL_RETRIEVED.equals(e.getAction())
                        && AuditOutcomes.SUCCESS.equals(e.getOutcome())
                        && sessionId.equals(e.getResourceId()));
        assertThat(captor.getAllValues().stream()
                .filter(e -> AuditActions.PAYMENT_URL_RETRIEVED.equals(e.getAction()))
                .flatMap(e -> e.getMetadata().values().stream())
                .map(Object::toString))
                .noneMatch(v -> v.contains(SECRET_URL) || v.contains("NEVER-LOG-THIS-TOKEN"));
    }

    @Test
    void r6_missingIdempotencyKey_returns400() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "applicationNumber": "APP-NO-IDEM" }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is(ErrorCodes.MISSING_IDEMPOTENCY_KEY)));

        ONESB.verify(0, postRequestedFor(urlEqualTo(PAYMENT_URL_PATH)));
    }

    @Test
    void ac1b_completedProposalJob_succeeds() throws Exception {
        String jobId = "job-done-" + UUID.randomUUID();
        String sessionId = "pay-done-" + UUID.randomUUID();
        PERSISTENCE.stubFor(get(urlEqualTo("/internal/v1/jobs/" + jobId))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "jobId": "%s",
                                  "jobType": "PROPOSAL",
                                  "lob": "TERM",
                                  "status": "COMPLETED",
                                  "applicationNumber": "APP-DONE",
                                  "journeyId": "j-done",
                                  "createdAt": "2026-07-30T12:00:00Z",
                                  "updatedAt": "2026-07-30T12:00:00Z",
                                  "completedAt": "2026-07-30T12:05:00Z",
                                  "version": 1,
                                  "createdByActor": "test"
                                }
                                """.formatted(jobId))));
        PERSISTENCE.stubFor(get(urlEqualTo("/internal/v1/jobs/" + jobId + "/offers"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("[]")));
        stubOneSbPayment(sessionId, SECRET_URL, "2026-07-31T00:00:00Z");
        stubPersistencePaymentSession(sessionId);

        mockMvc.perform(MockMvcRequestBuilders.post("/v1/payments")
                        .header("Idempotency-Key", "idem-pay-done-" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "applicationNumber": "APP-DONE",
                                  "proposalJobId": "%s"
                                }
                                """.formatted(jobId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentRef", is(sessionId)))
                .andExpect(jsonPath("$.paymentUrl", startsWith("https://")));
    }

    private static void stubOneSbPayment(String sessionId, String paymentUrl, String expiresAt) {
        ONESB.stubFor(post(urlEqualTo(PAYMENT_URL_PATH))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "transactionId": "%s",
                                  "PaymentDetails": {
                                    "paymentUrl": "%s"
                                  },
                                  "expiresAt": "%s"
                                }
                                """.formatted(sessionId, paymentUrl, expiresAt))));
    }

    private static void stubPersistencePaymentSession(String sessionId) {
        PERSISTENCE.stubFor(post(urlEqualTo("/internal/v1/payment-sessions"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "sessionId": "%s",
                                  "applicationNumber": "APP",
                                  "lob": "TERM",
                                  "paymentUrl": "%s",
                                  "redirectUrl": "https://bank.test/insurance/payment-complete",
                                  "status": "AWAITING_PAYMENT",
                                  "createdAt": "2026-07-30T12:00:00Z",
                                  "expiresAt": "2026-07-30T21:00:00Z",
                                  "updatedAt": "2026-07-30T12:00:00Z",
                                  "createdByActor": "test"
                                }
                                """.formatted(sessionId, SECRET_URL))));
    }
}
