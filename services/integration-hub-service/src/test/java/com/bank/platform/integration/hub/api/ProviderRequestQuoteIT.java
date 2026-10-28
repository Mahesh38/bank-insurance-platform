package com.bank.platform.integration.hub.api;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bank.common.error.ErrorCodes;
import com.bank.common.security.internal.InternalCallerAuth;
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

  private static final WireMockServer onesb =
      new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());

  static {
    onesb.start();
  }

  @Autowired private MockMvc mockMvc;

  @AfterAll
  static void stopWireMock() {
    onesb.stop();
  }

  @DynamicPropertySource
  static void hubProps(DynamicPropertyRegistry registry) {
    registry.add("hub.onesb.base-url", () -> "http://localhost:" + onesb.port());
    registry.add("hub.onesb.internal-service-key", () -> "local-onesb-hub-internal-key");
  }

  @BeforeEach
  void resetStubs() {
    onesb.resetAll();
  }

  @Test
  void savingsPremiumQuote_submitsMappedLobAndPollsOffers_AT_HUB_07() throws Exception {
    onesb.stubFor(
        WireMock.post(urlEqualTo("/v1/quotes"))
            .willReturn(
                aResponse()
                    .withStatus(202)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"jobId\":\"job-savings-1\",\"status\":\"PENDING\"}")));
    onesb.stubFor(
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

    onesb.verify(
        postRequestedFor(urlEqualTo("/v1/quotes"))
            .withHeader(InternalCallerAuth.HEADER, equalTo("local-onesb-hub-internal-key"))
            .withRequestBody(containing("\"lob\":\"SAVING\""))
            .withRequestBody(containing("\"paymentFrequency\":\"HALF-YEARLY\""))
            .withRequestBody(containing("\"premiumPaymentTerm\":10")));

    mockMvc
        .perform(
            MockMvcRequestBuilders.get("/internal/v1/provider-requests/" + providerRequestId)
                .header(InternalCallerAuth.HEADER, quotationKey()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.state").value("PARTIALLY_COMPLETED"))
        .andExpect(jsonPath("$.result.offers[0].providerOfferRef").value("off-1"));

    onesb.verify(getRequestedFor(urlPathEqualTo("/v1/quotes/job-savings-1")));
  }

  @Test
  void sameKeySameBody_replaysWithoutSecondAdapterCall_AT_HUB_02() throws Exception {
    onesb.stubFor(
        WireMock.post(urlEqualTo("/v1/quotes"))
            .willReturn(
                aResponse()
                    .withStatus(202)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"jobId\":\"job-idem-1\",\"status\":\"PENDING\"}")));

    String body = termQuoteBody();
    String key = "quote:01JTQ7Z9X4N2B8C5D6E7F8G9H1:submit";

    mockMvc
        .perform(quotePost(key, body))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.providerRequestId").exists());

    mockMvc.perform(quotePost(key, body)).andExpect(status().isAccepted());

    onesb.verify(1, postRequestedFor(urlEqualTo("/v1/quotes")));
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

    onesb.verify(0, postRequestedFor(urlEqualTo("/v1/quotes")));
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
        .andExpect(jsonPath("$.code").value(ErrorCodes.UNSUPPORTED_LOB));
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

  private static String termQuoteBody() {
    return """
        {
          "operation":"QUOTE",
          "lob":"LIFE",
          "productClass":"TERM",
          "callerRef":{"context":"quotation","aggregateId":"01JTQ7Z9X4N2B8C5D6E7F8G9H1"},
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
        """;
  }

  private static String quotationKey() {
    return "local-integrationhub-quotation-internal-key";
  }
}
