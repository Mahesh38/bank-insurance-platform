package com.bank.insurance.onesb.e2e;

import com.bank.common.audit.AuditActions;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.List;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.patch;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * RISK-012 — proves audit events reach the durable store on a real request, through the real
 * wiring.
 *
 * <p>{@code HttpAuditEventStoreAdapterTest} proves the adapter posts correctly when called
 * directly. That is not the same claim. This one boots the application with
 * {@code insurance.audit.sinks=LOG,PERSISTENCE}, drives an actual quote through the controller,
 * and asserts rows arrive at {@code POST /internal/v1/audit-events} — which is what Compliance
 * is being asked to rely on.
 *
 * <p>Before this existed, a green build was compatible with the audit trail going nowhere.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "insurance.audit.sinks=LOG,PERSISTENCE")
@Tag("integration")
@Tag("COMP-002")
class AuditPersistenceIT {

    private static final String AUDIT_PATH = "/internal/v1/audit-events";
    private static final String ONESB_QUOTE = "/insurance/lifeterm/v1/quote";
    private static final String ONESB_QUOTE_POLL = "/insurance/lifeterm/v1/quote/poll/";

    private static final WireMockServer ONESB = new WireMockServer(wireMockConfig().dynamicPort());
    private static final WireMockServer PERSISTENCE = new WireMockServer(wireMockConfig().dynamicPort());

    static {
        ONESB.start();
        PERSISTENCE.start();
    }

    @AfterAll
    static void stop() {
        ONESB.stop();
        PERSISTENCE.stop();
    }

    @DynamicPropertySource
    static void bindWireMocks(DynamicPropertyRegistry registry) {
        registry.add("onesb.client.base-url", ONESB::baseUrl);
        registry.add("bank.persistence.base-url", PERSISTENCE::baseUrl);
        registry.add("onesb.poll.base-delay-ms", () -> "1");
        registry.add("onesb.poll.max-delay-ms", () -> "5");
        registry.add("onesb.poll.max-attempts", () -> "3");
    }

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void resetStubs() {
        ONESB.resetAll();
        PERSISTENCE.resetAll();

        PERSISTENCE.stubFor(post(urlEqualTo(AUDIT_PATH))
                .willReturn(created("{\"eventId\":\"evt-1\"}")));
        PERSISTENCE.stubFor(post(urlEqualTo("/internal/v1/jobs"))
                .willReturn(created("""
                        {"jobId":"job-audit-1","jobType":"QUOTE","lob":"TERM","status":"PENDING",
                         "createdAt":"2026-08-14T10:00:00Z","updatedAt":"2026-08-14T10:00:00Z",
                         "version":0,"createdByActor":"rm-audit"}
                        """)));
        PERSISTENCE.stubFor(patch(urlPathMatching("/internal/v1/jobs/.*/status"))
                .willReturn(ok("{\"status\":\"RUNNING\"}")));
        PERSISTENCE.stubFor(post(urlPathMatching("/internal/v1/jobs/.*/poll-attempts"))
                .willReturn(created("{\"attemptId\":1}")));
        PERSISTENCE.stubFor(post(urlPathMatching("/internal/v1/jobs/.*/offers"))
                .willReturn(created("{\"offerId\":\"o1\"}")));
        PERSISTENCE.stubFor(post(urlEqualTo("/internal/v1/raw-payloads"))
                .willReturn(created("{\"payloadId\":\"raw-1\"}")));

        ONESB.stubFor(post(urlEqualTo(ONESB_QUOTE))
                .willReturn(ok("{\"reqId\":\"REQ-AUDIT\",\"data\":{}}")));
        ONESB.stubFor(get(urlPathMatching(ONESB_QUOTE_POLL + ".*"))
                .willReturn(ok("""
                        {"data":{"isPollComplete":true,"quote":[{
                          "offerId":"off-1","insurerCode":"HDFC","productCode":"T1",
                          "productName":"Term","premiumAmount":4200,"sumAssured":5000000}]}}
                        """)));
    }

    @Test
    void aQuoteRequestPersistsItsAuditTrail() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/v1/quotes")
                        .header("Idempotency-Key", "audit-" + UUID.randomUUID())
                        .header("X-Actor-Id", "rm-audit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "lob": "TERM",
                                  "journeyId": "journey-audit-1",
                                  "sumAssured": 5000000,
                                  "members": [{ "dob": "1990-01-15", "gender": "M" }],
                                  "distribution": { "agentId": "109337" }
                                }
                                """))
                .andExpect(status().isAccepted());

        // Polling runs on pollingExecutor, so later events arrive after the response.
        await().atMost(java.time.Duration.ofSeconds(10)).untilAsserted(() ->
                assertThat(auditBodies()).isNotEmpty());

        List<String> bodies = auditBodies();

        assertThat(bodies)
                .as("the quote submission must be recorded against a real actor")
                .anyMatch(b -> b.contains(AuditActions.QUOTE_CREATED) && b.contains("rm-audit"));
        assertThat(bodies)
                .as("every outbound 1SB call must be recorded")
                .anyMatch(b -> b.contains(AuditActions.ONESB_OUTBOUND_CALL));
        assertThat(bodies)
                .as("the journey id must be carried so a trail can be reconstructed")
                .anyMatch(b -> b.contains("journey-audit-1"));
    }

    /**
     * The trade-off Compliance is asked to accept: evidence capture degrades, the customer's
     * transaction does not.
     */
    @Test
    void aPersistenceOutageDoesNotFailTheCustomersQuote() throws Exception {
        PERSISTENCE.stubFor(post(urlEqualTo(AUDIT_PATH))
                .willReturn(aResponse().withStatus(503)));

        mockMvc.perform(MockMvcRequestBuilders.post("/v1/quotes")
                        .header("Idempotency-Key", "audit-outage-" + UUID.randomUUID())
                        .header("X-Actor-Id", "rm-audit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "lob": "TERM",
                                  "journeyId": "journey-audit-2",
                                  "sumAssured": 5000000,
                                  "members": [{ "dob": "1990-01-15", "gender": "M" }],
                                  "distribution": { "agentId": "109337" }
                                }
                                """))
                .andExpect(status().isAccepted());
    }

    /** No PII may reach the durable store, for the same reason none may reach a log. */
    @Test
    void thePersistedTrailCarriesNoPlaintextPii() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/v1/quotes")
                        .header("Idempotency-Key", "audit-pii-" + UUID.randomUUID())
                        .header("X-Actor-Id", "rm-audit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "lob": "TERM",
                                  "journeyId": "journey-audit-3",
                                  "sumAssured": 5000000,
                                  "members": [{ "dob": "1990-01-15", "gender": "M" }],
                                  "distribution": { "agentId": "109337" }
                                }
                                """))
                .andExpect(status().isAccepted());

        await().atMost(java.time.Duration.ofSeconds(10)).untilAsserted(() ->
                assertThat(auditBodies()).isNotEmpty());

        assertThat(auditBodies())
                .as("the member's date of birth must never appear in the audit trail")
                .noneMatch(b -> b.contains("1990-01-15"));
    }

    private static List<String> auditBodies() {
        return PERSISTENCE.findAll(postRequestedFor(urlEqualTo(AUDIT_PATH)))
                .stream().map(LoggedRequest::getBodyAsString).toList();
    }

    private static com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder ok(String body) {
        return aResponse().withStatus(200)
                .withHeader("Content-Type", "application/json").withBody(body);
    }

    private static com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder created(String body) {
        return aResponse().withStatus(201)
                .withHeader("Content-Type", "application/json").withBody(body);
    }
}
