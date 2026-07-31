package com.bank.insurance.onesb;

import com.bank.common.error.ErrorCodes;
import com.bank.insurance.onesb.domain.model.JobStatus;
import com.bank.insurance.onesb.domain.model.Lob;
import com.bank.insurance.onesb.domain.model.QuoteJob;
import com.bank.insurance.onesb.domain.model.QuoteOffer;
import com.bank.insurance.onesb.domain.port.outbound.JobStorePort;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * FUNC-004 IT: GET /v1/proposals/schema — happy schema, cache hit, quote expired, upstream 5xx.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Tag("FUNC-004")
@Tag("integration")
class ProposalSchemaIT {

    private static final String TERM_SCHEMA_PATH = "/insurance/lifeterm/v1/proposal/form";

    private static final WireMockServer ONESB = new WireMockServer(wireMockConfig().dynamicPort());

    static {
        ONESB.start();
    }

    @AfterAll
    static void stopWireMocks() {
        ONESB.stop();
    }

    @DynamicPropertySource
    static void bindWireMockBaseUrls(DynamicPropertyRegistry registry) {
        registry.add("onesb.client.base-url", ONESB::baseUrl);
        registry.add("insurance.proposals.schema-cache-ttl-seconds", () -> "3600");
        // Persistence not needed — JobStorePort mocked for expiry cases
        registry.add("bank.persistence.base-url", () -> "http://localhost:9");
    }

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JobStorePort jobStorePort;

    @BeforeEach
    void resetStubs() {
        ONESB.resetAll();
    }

    @Test
    void ac1_happyPath_returnsDynamicSchema() throws Exception {
        ONESB.stubFor(get(urlPathEqualTo(TERM_SCHEMA_PATH))
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
                        .param("lob", "TERM")
                        .param("productCode", "T1")
                        .param("manufacturerId", "HDFC")
                        .param("version", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lob", is("TERM")))
                .andExpect(jsonPath("$.productCode", is("T1")))
                .andExpect(jsonPath("$.manufacturerId", is("HDFC")))
                .andExpect(jsonPath("$.version", is("1")))
                .andExpect(jsonPath("$.fields.fieldGroups[0].name", is("personal")))
                .andExpect(jsonPath("$.fields.fieldGroups[0].fields[0].id", is("pan")));

        ONESB.verify(exactly(1), getRequestedFor(urlPathEqualTo(TERM_SCHEMA_PATH)));
    }

    @Test
    void ac1_cacheHit_doesNotCallOneSbAgain() throws Exception {
        String product = "CACHE-PROD-" + System.nanoTime();
        ONESB.stubFor(get(urlPathEqualTo(TERM_SCHEMA_PATH))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"fieldGroups\":[],\"cached\":true}")));

        mockMvc.perform(MockMvcRequestBuilders.get("/v1/proposals/schema")
                        .param("lob", "TERM")
                        .param("productCode", product)
                        .param("manufacturerId", "ICICI")
                        .param("version", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fields.cached", is(true)));

        mockMvc.perform(MockMvcRequestBuilders.get("/v1/proposals/schema")
                        .param("lob", "TERM")
                        .param("productCode", product)
                        .param("manufacturerId", "ICICI")
                        .param("version", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fields.cached", is(true)));

        ONESB.verify(exactly(1), getRequestedFor(urlPathEqualTo(TERM_SCHEMA_PATH)));
    }

    @Test
    void ac2_quoteExpired_timeout_returns410() throws Exception {
        when(jobStorePort.findQuoteJob("job-timeout")).thenReturn(Optional.of(new QuoteJob(
                "job-timeout", JobStatus.TIMEOUT, "POLL_TIMEOUT", Lob.TERM, "j-1",
                List.of(), List.of(), Instant.now(), Instant.now(), null)));

        mockMvc.perform(MockMvcRequestBuilders.get("/v1/proposals/schema")
                        .param("lob", "TERM")
                        .param("productCode", "T1")
                        .param("manufacturerId", "HDFC")
                        .param("quoteJobId", "job-timeout"))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.code", is(ErrorCodes.QUOTE_EXPIRED)))
                .andExpect(jsonPath("$.retryable", is(false)));

        ONESB.verify(exactly(0), getRequestedFor(urlPathEqualTo(TERM_SCHEMA_PATH)));
    }

    @Test
    void ac2_quoteExpired_missingJob_returns410() throws Exception {
        when(jobStorePort.findQuoteJob("job-missing")).thenReturn(Optional.empty());

        mockMvc.perform(MockMvcRequestBuilders.get("/v1/proposals/schema")
                        .param("lob", "TERM")
                        .param("productCode", "T1")
                        .param("manufacturerId", "HDFC")
                        .param("quoteJobId", "job-missing"))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.code", is(ErrorCodes.QUOTE_EXPIRED)));
    }

    @Test
    void ac2_quoteExpired_failed_returns410() throws Exception {
        when(jobStorePort.findQuoteJob("job-failed")).thenReturn(Optional.of(new QuoteJob(
                "job-failed", JobStatus.FAILED, "upstream", Lob.TERM, "j-1",
                List.of(), List.of(), Instant.now(), Instant.now(), null)));

        mockMvc.perform(MockMvcRequestBuilders.get("/v1/proposals/schema")
                        .param("lob", "TERM")
                        .param("quoteJobId", "job-failed"))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.code", is(ErrorCodes.QUOTE_EXPIRED)));
    }

    @Test
    void ac3_upstream5xx_returnsUpstreamUnavailable_retryable() throws Exception {
        String product = "UP5XX-" + System.nanoTime();
        ONESB.stubFor(get(urlPathEqualTo(TERM_SCHEMA_PATH))
                .willReturn(aResponse().withStatus(503).withBody("unavailable")));

        mockMvc.perform(MockMvcRequestBuilders.get("/v1/proposals/schema")
                        .param("lob", "TERM")
                        .param("productCode", product)
                        .param("manufacturerId", "HDFC")
                        .param("version", "1"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code", is(ErrorCodes.UPSTREAM_UNAVAILABLE)))
                .andExpect(jsonPath("$.retryable", is(true)));
    }

    @Test
    void quoteJobId_withCompletedOffers_allowsSchemaFetch() throws Exception {
        String product = "OK-JOB-" + System.nanoTime();
        QuoteOffer offer = new QuoteOffer(
                "off-1", "HDFC", null, "T1", "Term",
                new BigDecimal("1000"), "YEARLY", new BigDecimal("500000"),
                false, "AVAILABLE", null
        );
        when(jobStorePort.findQuoteJob("job-ok")).thenReturn(Optional.of(new QuoteJob(
                "job-ok", JobStatus.COMPLETED, null, Lob.TERM, "j-1",
                List.of(offer), List.of(), Instant.now(), Instant.now(), null)));
        ONESB.stubFor(get(urlPathEqualTo(TERM_SCHEMA_PATH))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"ok\":true}")));

        mockMvc.perform(MockMvcRequestBuilders.get("/v1/proposals/schema")
                        .param("lob", "TERM")
                        .param("productCode", product)
                        .param("manufacturerId", "HDFC")
                        .param("quoteJobId", "job-ok"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fields.ok", is(true)));
    }
}
