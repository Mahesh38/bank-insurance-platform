package com.bank.platform.quotation.api;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bank.common.security.internal.InternalCallerAuth;
import com.bank.platform.quotation.domain.Ulid;
import com.bank.platform.quotation.support.EmbeddedDynamoDbTestConfig;
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
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** FUNC-033 — createQuote → Hub WireMock → getQuote poll once → QUOTED with coverTerm/ppt. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(EmbeddedDynamoDbTestConfig.class)
@Tag("integration")
@Tag("FUNC-033")
class CreateGetQuoteIT {

  private static final String PROVIDER_REQUEST_ID = "01JZPRQ033TEST000000000001";
  private static final String OFFER_REF = "01JZ0FFR033TEST00000000001";

  private static final WireMockServer HUB =
      new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());

  static {
    HUB.start();
  }

  @Autowired private MockMvc mvc;

  @AfterAll
  static void stopHub() {
    HUB.stop();
  }

  @DynamicPropertySource
  static void hubUrl(DynamicPropertyRegistry registry) {
    registry.add("quotation.clients.hub.base-url", () -> "http://localhost:" + HUB.port());
    registry.add(
        "quotation.clients.hub.internal-service-key",
        () -> "local-integrationhub-quotation-internal-key");
    registry.add("quotation.gates.stub", () -> "true");
  }

  @BeforeEach
  void resetHub() {
    HUB.resetAll();
  }

  @Test
  void createThenGet_pollsHubOnce_quotedWithCoverTermAndPpt() throws Exception {
    HUB.stubFor(
        WireMock.post(urlEqualTo("/internal/v1/provider-requests"))
            .willReturn(
                aResponse()
                    .withStatus(202)
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        """
                        {
                          "providerRequestId":"%s",
                          "operation":"QUOTE",
                          "state":"IN_PROGRESS",
                          "provider":"ONE_SB"
                        }
                        """
                            .formatted(PROVIDER_REQUEST_ID))));
    HUB.stubFor(
        WireMock.post(urlPathEqualTo("/internal/v1/provider-requests/" + PROVIDER_REQUEST_ID))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        """
                        {
                          "providerRequestId":"%s",
                          "operation":"QUOTE",
                          "state":"COMPLETED",
                          "provider":"ONE_SB",
                          "callerRef":{"context":"quotation","aggregateId":"01JZAGG033TEST000000000001"},
                          "createdAt":"2026-10-28T08:00:00Z",
                          "result":{
                            "offers":[{
                              "providerOfferRef":"%s",
                              "insurerCode":"INS01",
                              "productCode":"SAVE-1",
                              "productName":"Saver",
                              "premiumAmount":25000,
                              "premiumFrequency":"HALF_YEARLY",
                              "sumAssured":500000,
                              "outOfBound":false,
                              "coverTerm":15,
                              "premiumPayingTerm":10
                            }],
                            "partialErrors":[]
                          }
                        }
                        """
                            .formatted(PROVIDER_REQUEST_ID, OFFER_REF))));

    String leadId = Ulid.mint();
    String journeyId = Ulid.mint();
    String customerId = Ulid.mint();
    String suitabilityId = Ulid.mint();
    String body =
        """
        {
          "leadId":"%s",
          "journeyId":"%s",
          "customerId":"%s",
          "suitabilityId":"%s",
          "lob":"LIFE",
          "productClass":"SAVINGS",
          "mode":"MULTI",
          "category":"PREMIUM",
          "requestedCover":500000,
          "premiumAmount":25000,
          "members":[{
            "role":"LIFE_ASSURED","sequence":1,"dob":"1990-04-12","gender":"MALE",
            "tobacco":false,"annualIncome":1500000,"pincode":"400001"
          }],
          "preferences":{"premiumFrequency":"HALF_YEARLY","policyTerm":15,"premiumPayingTerm":10}
        }
        """
            .formatted(leadId, journeyId, customerId, suitabilityId);

    MvcResult created =
        mvc.perform(
                post("/internal/v1/quotes")
                    .header("X-Actor-Id", "RM-1001")
                    .header("Idempotency-Key", "journey:" + journeyId + ":quote:1")
                    .header("X-Correlation-Id", "corr-func-033")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.state").value("IN_PROGRESS"))
            .andReturn();

    String quoteId =
        com.jayway.jsonpath.JsonPath.read(created.getResponse().getContentAsString(), "$.quoteId");

    HUB.verify(
        postRequestedFor(urlEqualTo("/internal/v1/provider-requests"))
            .withHeader(
                InternalCallerAuth.HEADER, equalTo("local-integrationhub-quotation-internal-key"))
            .withHeader("Idempotency-Key", equalTo("quote:" + quoteId + ":submit"))
            .withRequestBody(containing("\"productClass\":\"SAVINGS\""))
            .withRequestBody(containing("\"premiumAmount\":25000"))
            .withRequestBody(containing("\"premiumFrequency\":\"HALF_YEARLY\""))
            .withRequestBody(containing("\"policyTerm\":15"))
            .withRequestBody(containing("\"premiumPayingTerm\":10")));

    mvc.perform(get("/internal/v1/quotes/" + quoteId).header("X-Actor-Id", "RM-1001"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.state").value("QUOTED"))
        .andExpect(jsonPath("$.recordSource").value("WORKING"))
        .andExpect(jsonPath("$.offers[0].coverTerm").value(15))
        .andExpect(jsonPath("$.offers[0].premiumPayingTerm").value(10))
        .andExpect(jsonPath("$.offers[0].premiumFrequency").value("HALF_YEARLY"))
        .andExpect(jsonPath("$.offers[0].offerState").value("QUOTED"));

    HUB.verify(
        1,
        postRequestedFor(urlPathEqualTo("/internal/v1/provider-requests/" + PROVIDER_REQUEST_ID)));
  }
}
