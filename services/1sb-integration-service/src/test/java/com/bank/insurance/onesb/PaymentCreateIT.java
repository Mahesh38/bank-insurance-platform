package com.bank.insurance.onesb;

import com.bank.common.audit.AuditActions;
import com.bank.common.audit.AuditEvent;
import com.bank.common.audit.AuditEventPublisher;
import com.bank.common.error.ErrorCodes;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * FUNC-007 IT: {@code POST /v1/payments} — happy path, non-payable job, non-HTTPS upstream URL,
 * missing Idempotency-Key.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Tag("FUNC-007")
@Tag("integration")
class PaymentCreateIT {

    private static final String PAYMENT_URL_PATH = "/v1/payment/url";

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
    }

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuditEventPublisher auditEventPublisher;

    @BeforeEach
    void resetStubs() {
        ONESB.resetAll();
        PERSISTENCE.resetAll();
    }

    @Test
    void ac1_happyPath_returns201WithHttpsPaymentUrlAndAuditsRetrieved() throws Exception {
        ONESB.stubFor(post(urlEqualTo(PAYMENT_URL_PATH))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"paymentUrl\":\"https://pay.example.com/abc\",\"transactionId\":\"TXN-1\"}")));
        stubCreateSession("sess-1");

        mockMvc.perform(MockMvcRequestBuilders.post("/v1/payments")
                        .header("Idempotency-Key", "idem-" + UUID.randomUUID())
                        .header("X-Actor-Id", "rm-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody(null)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentSessionId", is("sess-1")))
                .andExpect(jsonPath("$.paymentUrl", is("https://pay.example.com/abc")))
                .andExpect(jsonPath("$.status", is("AWAITING_PAYMENT")));

        ONESB.verify(exactly(1), postRequestedFor(urlEqualTo(PAYMENT_URL_PATH))
                .withRequestBody(matchingJsonPath("$.Distributor.distributorID",
                        com.github.tomakehurst.wiremock.client.WireMock.equalTo("TEST_DIST")))
                .withRequestBody(matchingJsonPath("$.Distributor.agentID",
                        com.github.tomakehurst.wiremock.client.WireMock.equalTo("AGT-1"))));

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditEventPublisher, atLeastOnce()).publish(captor.capture());
        assertThat(captor.getAllValues())
                .anyMatch(e -> AuditActions.PAYMENT_URL_RETRIEVED.equals(e.getAction()));
    }

    @Test
    void ac2_jobNotPayable_returns409_andNeverCallsOneSb() throws Exception {
        PERSISTENCE.stubFor(get(urlPathMatching("/internal/v1/jobs/job-pending"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "jobId": "job-pending",
                                  "jobType": "PROPOSAL",
                                  "lob": "TERM",
                                  "status": "RUNNING",
                                  "createdAt": "2026-08-04T10:00:00Z",
                                  "updatedAt": "2026-08-04T10:00:00Z",
                                  "version": 0,
                                  "createdByActor": "actor"
                                }
                                """)));
        PERSISTENCE.stubFor(get(urlPathMatching("/internal/v1/jobs/job-pending/offers"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("[]")));

        mockMvc.perform(MockMvcRequestBuilders.post("/v1/payments")
                        .header("Idempotency-Key", "idem-" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody("job-pending")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is(ErrorCodes.PROPOSAL_NOT_PAYABLE)));

        ONESB.verify(0, postRequestedFor(urlEqualTo(PAYMENT_URL_PATH)));
    }

    @Test
    void ac3_nonHttpsPaymentUrl_returns502_upstreamBadResponse() throws Exception {
        ONESB.stubFor(post(urlEqualTo(PAYMENT_URL_PATH))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"paymentUrl\":\"http://insecure.example.com/pay\"}")));

        mockMvc.perform(MockMvcRequestBuilders.post("/v1/payments")
                        .header("Idempotency-Key", "idem-" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody(null)))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code", is(ErrorCodes.UPSTREAM_BAD_RESPONSE)))
                .andExpect(jsonPath("$.retryable", is(true)));
    }

    @Test
    void r_missingIdempotencyKey_returns400() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody(null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is(ErrorCodes.MISSING_IDEMPOTENCY_KEY)));

        ONESB.verify(0, postRequestedFor(urlEqualTo(PAYMENT_URL_PATH)));
    }

    private static void stubCreateSession(String sessionId) {
        PERSISTENCE.stubFor(post(urlEqualTo("/internal/v1/payment-sessions"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "sessionId": "%s",
                                  "jobId": null,
                                  "applicationNumber": "APP-123",
                                  "lob": "TERM",
                                  "paymentUrl": "https://pay.example.com/abc",
                                  "redirectUrl": "https://bank.com/return",
                                  "status": "AWAITING_PAYMENT",
                                  "externalTxnId": "TXN-1",
                                  "createdAt": "2026-08-04T10:00:00Z",
                                  "expiresAt": "2026-08-04T11:00:00Z",
                                  "updatedAt": "2026-08-04T10:00:00Z",
                                  "createdByActor": "rm-1"
                                }
                                """.formatted(sessionId))));
    }

    private static String validBody(String jobId) {
        String jobIdField = jobId != null ? "\"jobId\": \"" + jobId + "\"," : "";
        return """
                {
                  "lob": "TERM",
                  "applicationNumber": "APP-123",
                  %s
                  "insurerCode": "HDFC",
                  "productCode": "T1",
                  "redirectUrl": "https://bank.com/return",
                  "journeyId": "j-1",
                  "payer": {
                    "firstName": "Anil",
                    "lastName": "Kumar",
                    "mobileNumber": "9876543210",
                    "email": "anil@example.com"
                  },
                  "amountToBePaid": 12300,
                  "premiumFrequency": "YEARLY",
                  "distribution": { "rmEmployeeId": "E123", "agentId": "AGT-1" }
                }
                """.formatted(jobIdField);
    }
}
