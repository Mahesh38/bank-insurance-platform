package com.bank.platform.integration.hub.api;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bank.common.error.ErrorCodes;
import com.bank.common.security.internal.InternalCallerAuth;
import com.bank.platform.integration.hub.domain.ProviderBreakerRegistry;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@SpringBootTest
@AutoConfigureMockMvc
@Tag("integration")
@Tag("FUNC-032")
class ProviderRequestQuoteIT {

  private static final WireMockServer ONESB =
      new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());

  static {
    ONESB.start();
  }

  @Autowired private MockMvc mockMvc;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private ProviderBreakerRegistry breakers;

  @AfterAll
  static void stopWireMock() {
    ONESB.stop();
  }

  @DynamicPropertySource
  static void hubProps(DynamicPropertyRegistry registry) {
    registry.add("hub.onesb.base-url", () -> "http://localhost:" + ONESB.port());
    registry.add("hub.onesb.internal-service-key", () -> "local-onesb-hub-internal-key");
    registry.add("hub.onesb.submit-read-timeout-ms", () -> "400");
    registry.add("hub.onesb.poll-read-timeout-ms", () -> "2000");
    registry.add("hub.breaker.enabled", () -> "true");
    registry.add("hub.breaker.failure-threshold", () -> "1");
    registry.add("hub.breaker.open-wait-ms", () -> "60000");
    registry.add("hub.reconciliation.uncertain-window", () -> "PT0.05S");
  }

  @BeforeEach
  void resetStubs() {
    ONESB.resetAll();
    breakers.resetAll();
    jdbc.update("DELETE FROM hub.outbox_event");
    jdbc.update("DELETE FROM hub.provider_request");
  }

  @Test
  void savingsPremiumQuote_submitsMappedLobAndPollsOffers_AT_HUB_07() throws Exception {
    ONESB.stubFor(
        WireMock.post(urlEqualTo("/v1/quotes"))
            .willReturn(
                aResponse()
                    .withStatus(202)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"jobId\":\"job-savings-1\",\"status\":\"PENDING\"}")));
    ONESB.stubFor(
        WireMock.get(urlPathEqualTo("/v1/quotes/job-savings-1"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        """
                        {
                          "jobId":"job-savings-1",
                          "status":"PARTIAL",
                          "offers":[{
                            "offerId":"off-1",
                            "insurerCode":"INS01",
                            "productCode":"SAVE-1",
                            "productName":"Saver",
                            "premiumAmount":25000,
                            "premiumFrequency":"YEARLY",
                            "sumAssured":500000,
                            "outOfBound":false
                          },{
                            "insurerCode":"INS02",
                            "productCode":"SAVE-2",
                            "errorSummary":"age band"
                          }]
                        }
                        """)));

    String body =
        """
        {
          "operation":"QUOTE",
          "lob":"LIFE",
          "productClass":"SAVINGS",
          "callerRef":{"context":"quotation","aggregateId":"01JTQ7Z9X4N2B8C5D6E7F8G9H0"},
          "journeyId":"01JQX4K8A1B2C3D4E5F6G7H8J9",
          "mode":"MULTI",
          "body":{
            "category":"PREMIUM",
            "requestedCover":500000,
            "premiumAmount":25000,
            "members":[{
              "role":"LIFE_ASSURED","sequence":1,"dob":"1990-04-12","gender":"MALE",
              "tobacco":false,"annualIncome":1500000,"pincode":"400001"
            }],
            "preferences":{"paymentFrequency":"HALF_YEARLY","policyTerm":15}
          },
          "selection":{
            "insurerCode":"INS01",
            "productCodes":["SAVE-1"],
            "policyTerm":15,
            "premiumPaymentTerm":10
          }
        }
        """;

    MvcResult created =
        mockMvc
            .perform(
                MockMvcRequestBuilders.post("/internal/v1/provider-requests")
                    .header(InternalCallerAuth.HEADER, quotationKey())
                    .header("Idempotency-Key", "quote:01JTQ7Z9X4N2B8C5D6E7F8G9H0:submit")
                    .header("X-Actor-Id", "RM-1001")
                    .header("X-Correlation-Id", "11111111-1111-1111-1111-111111111111")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.state").value("IN_PROGRESS"))
            .andExpect(jsonPath("$.provider").value("ONE_SB"))
            .andReturn();

    String providerRequestId =
        com.jayway.jsonpath.JsonPath.read(
            created.getResponse().getContentAsString(), "$.providerRequestId");

    ONESB.verify(
        postRequestedFor(urlEqualTo("/v1/quotes"))
            .withHeader(InternalCallerAuth.HEADER, equalTo("local-onesb-hub-internal-key"))
            .withRequestBody(containing("\"lob\":\"SAVING\""))
            .withRequestBody(containing("\"paymentFrequency\":\"HALF-YEARLY\""))
            .withRequestBody(containing("\"sequenceNumber\":1"))
            .withRequestBody(containing("\"premiumPaymentTerm\":10")));

    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/internal/v1/provider-requests/" + providerRequestId)
                .header(InternalCallerAuth.HEADER, quotationKey()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.state").value("PARTIALLY_COMPLETED"))
        .andExpect(jsonPath("$.result.offers[0].providerOfferRef").value("off-1"))
        .andExpect(jsonPath("$.result.offers.length()").value(1))
        .andExpect(jsonPath("$.result.partialErrors[0].insurerCode").value("INS02"))
        .andExpect(jsonPath("$.result.partialErrors[0].code").value("UPSTREAM_BUSINESS_ERROR"));
  }

  @Test
  void sameKeySameBody_replaysWithoutSecondAdapterCall_AT_HUB_02() throws Exception {
    ONESB.stubFor(
        WireMock.post(urlEqualTo("/v1/quotes"))
            .willReturn(
                aResponse()
                    .withStatus(202)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"jobId\":\"job-idem-1\",\"status\":\"PENDING\"}")));

    String body = termQuoteBody("01JTQ7Z9X4N2B8C5D6E7F8G9H1");
    String key = "quote:01JTQ7Z9X4N2B8C5D6E7F8G9H1:submit";

    mockMvc.perform(quotePost(key, body)).andExpect(status().isAccepted());
    mockMvc.perform(quotePost(key, body)).andExpect(status().isAccepted());
    ONESB.verify(1, postRequestedFor(urlEqualTo("/v1/quotes")));
  }

  @Test
  void sameKeyDifferentBody_conflict_AT_HUB_02() throws Exception {
    ONESB.stubFor(
        WireMock.post(urlEqualTo("/v1/quotes"))
            .willReturn(
                aResponse()
                    .withStatus(202)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"jobId\":\"job-idem-2\",\"status\":\"PENDING\"}")));

    String key = "quote:01JTQ7Z9X4N2B8C5D6E7F8G9H4:submit";
    mockMvc
        .perform(quotePost(key, termQuoteBody("01JTQ7Z9X4N2B8C5D6E7F8G9H4")))
        .andExpect(status().isAccepted());

    String different = termQuoteBody("01JTQ7Z9X4N2B8C5D6E7F8G9H4").replace("10000000", "20000000");
    mockMvc
        .perform(quotePost(key, different))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value(ErrorCodes.IDEMPOTENCY_CONFLICT));
  }

  @Test
  void attributionInBody_refused_AT_HUB_04() throws Exception {
    String body =
        """
        {
          "operation":"QUOTE",
          "lob":"LIFE",
          "productClass":"TERM",
          "callerRef":{"context":"quotation","aggregateId":"01JTQ7Z9X4N2B8C5D6E7F8G9H2"},
          "journeyId":"01JQX4K8A1B2C3D4E5F6G7H8J9",
          "mode":"MULTI",
          "body":{
            "category":"SUM_ASSURED",
            "requestedCover":1000000,
            "members":[{"role":"LIFE_ASSURED","sequence":1,"dob":"1990-04-12","gender":"MALE"}],
            "distribution":{"agentId":"sneaky"}
          }
        }
        """;

    mockMvc
        .perform(quotePost("quote:attr-1:submit", body))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value(ErrorCodes.ATTRIBUTION_NOT_CALLER_SUPPLIED));

    ONESB.verify(0, postRequestedFor(urlEqualTo("/v1/quotes")));
    Integer audits =
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM hub.outbox_event WHERE event_type = ?",
            Integer.class,
            "ATTRIBUTION_NOT_CALLER_SUPPLIED");
    org.assertj.core.api.Assertions.assertThat(audits).isEqualTo(1);
  }

  @Test
  void healthLob_configurationUnresolvable_AT_HUB_01() throws Exception {
    String body =
        """
        {
          "operation":"QUOTE",
          "lob":"HEALTH",
          "productClass":"TERM",
          "callerRef":{"context":"quotation","aggregateId":"01JTQ7Z9X4N2B8C5D6E7F8G9H3"},
          "journeyId":"01JQX4K8A1B2C3D4E5F6G7H8J9",
          "mode":"MULTI",
          "body":{
            "category":"SUM_ASSURED",
            "requestedCover":1000000,
            "members":[{"role":"LIFE_ASSURED","sequence":1,"dob":"1990-04-12","gender":"MALE"}]
          }
        }
        """;

    mockMvc
        .perform(quotePost("quote:health-1:submit", body))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value(ErrorCodes.CONFIGURATION_UNRESOLVABLE));
  }

  @Test
  void adapterSubmitTimeout_submissionUncertain_AT_HUB_05() throws Exception {
    ONESB.stubFor(
        WireMock.post(urlEqualTo("/v1/quotes"))
            .willReturn(
                aResponse()
                    .withFixedDelay(800)
                    .withStatus(202)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"jobId\":\"late\",\"status\":\"PENDING\"}")));

    mockMvc
        .perform(quotePost("quote:timeout-1:submit", termQuoteBody("01JTQ7Z9X4N2B8C5D6E7F8G9H5")))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.state").value("SUBMISSION_UNCERTAIN"));
  }

  @Test
  void breakerOpen_returns503_AT_HUB_09() throws Exception {
    ONESB.stubFor(
        WireMock.post(urlEqualTo("/v1/quotes"))
            .willReturn(aResponse().withStatus(503).withBody("down")));

    mockMvc
        .perform(quotePost("quote:brk-1:submit", termQuoteBody("01JTQ7Z9X4N2B8C5D6E7F8G9H6")))
        .andExpect(status().isServiceUnavailable());

    mockMvc
        .perform(quotePost("quote:brk-2:submit", termQuoteBody("01JTQ7Z9X4N2B8C5D6E7F8G9H7")))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.code").value(ErrorCodes.UPSTREAM_UNAVAILABLE));

    ONESB.verify(1, postRequestedFor(urlEqualTo("/v1/quotes")));
  }

  @Test
  void pollByNonCaller_404_AT_HUB_11() throws Exception {
    ONESB.stubFor(
        WireMock.post(urlEqualTo("/v1/quotes"))
            .willReturn(
                aResponse()
                    .withStatus(202)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"jobId\":\"job-x\",\"status\":\"PENDING\"}")));

    MvcResult created =
        mockMvc
            .perform(
                quotePost("quote:caller-1:submit", termQuoteBody("01JTQ7Z9X4N2B8C5D6E7F8G9H8")))
            .andExpect(status().isAccepted())
            .andReturn();
    String id =
        com.jayway.jsonpath.JsonPath.read(
            created.getResponse().getContentAsString(), "$.providerRequestId");

    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/internal/v1/provider-requests/" + id)
                .header(InternalCallerAuth.HEADER, "local-integrationhub-proposal-internal-key"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value(ErrorCodes.RESOURCE_NOT_FOUND));
  }

  @Test
  void uncertainWindowElapsed_failsOnPoll_AT_HUB_05_reconcile() throws Exception {
    ONESB.stubFor(
        WireMock.post(urlEqualTo("/v1/quotes"))
            .willReturn(
                aResponse()
                    .withFixedDelay(800)
                    .withStatus(202)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"jobId\":\"late2\",\"status\":\"PENDING\"}")));

    MvcResult created =
        mockMvc
            .perform(
                quotePost("quote:uncert-1:submit", termQuoteBody("01JTQ7Z9X4N2B8C5D6E7F8G9H9")))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.state").value("SUBMISSION_UNCERTAIN"))
            .andReturn();
    String id =
        com.jayway.jsonpath.JsonPath.read(
            created.getResponse().getContentAsString(), "$.providerRequestId");

    Thread.sleep(80);

    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/internal/v1/provider-requests/" + id)
                .header(InternalCallerAuth.HEADER, quotationKey()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.state").value("FAILED"))
        .andExpect(jsonPath("$.failure.code").value(ErrorCodes.UPSTREAM_TIMEOUT));
  }

  private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder quotePost(
      String key, String body) {
    return MockMvcRequestBuilders.post("/internal/v1/provider-requests")
        .header(InternalCallerAuth.HEADER, quotationKey())
        .header("Idempotency-Key", key)
        .header("X-Actor-Id", "RM-1001")
        .header("X-Correlation-Id", "22222222-2222-2222-2222-222222222222")
        .contentType(MediaType.APPLICATION_JSON)
        .content(body);
  }

  private static String termQuoteBody(String aggregateId) {
    return """
        {
          "operation":"QUOTE",
          "lob":"LIFE",
          "productClass":"TERM",
          "callerRef":{"context":"quotation","aggregateId":"%s"},
          "journeyId":"01JQX4K8A1B2C3D4E5F6G7H8J9",
          "mode":"MULTI",
          "body":{
            "category":"SUM_ASSURED",
            "requestedCover":10000000,
            "members":[{
              "role":"LIFE_ASSURED","sequence":1,"dob":"1990-04-12","gender":"MALE",
              "tobacco":false,"annualIncome":1500000,"pincode":"400001"
            }],
            "preferences":{"paymentFrequency":"YEARLY"}
          }
        }
        """
        .formatted(aggregateId);
  }

  private static String quotationKey() {
    return "local-integrationhub-quotation-internal-key";
  }
}
