package com.bank.insurance.onesb;

import com.bank.common.audit.AuditActions;
import com.bank.common.audit.AuditEvent;
import com.bank.common.audit.AuditEventPublisher;
import com.bank.common.audit.AuditOutcomes;
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
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * FUNC-009 IT: {@code GET /v1/status/{applicationNumber}} — BankStage mapping, 404, audit.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Tag("FUNC-009")
@Tag("integration")
class ApplicationStatusIT {

    private static final String PROSTAT_PATH = "/LifeTerm/prostat/";

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
    }

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuditEventPublisher auditEventPublisher;

    @BeforeEach
    void resetStubs() {
        ONESB.resetAll();
        PERSISTENCE.resetAll();
    }

    @Test
    void ac1_happyPath_returnsBankStageAndManufacturerSubstatus() throws Exception {
        ONESB.stubFor(post(urlEqualTo(PROSTAT_PATH))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "applicationNo": "APP-STATUS-1",
                                  "manufacturer": [
                                    {
                                      "insuranceCompanyCode": "XYZ",
                                      "product": [
                                        {
                                          "productCode": "TERM_LIFE",
                                          "applicationStatus": {
                                            "applicationStatus": "REQUIREMENTS_PENDING",
                                            "applicationStatusDesc": "Pending requirements",
                                            "manufacturerAppStatus": "DOC_WAIT",
                                            "manufacturerAppStatusDesc": "Waiting for income proof"
                                          },
                                          "policyDetails": {
                                            "policyNo": null
                                          }
                                        }
                                      ]
                                    }
                                  ]
                                }
                                """)));

        mockMvc.perform(MockMvcRequestBuilders.get("/v1/status/APP-STATUS-1")
                        .param("lob", "TERM")
                        .param("insuranceCompanyCode", "XYZ")
                        .header("X-Actor-Id", "rm-status-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applicationNumber", is("APP-STATUS-1")))
                .andExpect(jsonPath("$.bankStatus", is("UW_REQUIREMENTS")))
                .andExpect(jsonPath("$.manufacturerAppStatus", is("DOC_WAIT")))
                .andExpect(jsonPath("$.manufacturerAppStatusDesc", is("Waiting for income proof")))
                .andExpect(jsonPath("$.onesbStatus").doesNotExist())
                .andExpect(jsonPath("$.policyNumber").value(nullValue()));

        ONESB.verify(exactly(1), postRequestedFor(urlEqualTo(PROSTAT_PATH))
                .withRequestBody(containing("\"applicationNo\":\"APP-STATUS-1\""))
                .withRequestBody(containing("\"insuranceCompanyCode\":\"XYZ\""))
                .withRequestBody(containing("\"distributorID\"")));
    }

    @Test
    void ac3_emptyManufacturer_returns404ResourceNotFound() throws Exception {
        ONESB.stubFor(post(urlEqualTo(PROSTAT_PATH))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "applicationNo": "APP-MISSING",
                                  "manufacturer": []
                                }
                                """)));

        mockMvc.perform(MockMvcRequestBuilders.get("/v1/status/APP-MISSING")
                        .param("lob", "TERM")
                        .param("insuranceCompanyCode", "XYZ"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is(ErrorCodes.RESOURCE_NOT_FOUND)));
    }

    @Test
    void ac4_auditsApplicationStatusChecked() throws Exception {
        ONESB.stubFor(post(urlEqualTo(PROSTAT_PATH))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "applicationStatus": "POLICY_ISSUED",
                                  "manufacturerAppStatus": "ACTIVE",
                                  "manufacturerAppStatusDesc": "Policy live",
                                  "policyNo": "POL-AUDIT-1"
                                }
                                """)));

        mockMvc.perform(MockMvcRequestBuilders.get("/v1/status/APP-AUDIT")
                        .param("lob", "TERM")
                        .param("insuranceCompanyCode", "XYZ")
                        .param("policyNo", "POL-AUDIT-1")
                        .header("X-Actor-Id", "rm-audit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bankStatus", is("ISSUED")))
                .andExpect(jsonPath("$.policyNumber", is("POL-AUDIT-1")));

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditEventPublisher, atLeastOnce()).publish(captor.capture());
        assertThat(captor.getAllValues())
                .anyMatch(e -> AuditActions.APPLICATION_STATUS_CHECKED.equals(e.getAction())
                        && AuditOutcomes.SUCCESS.equals(e.getOutcome())
                        && "APP-AUDIT".equals(e.getResourceId())
                        && e.getMetadata() != null
                        && "ISSUED".equals(e.getMetadata().get("bankStatus"))
                        && "APP-AUDIT".equals(e.getMetadata().get("applicationNumber")));
    }
}
