package com.bank.insurance.onesb.support;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bank.common.audit.AuditEventPublisher;
import com.bank.common.test.PyramidTags;
import com.bank.common.test.contract.ContractTags;
import com.bank.common.test.e2e.E2ETags;
import com.bank.common.test.fixtures.DomainFixtures;
import com.bank.common.test.wiremock.WireMockHarness;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
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

/**
 * S08-E03-S02 / S04 / S05 + TD-014 — shared {@link WireMockHarness} drives the integration →
 * persistence HTTP seam (contract) and an assisted-life smoke path (e2e tag).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Tag(PyramidTags.INTEGRATION)
@Tag(ContractTags.CONTRACT)
@Tag(ContractTags.PERSISTENCE_JOBS_API)
@Tag(E2ETags.E2E)
@Tag(E2ETags.ASSISTED_LIFE_SMOKE)
class SharedHarnessProposalSmokeIT {

  private static final String TERM_PROPOSAL_PATH = "/insurance/lifeterm/v1/proposal";
  private static final WireMockHarness HARNESS = WireMockHarness.start();

  @AfterAll
  static void stopHarness() {
    HARNESS.close();
  }

  @DynamicPropertySource
  static void bindHarness(DynamicPropertyRegistry registry) {
    HARNESS.register(registry);
    registry.add("onesb.distributor-id", () -> "TEST_DIST");
    registry.add("onesb.poll.base-delay-ms", () -> "1");
    registry.add("onesb.poll.max-delay-ms", () -> "5");
    registry.add("onesb.poll.max-attempts", () -> "3");
  }

  @Autowired private MockMvc mockMvc;

  @MockBean private AuditEventPublisher auditEventPublisher;

  @BeforeEach
  void reset() {
    HARNESS.reset();
  }

  @Test
  void proposal_persistsJobViaSharedHarness_andCallsOneSb() throws Exception {
    String jobId = DomainFixtures.jobId();
    HARNESS
        .persistence()
        .stubFor(
            post(urlEqualTo("/internal/v1/jobs"))
                .willReturn(
                    aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            """
                                {
                                  "jobId": "%s",
                                  "jobType": "PROPOSAL",
                                  "lob": "TERM",
                                  "status": "PENDING",
                                  "journeyId": "j-shared-1",
                                  "idempotencyKey": "idem",
                                  "createdAt": "2026-09-13T12:00:00Z",
                                  "updatedAt": "2026-09-13T12:00:00Z",
                                  "version": 0,
                                  "createdByActor": "SharedHarnessProposalSmokeIT"
                                }
                                """
                                .formatted(jobId))));
    HARNESS
        .persistence()
        .stubFor(
            com.github.tomakehurst.wiremock.client.WireMock.patch(
                    urlPathMatching("/internal/v1/jobs/.*/status"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"jobId\":\"%s\",\"status\":\"COMPLETED\"}".formatted(jobId))));

    HARNESS
        .onesb()
        .stubFor(
            post(urlEqualTo(TERM_PROPOSAL_PATH))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                            "{\"applicationNumber\":\"APP-SHARED\",\"reqId\":\"REQ-SHARED\"}")));

    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/v1/proposals")
                .header("Idempotency-Key", DomainFixtures.idempotencyKey("shared-prop"))
                .header("X-Actor-Id", DomainFixtures.actorId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                                {
                                  "lob": "TERM",
                                  "journeyId": "j-shared-1",
                                  "schemaId": "scm-term-1",
                                  "offerId": "off-1",
                                  "productCode": "T1",
                                  "manufacturerId": "HDFC",
                                  "version": "1",
                                  "consentRef": "consent-1",
                                  "agentId": "109337",
                                  "values": {
                                    "proposer.panNumber": "ABCDE1234F",
                                    "nominee.name": "Jane Doe"
                                  },
                                  "distribution": { "rmEmployeeId": "E123", "channelType": "B2B" }
                                }
                                """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.proposalJobId", is(jobId)));

    HARNESS.persistence().verify(exactly(1), postRequestedFor(urlEqualTo("/internal/v1/jobs")));
    HARNESS.onesb().verify(exactly(1), postRequestedFor(urlEqualTo(TERM_PROPOSAL_PATH)));
  }
}
