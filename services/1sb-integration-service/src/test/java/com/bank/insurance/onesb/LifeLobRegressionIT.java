package com.bank.insurance.onesb;

import com.bank.common.error.ErrorCodes;
import com.github.tomakehurst.wiremock.WireMockServer;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.patch;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * QA-012 — Life LOB WireMock regression: Term non-regression + Savings/ULIP quote & proposal paths.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Tag("QA-012")
@Tag("integration")
class LifeLobRegressionIT {

    private static final String TERM_QUOTE = "/insurance/lifeterm/v1/quote";
    private static final String LIFESAVE_QUOTE = "/insurance/lifesave/v1/quote";
    private static final String TERM_PROPOSAL = "/insurance/lifeterm/v1/proposal";
    private static final String LIFESAVE_PROPOSAL = "/insurance/lifesave/v1/proposal";

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
    }

    @Test
    void termQuote_stillPostsToLifeterm_nonRegression() throws Exception {
        String jobId = "job-term-" + UUID.randomUUID();
        stubPersistenceJob(jobId, "QUOTE", "TERM");
        ONESB.stubFor(post(urlEqualTo(TERM_QUOTE))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"reqId\":\"REQ-TERM\",\"data\":{}}")));
        stubQuotePollComplete("/insurance/lifeterm/v1/quote/poll/REQ-TERM");

        mockMvc.perform(MockMvcRequestBuilders.post("/v1/quotes")
                        .header("Idempotency-Key", "idem-term-" + UUID.randomUUID())
                        .header("X-Actor-Id", "rm-qa")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(quoteBody("TERM")))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.jobId", is(jobId)));

        ONESB.verify(exactly(1), postRequestedFor(urlEqualTo(TERM_QUOTE))
                .withRequestBody(matchingJsonPath("$.product.productType", containing("LifeTerm"))));
    }

    @Test
    void savingQuote_postsToLifesave_withLifeSaveProductType() throws Exception {
        String jobId = "job-save-" + UUID.randomUUID();
        stubPersistenceJob(jobId, "QUOTE", "SAVING");
        ONESB.stubFor(post(urlEqualTo(LIFESAVE_QUOTE))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"reqId\":\"REQ-SAVE\",\"data\":{}}")));
        stubQuotePollComplete("/insurance/lifesave/v1/quote/poll/REQ-SAVE");

        mockMvc.perform(MockMvcRequestBuilders.post("/v1/quotes")
                        .header("Idempotency-Key", "idem-save-" + UUID.randomUUID())
                        .header("X-Actor-Id", "rm-qa")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(quoteBody("SAVING")))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.jobId", is(jobId)));

        ONESB.verify(exactly(1), postRequestedFor(urlEqualTo(LIFESAVE_QUOTE))
                .withRequestBody(matchingJsonPath("$.product.productType", containing("LifeSave")))
                .withRequestBody(matchingJsonPath("$.product.savingsProductType[0]",
                        containing("nonParticipating"))));
    }

    @Test
    void ulipQuote_postsToLifesave_withUlipSavingsProductType() throws Exception {
        String jobId = "job-ulip-" + UUID.randomUUID();
        stubPersistenceJob(jobId, "QUOTE", "ULIP");
        ONESB.stubFor(post(urlEqualTo(LIFESAVE_QUOTE))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"reqId\":\"REQ-ULIP\",\"data\":{}}")));
        stubQuotePollComplete("/insurance/lifesave/v1/quote/poll/REQ-ULIP");

        mockMvc.perform(MockMvcRequestBuilders.post("/v1/quotes")
                        .header("Idempotency-Key", "idem-ulip-" + UUID.randomUUID())
                        .header("X-Actor-Id", "rm-qa")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(quoteBody("ULIP")))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.jobId", is(jobId)));

        ONESB.verify(exactly(1), postRequestedFor(urlEqualTo(LIFESAVE_QUOTE))
                .withRequestBody(matchingJsonPath("$.product.productType", containing("LifeSave")))
                .withRequestBody(matchingJsonPath("$.product.savingsProductType[0]",
                        containing("ULIP"))));
    }

    @Test
    void savingProposal_postsToLifesaveProposal() throws Exception {
        String jobId = "job-sp-" + UUID.randomUUID();
        stubPersistenceJob(jobId, "PROPOSAL", "SAVING");
        ONESB.stubFor(post(urlEqualTo(LIFESAVE_PROPOSAL))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"applicationNumber\":\"APP-S\",\"reqId\":\"REQ-SP\"}")));

        mockMvc.perform(MockMvcRequestBuilders.post("/v1/proposals")
                        .header("Idempotency-Key", "idem-sp-" + UUID.randomUUID())
                        .header("X-Actor-Id", "rm-qa")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(proposalBody("SAVING")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.proposalJobId", is(jobId)))
                .andExpect(jsonPath("$.status", is("COMPLETED")));

        ONESB.verify(exactly(1), postRequestedFor(urlEqualTo(LIFESAVE_PROPOSAL))
                .withRequestBody(matchingJsonPath("$.distributor.distributorID", containing("TEST_DIST")))
                .withRequestBody(matchingJsonPath("$['proposer.panNumber']", containing("ABCDE1234F"))));
    }

    @Test
    void ulipProposal_postsToLifesaveProposal() throws Exception {
        String jobId = "job-up-" + UUID.randomUUID();
        stubPersistenceJob(jobId, "PROPOSAL", "ULIP");
        ONESB.stubFor(post(urlEqualTo(LIFESAVE_PROPOSAL))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"applicationNumber\":\"APP-U\",\"reqId\":\"REQ-UP\"}")));

        mockMvc.perform(MockMvcRequestBuilders.post("/v1/proposals")
                        .header("Idempotency-Key", "idem-up-" + UUID.randomUUID())
                        .header("X-Actor-Id", "rm-qa")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(proposalBody("ULIP")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.proposalJobId", is(jobId)))
                .andExpect(jsonPath("$.status", is("COMPLETED")));

        ONESB.verify(exactly(1), postRequestedFor(urlEqualTo(LIFESAVE_PROPOSAL)));
        ONESB.verify(0, postRequestedFor(urlEqualTo(TERM_PROPOSAL)));
    }

    @Test
    void savingProposalSchema_getsLifesaveProposalPath() throws Exception {
        ONESB.stubFor(get(urlPathEqualTo(LIFESAVE_PROPOSAL))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "fieldGroups": [{"name":"personal","fields":[{"id":"pan","type":"string"}]}],
                                  "version": "1"
                                }
                                """)));

        mockMvc.perform(MockMvcRequestBuilders.get("/v1/proposals/schema")
                        .param("lob", "SAVING")
                        .param("productCode", "S1")
                        .param("manufacturerId", "MFG")
                        .param("version", "1"))
                .andExpect(status().isOk());

        ONESB.verify(exactly(1), com.github.tomakehurst.wiremock.client.WireMock
                .getRequestedFor(urlPathEqualTo(LIFESAVE_PROPOSAL)));
    }

    @Test
    void healthLob_returnsUnsupported_andNeverCallsOneSb() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/v1/quotes")
                        .header("Idempotency-Key", "idem-health-" + UUID.randomUUID())
                        .header("X-Actor-Id", "rm-qa")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(quoteBody("HEALTH")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code", is(ErrorCodes.UNSUPPORTED_LOB)));

        ONESB.verify(0, postRequestedFor(urlEqualTo(TERM_QUOTE)));
        ONESB.verify(0, postRequestedFor(urlEqualTo(LIFESAVE_QUOTE)));
        PERSISTENCE.verify(0, postRequestedFor(urlEqualTo("/internal/v1/jobs")));
    }

    private static String quoteBody(String lob) {
        return """
                {
                  "lob": "%s",
                  "journeyId": "j-qa-012",
                  "sumAssured": 5000000,
                  "members": [{ "dob": "1990-01-15", "gender": "M" }],
                  "distribution": { "agentId": "109337" }
                }
                """.formatted(lob);
    }

    private static String proposalBody(String lob) {
        return """
                {
                  "lob": "%s",
                  "journeyId": "j-qa-prop",
                  "schemaId": "scm-1",
                  "offerId": "off-1",
                  "productCode": "P1",
                  "manufacturerId": "MFG",
                  "version": "1",
                  "consentRef": "consent-1",
                  "agentId": "109337",
                  "values": { "proposer.panNumber": "ABCDE1234F" },
                  "distribution": { "rmEmployeeId": "E123", "channelType": "B2B" }
                }
                """.formatted(lob);
    }

    private static void stubPersistenceJob(String jobId, String jobType, String lob) {
        PERSISTENCE.stubFor(post(urlEqualTo("/internal/v1/jobs"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "jobId": "%s",
                                  "jobType": "%s",
                                  "lob": "%s",
                                  "status": "PENDING",
                                  "journeyId": "j-qa-012",
                                  "idempotencyKey": "idem",
                                  "createdAt": "2026-09-03T12:00:00Z",
                                  "updatedAt": "2026-09-03T12:00:00Z",
                                  "version": 0,
                                  "createdByActor": "LifeLobRegressionIT"
                                }
                                """.formatted(jobId, jobType, lob))));

        PERSISTENCE.stubFor(patch(urlPathMatching("/internal/v1/jobs/.*/status"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"jobId\":\"" + jobId + "\",\"status\":\"RUNNING\"}")));

        PERSISTENCE.stubFor(post(urlPathMatching("/internal/v1/jobs/.*/poll-attempts"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"attemptId\":1}")));

        PERSISTENCE.stubFor(post(urlPathMatching("/internal/v1/jobs/.*/offers"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"offerId\":\"o1\"}")));
    }

    private static void stubQuotePollComplete(String pollPath) {
        ONESB.stubFor(get(urlEqualTo(pollPath))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "data": {
                                    "isPollComplete": true,
                                    "quote": [{
                                      "offerId": "off-qa",
                                      "insurerCode": "MFG",
                                      "productCode": "P1",
                                      "productName": "Life",
                                      "premiumAmount": 1000,
                                      "sumAssured": 5000000
                                    }]
                                  }
                                }
                                """)));
    }
}
