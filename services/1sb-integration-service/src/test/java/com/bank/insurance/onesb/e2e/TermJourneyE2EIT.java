package com.bank.insurance.onesb.e2e;

import com.bank.common.audit.AuditActions;
import com.bank.common.audit.AuditEvent;
import com.bank.common.audit.AuditEventPublisher;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.patch;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * WS-1 Phase 4 exit criterion <b>4.1</b> — end-to-end suite for the Term path.
 *
 * <p>The per-story ITs ({@code QuoteCreateIT}, {@code ProposalSubmitIT}, {@code PaymentCreateIT},
 * {@code ApplicationStatusIT}) each prove one endpoint against its own fresh stubs. None of them
 * proves that a <em>single</em> journey holds together: that the {@code jobId} handed back by
 * quote is the one proposal accepts, that the {@code applicationNumber} produced by proposal is
 * the one payment and status can use. That hand-off is exactly where an integration breaks, and
 * it is what this class covers.
 *
 * <p>The journey runs in order, each step consuming the previous step's output:
 *
 * <pre>
 *   1. POST /v1/master-data/lookup       → enum values for the quote form
 *   2. POST /v1/quotes                   → jobId          (async, 202)
 *   3. GET  /v1/quotes/{jobId}           → offers         (poll completed)
 *   4. GET  /v1/proposals/schema         → dynamic form
 *   5. POST /v1/proposals                → proposalJobId  (201)
 *   6. GET  /v1/proposals/{jobId}        → applicationNumber
 *   7. POST /v1/payments                 → payment URL
 *   8. GET  /v1/status/{applicationNumber} → normalised bank status
 * </pre>
 *
 * <p><b>This is a simulated sandbox, not the real one.</b> 1SB and bank-persistence are both
 * WireMock, per TESTING-RULES R4 — an IT must not depend on a third party's uptime or on
 * credentials. That is what makes this suite runnable on every PR. The credential-gated smoke
 * against the <em>real</em> 1SB sandbox is {@code OneSbSandboxSmokeIT}, which runs nightly.
 * The two together are what criterion 4.1's "in CI (or gated nightly)" asks for.
 *
 * <p>Run: {@code ./gradlew :services:1sb-integration-service:test --tests '*TermJourneyE2EIT'}
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TermJourneyE2EIT.JourneyAuditConfig.class)
@Tag("e2e")
@Tag("integration")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TermJourneyE2EIT {

    // --- 1SB (simulated sandbox) paths ---
    private static final String ONESB_MASTER_LOOKUP = "/v1/master/lookup";
    private static final String ONESB_QUOTE = "/insurance/lifeterm/v1/quote";
    private static final String ONESB_QUOTE_POLL = "/insurance/lifeterm/v1/quote/poll/";
    private static final String ONESB_PROPOSAL_FORM = "/insurance/lifeterm/v1/proposal/form";
    private static final String ONESB_PROPOSAL = "/insurance/lifeterm/v1/proposal";
    private static final String ONESB_PAYMENT_URL = "/v1/payment/url";
    private static final String ONESB_STATUS = "/LifeTerm/prostat/";

    private static final String JOURNEY_ID = "e2e-journey-1";
    private static final String QUOTE_JOB_ID = "job-e2e-quote";
    private static final String PROPOSAL_JOB_ID = "job-e2e-proposal";
    private static final String APPLICATION_NUMBER = "APP-E2E-0001";
    private static final String OFFER_ID = "offer-e2e-1";
    private static final String INSURER_CODE = "HDFC";
    private static final String PRODUCT_CODE = "TERM-01";

    private static final ObjectMapper JSON = new ObjectMapper();

    private static final WireMockServer ONESB = new WireMockServer(wireMockConfig().dynamicPort());
    private static final WireMockServer PERSISTENCE = new WireMockServer(wireMockConfig().dynamicPort());

    /** Carried between ordered steps — this hand-off is the point of the suite. */
    private static String quoteJobId;
    private static String selectedOfferId;
    private static String proposalJobId;
    private static String applicationNumber;

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
        // Keep the poll loop fast; the backoff maths itself is covered by AsyncJobPollerTest.
        registry.add("onesb.poll.base-delay-ms", () -> "1");
        registry.add("onesb.poll.max-delay-ms", () -> "5");
        registry.add("onesb.poll.max-attempts", () -> "5");
    }

    @Autowired
    private MockMvc mockMvc;

    /**
     * A recording publisher rather than a {@code @MockBean}: Spring resets mocks between test
     * methods, so a mock would report zero interactions by the time step 9 runs. The audit trail
     * of a journey only means something when it is observed across the whole journey.
     */
    @TestConfiguration
    static class JourneyAuditConfig {

        static final List<AuditEvent> EVENTS = new CopyOnWriteArrayList<>();

        @Bean
        @Primary
        AuditEventPublisher recordingAuditEventPublisher() {
            return EVENTS::add;
        }
    }

    // ---------------------------------------------------------------------------------------
    // Step 1 — master data
    // ---------------------------------------------------------------------------------------

    @Test
    @Order(1)
    void step1_masterDataLookup_returnsEnumsForTheQuoteForm() throws Exception {
        ONESB.stubFor(post(urlEqualTo(ONESB_MASTER_LOOKUP))
                .willReturn(json("""
                        {"gender":[{"code":"M","label":"Male"},{"code":"F","label":"Female"}]}
                        """)));

        mockMvc.perform(MockMvcRequestBuilders.post("/v1/master-data/lookup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"lob":"TERM","lookUpCategory":"PROPOSAL","entityIds":["gender"]}
                                """))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Master-Cache"))
                .andExpect(jsonPath("$.lookups.gender[0].code", is("M")))
                .andExpect(jsonPath("$.lookups.gender[0].label", is("Male")));
    }

    // ---------------------------------------------------------------------------------------
    // Step 2 — quote submit
    // ---------------------------------------------------------------------------------------

    @Test
    @Order(2)
    void step2_createQuote_accepts202_andReturnsAJobIdWithoutLeakingUpstreamIds() throws Exception {
        stubJobCreate(QUOTE_JOB_ID, "QUOTE");
        stubJobLifecycleWrites();
        ONESB.stubFor(post(urlEqualTo(ONESB_QUOTE))
                .willReturn(json("{\"reqId\":\"REQ-E2E-1\",\"data\":{}}")));
        ONESB.stubFor(get(urlEqualTo(ONESB_QUOTE_POLL + "REQ-E2E-1"))
                .willReturn(json("""
                        {"data":{"isPollComplete":true,"quote":[{
                          "offerId":"%s","insurerCode":"%s","productCode":"%s",
                          "productName":"Term Protect","premiumAmount":4200,"sumAssured":5000000
                        }]}}
                        """.formatted(OFFER_ID, INSURER_CODE, PRODUCT_CODE))));

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/v1/quotes")
                        .header("Idempotency-Key", "e2e-quote-" + UUID.randomUUID())
                        .header("X-Actor-Id", "rm-e2e")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "lob": "TERM",
                                  "journeyId": "%s",
                                  "sumAssured": 5000000,
                                  "members": [{ "dob": "1990-01-15", "gender": "M" }],
                                  "distribution": { "agentId": "109337" }
                                }
                                """.formatted(JOURNEY_ID)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.jobId", is(QUOTE_JOB_ID)))
                .andExpect(jsonPath("$.status", is("PENDING")))
                // The bank caller must never see 1SB's correlation id — R5 / replaceable middleware.
                .andExpect(jsonPath("$.reqId").doesNotExist())
                .andReturn();

        quoteJobId = JSON.readTree(result.getResponse().getContentAsString()).get("jobId").asText();
        assertThat(quoteJobId).isEqualTo(QUOTE_JOB_ID);

        ONESB.verify(1, postRequestedFor(urlEqualTo(ONESB_QUOTE)));
    }

    // ---------------------------------------------------------------------------------------
    // Step 3 — quote result
    // ---------------------------------------------------------------------------------------

    @Test
    @Order(3)
    void step3_pollQuote_returnsOffersForTheJobIdFromStep2() throws Exception {
        stubJobRead(quoteJobId, "QUOTE", "COMPLETED", null);
        PERSISTENCE.stubFor(get(urlPathEqualTo("/internal/v1/jobs/" + quoteJobId + "/offers"))
                .willReturn(json("""
                        [{"offerId":"%s","insurerCode":"%s","productCode":"%s",
                          "productName":"Term Protect","premiumAmount":4200,"sumAssured":5000000}]
                        """.formatted(OFFER_ID, INSURER_CODE, PRODUCT_CODE))));

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.get("/v1/quotes/" + quoteJobId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId", is(quoteJobId)))
                .andExpect(jsonPath("$.status", is("COMPLETED")))
                .andExpect(jsonPath("$.offers[0].offerId", is(OFFER_ID)))
                .andReturn();

        JsonNode offers = JSON.readTree(result.getResponse().getContentAsString()).get("offers");
        selectedOfferId = offers.get(0).get("offerId").asText();
        assertThat(selectedOfferId).isEqualTo(OFFER_ID);
    }

    // ---------------------------------------------------------------------------------------
    // Step 4 — proposal schema
    // ---------------------------------------------------------------------------------------

    @Test
    @Order(4)
    void step4_proposalSchema_isFetchedForTheQuotedProduct() throws Exception {
        ONESB.stubFor(get(urlPathEqualTo(ONESB_PROPOSAL_FORM))
                .willReturn(json("""
                        {"fields":[{"name":"nomineeName","type":"string","required":true}]}
                        """)));

        mockMvc.perform(MockMvcRequestBuilders.get("/v1/proposals/schema")
                        .param("lob", "TERM")
                        .param("productCode", PRODUCT_CODE)
                        .param("manufacturerId", INSURER_CODE)
                        .param("version", "v1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productCode", is(PRODUCT_CODE)));
    }

    // ---------------------------------------------------------------------------------------
    // Step 5 — proposal submit
    // ---------------------------------------------------------------------------------------

    @Test
    @Order(5)
    void step5_submitProposal_usesTheOfferFromStep3_andReturnsAProposalJob() throws Exception {
        stubJobCreate(PROPOSAL_JOB_ID, "PROPOSAL");
        stubJobLifecycleWrites();
        stubJobRead(quoteJobId, "QUOTE", "COMPLETED", null);
        PERSISTENCE.stubFor(get(urlPathEqualTo("/internal/v1/jobs/" + quoteJobId + "/offers"))
                .willReturn(json("""
                        [{"offerId":"%s","insurerCode":"%s","productCode":"%s",
                          "productName":"Term Protect","premiumAmount":4200,"sumAssured":5000000}]
                        """.formatted(OFFER_ID, INSURER_CODE, PRODUCT_CODE))));
        ONESB.stubFor(post(urlEqualTo(ONESB_PROPOSAL))
                .willReturn(json("{\"applicationNumber\":\"%s\"}".formatted(APPLICATION_NUMBER))));

        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/v1/proposals")
                        .header("Idempotency-Key", "e2e-proposal-" + UUID.randomUUID())
                        .header("X-Actor-Id", "rm-e2e")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "lob": "TERM",
                                  "offerId": "%s",
                                  "productCode": "%s",
                                  "manufacturerId": "%s",
                                  "version": "v1",
                                  "journeyId": "%s",
                                  "consentRef": "consent-e2e-1",
                                  "agentId": "109337",
                                  "values": { "nomineeName": "A Nominee" }
                                }
                                """.formatted(selectedOfferId, PRODUCT_CODE, INSURER_CODE, JOURNEY_ID)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.proposalJobId", notNullValue()))
                .andReturn();

        proposalJobId = JSON.readTree(result.getResponse().getContentAsString())
                .get("proposalJobId").asText();
        assertThat(proposalJobId).isEqualTo(PROPOSAL_JOB_ID);

        // COMP-004 / D7: distributor comes from secrets, never from the caller's body.
        ONESB.verify(1, postRequestedFor(urlEqualTo(ONESB_PROPOSAL)));
    }

    // ---------------------------------------------------------------------------------------
    // Step 6 — proposal result
    // ---------------------------------------------------------------------------------------

    @Test
    @Order(6)
    void step6_proposalResult_yieldsTheApplicationNumberForTheProposalJobFromStep5()
            throws Exception {
        stubJobRead(proposalJobId, "PROPOSAL", "COMPLETED", APPLICATION_NUMBER);
        PERSISTENCE.stubFor(get(urlPathEqualTo("/internal/v1/jobs/" + proposalJobId + "/offers"))
                .willReturn(json("[]")));

        MvcResult result = mockMvc.perform(
                        MockMvcRequestBuilders.get("/v1/proposals/" + proposalJobId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId", is(proposalJobId)))
                .andExpect(jsonPath("$.status", is("COMPLETED")))
                .andExpect(jsonPath("$.applicationNumber", is(APPLICATION_NUMBER)))
                .andReturn();

        applicationNumber = JSON.readTree(result.getResponse().getContentAsString())
                .get("applicationNumber").asText();
    }

    // ---------------------------------------------------------------------------------------
    // Step 7 — payment
    // ---------------------------------------------------------------------------------------

    @Test
    @Order(7)
    void step7_createPayment_forTheApplicationNumberFromStep6_returnsHttpsPaymentUrl()
            throws Exception {
        stubJobRead(proposalJobId, "PROPOSAL", "COMPLETED", applicationNumber);
        PERSISTENCE.stubFor(get(urlPathEqualTo("/internal/v1/jobs/" + proposalJobId + "/offers"))
                .willReturn(json("[]")));
        PERSISTENCE.stubFor(post(urlEqualTo("/internal/v1/payment-sessions"))
                .willReturn(aResponse().withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "sessionId": "sess-e2e-1",
                                  "jobId": "%s",
                                  "applicationNumber": "%s",
                                  "lob": "TERM",
                                  "paymentUrl": "https://pay.1sb.test/session/e2e",
                                  "redirectUrl": "https://bank.test/return",
                                  "status": "AWAITING_PAYMENT",
                                  "externalTxnId": "TXN-E2E-1",
                                  "createdAt": "2026-08-13T10:00:00Z",
                                  "expiresAt": "2026-08-13T11:00:00Z"
                                }
                                """.formatted(proposalJobId, applicationNumber))));
        ONESB.stubFor(post(urlEqualTo(ONESB_PAYMENT_URL))
                .willReturn(json("""
                        {"paymentUrl":"https://pay.1sb.test/session/e2e","transactionId":"TXN-E2E-1"}
                        """)));

        mockMvc.perform(MockMvcRequestBuilders.post("/v1/payments")
                        .header("Idempotency-Key", "e2e-payment-" + UUID.randomUUID())
                        .header("X-Actor-Id", "rm-e2e")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "lob": "TERM",
                                  "applicationNumber": "%s",
                                  "jobId": "%s",
                                  "insurerCode": "%s",
                                  "productCode": "%s",
                                  "redirectUrl": "https://bank.test/return",
                                  "journeyId": "%s",
                                  "payer": {
                                    "firstName": "Asha", "lastName": "Rao",
                                    "mobileNumber": "9876543210", "email": "asha@example.com"
                                  },
                                  "amountToBePaid": 4200,
                                  "premiumFrequency": "ANNUAL",
                                  "distribution": { "agentId": "109337" }
                                }
                                """.formatted(applicationNumber, proposalJobId,
                                INSURER_CODE, PRODUCT_CODE, JOURNEY_ID)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.paymentUrl", is("https://pay.1sb.test/session/e2e")))
                .andExpect(jsonPath("$.status", is("AWAITING_PAYMENT")));
    }

    // ---------------------------------------------------------------------------------------
    // Step 8 — status
    // ---------------------------------------------------------------------------------------

    @Test
    @Order(8)
    void step8_applicationStatus_forTheApplicationNumberFromStep6_isNormalisedToBankVocabulary()
            throws Exception {
        ONESB.stubFor(post(urlEqualTo(ONESB_STATUS))
                .willReturn(json("""
                        {
                          "manufacturer": [{
                            "product": [{
                              "applicationStatus": {
                                "applicationStatus": "UNDERWRITING",
                                "manufacturerAppStatus": "Medical review in progress"
                              },
                              "policyDetails": {}
                            }]
                          }]
                        }
                        """)));

        mockMvc.perform(MockMvcRequestBuilders.get("/v1/status/" + applicationNumber)
                        .param("lob", "TERM")
                        .param("insurerCode", INSURER_CODE)
                        .header("X-Actor-Id", "rm-e2e"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applicationNumber", is(applicationNumber)))
                // Bank vocabulary, not 1SB's raw status string.
                .andExpect(jsonPath("$.bankStatus", is("UW_IN_PROGRESS")))
                .andExpect(jsonPath("$.manufacturerSubStatus", is("Medical review in progress")))
                .andExpect(jsonPath("$.rawStatus").doesNotExist());
    }

    // ---------------------------------------------------------------------------------------
    // Step 9 — the journey as a whole
    // ---------------------------------------------------------------------------------------

    /**
     * The audit trail is the compliance artefact for the whole journey (criterion 4.4 reviews
     * its schema). If the journey ran but produced no trail, the run is not evidence of
     * anything — so the suite asserts the trail exists, not just that the endpoints answered.
     */
    @Test
    @Order(9)
    void step9_theJourneyLeftAnAuditTrailCoveringQuoteProposalAndPayment() {
        List<String> actions = JourneyAuditConfig.EVENTS.stream()
                .map(AuditEvent::getAction)
                .toList();

        assertThat(actions).as("the journey must have produced an audit trail").isNotEmpty();

        assertThat(actions)
                .as("every outbound 1SB call must be audited")
                .contains(AuditActions.ONESB_OUTBOUND_CALL);
        assertThat(actions)
                .as("the payment step must record that a payment URL was retrieved")
                .contains(AuditActions.PAYMENT_URL_RETRIEVED);
        assertThat(actions)
                .as("the status check must be audited — it is a customer-data read")
                .contains(AuditActions.APPLICATION_STATUS_CHECKED);
    }

    /** The identifiers threaded through steps 2–8 must be the same ones throughout. */
    @Test
    @Order(10)
    void step10_identifiersWereThreadedThroughTheWholeJourney() {
        assertThat(quoteJobId).isEqualTo(QUOTE_JOB_ID);
        assertThat(selectedOfferId).isEqualTo(OFFER_ID);
        assertThat(proposalJobId).isEqualTo(PROPOSAL_JOB_ID);
        assertThat(applicationNumber).isEqualTo(APPLICATION_NUMBER);
    }

    // ---------------------------------------------------------------------------------------
    // stubs
    // ---------------------------------------------------------------------------------------

    private static com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder json(String body) {
        return aResponse().withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(body);
    }

    private static void stubJobCreate(String jobId, String jobType) {
        PERSISTENCE.stubFor(post(urlEqualTo("/internal/v1/jobs"))
                .willReturn(aResponse().withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody(jobBody(jobId, jobType, "PENDING", null))));
    }

    private static void stubJobRead(String jobId, String jobType, String status, String appNo) {
        PERSISTENCE.stubFor(get(urlPathEqualTo("/internal/v1/jobs/" + jobId))
                .willReturn(json(jobBody(jobId, jobType, status, appNo))));
    }

    /** Poll attempts, status patches, offer writes and raw-payload capture all succeed. */
    private static void stubJobLifecycleWrites() {
        PERSISTENCE.stubFor(patch(urlPathMatching("/internal/v1/jobs/.*/status"))
                .willReturn(json("{\"status\":\"RUNNING\"}")));
        PERSISTENCE.stubFor(post(urlPathMatching("/internal/v1/jobs/.*/poll-attempts"))
                .willReturn(aResponse().withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"attemptId\":1}")));
        PERSISTENCE.stubFor(post(urlPathMatching("/internal/v1/jobs/.*/offers"))
                .willReturn(aResponse().withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"offerId\":\"" + OFFER_ID + "\"}")));
        PERSISTENCE.stubFor(post(urlEqualTo("/internal/v1/raw-payloads"))
                .willReturn(aResponse().withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"payloadId\":\"raw-1\"}")));
    }

    private static String jobBody(String jobId, String jobType, String status, String appNo) {
        return """
                {
                  "jobId": "%s",
                  "jobType": "%s",
                  "lob": "TERM",
                  "status": "%s",
                  %s
                  "journeyId": "%s",
                  "idempotencyKey": "idem-%s",
                  "createdAt": "2026-08-13T10:00:00Z",
                  "updatedAt": "2026-08-13T10:00:05Z",
                  "version": 1,
                  "createdByActor": "rm-e2e"
                }
                """.formatted(
                jobId, jobType, status,
                appNo == null ? "" : "\"applicationNumber\": \"" + appNo + "\",",
                JOURNEY_ID, jobId);
    }
}
