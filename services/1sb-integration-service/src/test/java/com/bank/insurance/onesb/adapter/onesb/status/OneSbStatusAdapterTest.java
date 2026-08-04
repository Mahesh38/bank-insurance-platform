package com.bank.insurance.onesb.adapter.onesb.status;

import com.bank.common.secrets.SecretProvider;
import com.bank.insurance.onesb.adapter.onesb.client.OneSbHttpClient;
import com.bank.insurance.onesb.domain.model.OneSbApplicationStatusResult;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@Tag("FUNC-009")
class OneSbStatusAdapterTest {

    private static final WireMockServer ONESB = new WireMockServer(wireMockConfig().dynamicPort());

    static {
        ONESB.start();
    }

    @AfterAll
    static void stop() {
        ONESB.stop();
    }

    private OneSbStatusAdapter adapter;

    @BeforeEach
    void setUp() {
        ONESB.resetAll();
        RestClient restClient = RestClient.builder()
                .baseUrl(ONESB.baseUrl())
                .requestFactory(http1Factory())
                .build();
        SecretProvider secretProvider = org.mockito.Mockito.mock(SecretProvider.class);
        when(secretProvider.getDistributorId()).thenReturn("BCIBL");
        adapter = new OneSbStatusAdapter(new OneSbHttpClient(restClient), secretProvider);
    }

    private static JdkClientHttpRequestFactory http1Factory() {
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(10));
        return factory;
    }

    @Test
    void getStatus_happyPath_parsesNestedManufacturerProductShape() {
        ONESB.stubFor(post(urlEqualTo("/LifeTerm/prostat/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "applicationNo": "APP-1",
                                  "manufacturer": [{
                                    "insuranceCompanyCode": "HDFC",
                                    "product": [{
                                      "productCode": "T1",
                                      "applicationStatus": {
                                        "applicationStatus": "REQUIREMENTS_PENDING",
                                        "manufacturerAppStatus": "Income proof pending"
                                      },
                                      "policyDetails": { "policyNo": null }
                                    }]
                                  }]
                                }
                                """)));

        Optional<OneSbApplicationStatusResult> result = adapter.getStatus("APP-1", "HDFC", "T1");

        assertThat(result).isPresent();
        assertThat(result.get().rawStatus()).isEqualTo("REQUIREMENTS_PENDING");
        assertThat(result.get().subStatus()).isEqualTo("Income proof pending");
        assertThat(result.get().policyNumber()).isNull();

        ONESB.verify(exactly(1), postRequestedFor(urlEqualTo("/LifeTerm/prostat/"))
                .withRequestBody(equalToJson("""
                        {"insuranceCompanyCode":"HDFC","distributorID":"BCIBL","applicationNo":"APP-1","productCode":"T1"}
                        """, true, true)));
    }

    @Test
    void getStatus_emptyManufacturerArray_returnsEmpty() {
        ONESB.stubFor(post(urlEqualTo("/LifeTerm/prostat/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"applicationNo\":\"APP-2\",\"manufacturer\":[]}")));

        Optional<OneSbApplicationStatusResult> result = adapter.getStatus("APP-2", "HDFC", null);

        assertThat(result).isEmpty();
    }

    @Test
    void getStatus_policyIssued_includesPolicyNumber() {
        ONESB.stubFor(post(urlEqualTo("/LifeTerm/prostat/"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "manufacturer": [{
                                    "product": [{
                                      "applicationStatus": {"applicationStatus": "POLICY_ISSUED"},
                                      "policyDetails": {"policyNo": "POL-99"}
                                    }]
                                  }]
                                }
                                """)));

        Optional<OneSbApplicationStatusResult> result = adapter.getStatus("APP-3", "HDFC", null);

        assertThat(result).isPresent();
        assertThat(result.get().policyNumber()).isEqualTo("POL-99");
    }
}
