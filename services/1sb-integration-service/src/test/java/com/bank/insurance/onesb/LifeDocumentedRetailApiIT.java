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
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * FUNC-027: documented Life retail remainder — Product UI Data + ULIP fund list/performance.
 * Asserts portal paths only; never guessed {@code /quote/ulipList}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Tag("FUNC-027")
@Tag("integration")
class LifeDocumentedRetailApiIT {

    private static final String UI_PATH = "/insurance/lifeterm/v1/master/getproductuidata";
    private static final String LIST_PATH = "/insurance/lifesave/v1/fund/list";
    private static final String PERF_PATH = "/insurance/lifesave/v1/fund/performance";

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
        registry.add("onesb.distributor-id", () -> "TEST_DIST");
    }

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void resetStubs() {
        ONESB.resetAll();
        PERSISTENCE.resetAll();
    }

    @Test
    void getProductUiData_callsDocumentedTermPath() throws Exception {
        ONESB.stubFor(get(urlPathEqualTo(UI_PATH))
                .withQueryParam("productId", equalTo("345"))
                .withQueryParam("manufacturerId", equalTo("BALIC"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"reqId\":\"REQ-UI\",\"data\":{\"ok\":true}}")));

        mockMvc.perform(MockMvcRequestBuilders.get("/v1/products/ui-data")
                        .param("productId", "345")
                        .param("manufacturerId", "BALIC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId", is("345")))
                .andExpect(jsonPath("$.reqId", is("REQ-UI")))
                .andExpect(jsonPath("$.data.ok", is(true)));

        ONESB.verify(exactly(1), getRequestedFor(urlPathEqualTo(UI_PATH))
                .withQueryParam("productId", equalTo("345"))
                .withQueryParam("manufacturerId", equalTo("BALIC")));
        ONESB.verify(0, getRequestedFor(urlPathEqualTo(
                "/insurance/lifesave/v1/master/getproductuidata")));
    }

    @Test
    void postFundList_requiresIdempotencyKey_andPostsDocumentedPath() throws Exception {
        ONESB.stubFor(post(urlEqualTo(LIST_PATH))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"reqId\":\"REQ-LIST\",\"data\":{\"funds\":[]}}")));

        mockMvc.perform(MockMvcRequestBuilders.post("/v1/ulip/funds/list")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(listBody()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is(ErrorCodes.MISSING_IDEMPOTENCY_KEY)));
        ONESB.verify(0, postRequestedFor(urlEqualTo(LIST_PATH)));

        mockMvc.perform(MockMvcRequestBuilders.post("/v1/ulip/funds/list")
                        .header("Idempotency-Key", "idem-list-" + UUID.randomUUID())
                        .header("X-Actor-Id", "rm-ulip")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(listBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reqId", is("REQ-LIST")));

        ONESB.verify(exactly(1), postRequestedFor(urlEqualTo(LIST_PATH))
                .withRequestBody(matchingJsonPath("$.distributor.distributorID", equalTo("TEST_DIST")))
                .withRequestBody(matchingJsonPath("$.product.productType", equalTo("LifeSave")))
                .withRequestBody(matchingJsonPath("$.product.savingsProductType[0]", equalTo("ULIP"))));
        ONESB.verify(0, postRequestedFor(urlPathEqualTo("/insurance/lifesave/v1/quote/ulipList")));
    }

    @Test
    void postFundPerformance_missingPin_returns422_noUpstream() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post("/v1/ulip/funds/performance")
                        .header("Idempotency-Key", "idem-perf-miss-" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(listBody()))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code", is(ErrorCodes.VALIDATION_ERROR)));
        ONESB.verify(0, postRequestedFor(urlEqualTo(PERF_PATH)));
    }

    @Test
    void postFundPerformance_withPin_postsDocumentedPath() throws Exception {
        ONESB.stubFor(post(urlEqualTo(PERF_PATH))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"reqId\":\"REQ-PERF\",\"data\":{\"performance\":[]}}")));

        mockMvc.perform(MockMvcRequestBuilders.post("/v1/ulip/funds/performance")
                        .header("Idempotency-Key", "idem-perf-" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(performanceBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reqId", is("REQ-PERF")));

        ONESB.verify(exactly(1), postRequestedFor(urlEqualTo(PERF_PATH))
                .withRequestBody(matchingJsonPath(
                        "$.product.insuranceAndProducts[0].insuranceCompanyCode", equalTo("BALIC"))));
        ONESB.verify(0, postRequestedFor(urlPathEqualTo(
                "/insurance/lifesave/v1/quote/ulipPerformance")));
    }

    private static String listBody() {
        return """
                {
                  "lob": "ULIP",
                  "sumAssured": 500000,
                  "members": [{ "dob": "1990-01-15", "gender": "M", "annualIncome": 1000000 }],
                  "distribution": { "agentId": "109337" }
                }
                """;
    }

    private static String performanceBody() {
        return """
                {
                  "lob": "ULIP",
                  "sumAssured": 500000,
                  "members": [{ "dob": "1990-01-15", "gender": "M", "annualIncome": 1000000 }],
                  "distribution": { "agentId": "109337" },
                  "selection": { "insurerCode": "BALIC", "productCodes": ["345"] }
                }
                """;
    }
}
