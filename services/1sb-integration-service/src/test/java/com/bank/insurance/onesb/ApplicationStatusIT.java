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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * FUNC-009 IT: {@code GET /v1/status/{applicationNumber}} — happy path normalisation,
 * not-found, upstream 5xx.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Tag("FUNC-009")
@Tag("integration")
class ApplicationStatusIT {

    private static final String STATUS_PATH = "/LifeTerm/prostat/";

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
        registry.add("bank.persistence.base-url", () -> "http://localhost:9");
    }

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuditEventPublisher auditEventPublisher;

    @BeforeEach
    void resetStubs() {
        ONESB.resetAll();
    }

    @Test
    void ac1_happyPath_normalisesStatus_returnsManufacturerSubStatus_andAudits() throws Exception {
        ONESB.stubFor(post(urlEqualTo(STATUS_PATH))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
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

        mockMvc.perform(MockMvcRequestBuilders.get("/v1/status/APP-1")
                        .param("lob", "TERM")
                        .param("insurerCode", "HDFC")
                        .header("X-Actor-Id", "rm-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applicationNumber", is("APP-1")))
                .andExpect(jsonPath("$.bankStatus", is("UW_IN_PROGRESS")))
                .andExpect(jsonPath("$.manufacturerSubStatus", is("Medical review in progress")))
                .andExpect(jsonPath("$.rawStatus").doesNotExist());

        ONESB.verify(exactly(1), postRequestedFor(urlEqualTo(STATUS_PATH))
                .withRequestBody(matchingJsonPath("$.distributorID",
                        com.github.tomakehurst.wiremock.client.WireMock.equalTo("TEST_DIST"))));

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditEventPublisher, atLeastOnce()).publish(captor.capture());
        assertThat(captor.getAllValues())
                .anyMatch(e -> AuditActions.APPLICATION_STATUS_CHECKED.equals(e.getAction()));
    }

    @Test
    void ac2_notFound_returns404() throws Exception {
        ONESB.stubFor(post(urlEqualTo(STATUS_PATH))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"manufacturer\":[]}")));

        mockMvc.perform(MockMvcRequestBuilders.get("/v1/status/APP-missing")
                        .param("lob", "TERM")
                        .param("insurerCode", "HDFC"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is(ErrorCodes.RESOURCE_NOT_FOUND)));
    }

    @Test
    void ac3_upstream5xx_returnsUpstreamUnavailable_retryable() throws Exception {
        ONESB.stubFor(post(urlEqualTo(STATUS_PATH))
                .willReturn(aResponse().withStatus(503).withBody("down")));

        mockMvc.perform(MockMvcRequestBuilders.get("/v1/status/APP-1")
                        .param("lob", "TERM")
                        .param("insurerCode", "HDFC"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code", is(ErrorCodes.UPSTREAM_UNAVAILABLE)))
                .andExpect(jsonPath("$.retryable", is(true)));
    }
}
