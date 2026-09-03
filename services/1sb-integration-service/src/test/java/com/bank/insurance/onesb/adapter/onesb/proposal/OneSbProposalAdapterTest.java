package com.bank.insurance.onesb.adapter.onesb.proposal;

import com.bank.insurance.onesb.TestErrors;

import com.bank.common.error.ErrorCodes;
import com.bank.common.error.ServiceException;
import com.bank.insurance.onesb.adapter.onesb.client.OneSbHttpClient;
import com.bank.common.domain.Lob;
import com.bank.insurance.onesb.domain.model.OneSbProposalSubmitResult;
import com.bank.common.domain.ProposalSchema;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("FUNC-004")
class OneSbProposalAdapterTest {

    private static final WireMockServer ONESB = new WireMockServer(wireMockConfig().dynamicPort());

    static {
        ONESB.start();
    }

    @AfterAll
    static void stop() {
        ONESB.stop();
    }

    private OneSbProposalAdapter adapter;

    @BeforeEach
    void setUp() {
        ONESB.resetAll();
        RestClient restClient = RestClient.builder()
                .baseUrl(ONESB.baseUrl())
                .requestFactory(http1Factory())
                .build();
        adapter = new OneSbProposalAdapter(new OneSbHttpClient(restClient, TestErrors.ONESB), 3600L);
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
    void getSchema_passThroughFields() {
        String path = "/insurance/lifeterm/v1/proposal?productId=T1&manufacturerId=HDFC&version=1";
        ONESB.stubFor(get(urlEqualTo(path))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"fieldGroups\":[{\"name\":\"kyc\"}]}")));

        ProposalSchema schema = adapter.getSchema(Lob.TERM, "T1", "HDFC", "1", path);

        assertThat(schema.lob()).isEqualTo(Lob.TERM);
        assertThat(schema.fields()).containsKey("fieldGroups");
        ONESB.verify(exactly(1), getRequestedFor(urlEqualTo(path)));
    }

    @Test
    void getSchema_cacheHit_skipsSecondHttpCall() {
        String path = "/insurance/lifeterm/v1/proposal?productId=C1&manufacturerId=M1&version=v";
        ONESB.stubFor(get(urlEqualTo(path))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"n\":1}")));

        adapter.getSchema(Lob.TERM, "C1", "M1", "v", path);
        adapter.getSchema(Lob.TERM, "C1", "M1", "v", path);

        ONESB.verify(exactly(1), getRequestedFor(urlEqualTo(path)));
    }

    @Test
    void getSchema_upstream5xx_throwsRetryableUpstreamUnavailable() {
        String path = "/insurance/lifeterm/v1/proposal?productId=ERR";
        ONESB.stubFor(get(urlEqualTo(path))
                .willReturn(aResponse().withStatus(503).withBody("down")));

        assertThatThrownBy(() -> adapter.getSchema(Lob.TERM, "ERR", null, null, path))
                .isInstanceOf(ServiceException.class)
                .satisfies(ex -> {
                    ServiceException se = (ServiceException) ex;
                    assertThat(se.getErrorResponse().getCode()).isEqualTo(ErrorCodes.UPSTREAM_UNAVAILABLE);
                    assertThat(se.isRetryable()).isTrue();
                });
    }

    @Test
    @Tag("FUNC-005")
    void submit_withReqId_returnsIncompleteResult() {
        String path = "/insurance/lifeterm/v1/proposal";
        ONESB.stubFor(post(urlEqualTo(path))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"reqId\":\"REQ-42\",\"data\":{}}")));

        OneSbProposalSubmitResult result =
                adapter.submit("job-1", path, Map.of("distributor", Map.of("distributorID", "BCIBL")));

        assertThat(result.externalReqId()).isEqualTo("REQ-42");
        assertThat(result.complete()).isFalse();
        ONESB.verify(exactly(1), postRequestedFor(urlEqualTo(path))
                .withRequestBody(equalToJson("{\"distributor\":{\"distributorID\":\"BCIBL\"}}")));
    }

    @Test
    @Tag("FUNC-005")
    void submit_withApplicationNumber_returnsCompleteResult() {
        String path = "/insurance/lifeterm/v1/proposal";
        ONESB.stubFor(post(urlEqualTo(path))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"applicationNumber\":\"APP-7\",\"reqId\":\"REQ-7\"}")));

        OneSbProposalSubmitResult result = adapter.submit("job-1", path, Map.of());

        assertThat(result.applicationNumber()).isEqualTo("APP-7");
        assertThat(result.complete()).isTrue();
    }

    @Test
    @Tag("FUNC-005")
    void submit_business422_mapsToProposalRejected() {
        String path = "/insurance/lifeterm/v1/proposal";
        ONESB.stubFor(post(urlEqualTo(path))
                .willReturn(aResponse()
                        .withStatus(422)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "errors": [{
                                    "field": "proposer.panNumber",
                                    "message": "Invalid PAN",
                                    "code": "PAN_INVALID"
                                  }]
                                }
                                """)));

        assertThatThrownBy(() -> adapter.submit("job-1", path, Map.of()))
                .isInstanceOf(ServiceException.class)
                .satisfies(ex -> {
                    ServiceException se = (ServiceException) ex;
                    assertThat(se.getHttpStatus()).isEqualTo(422);
                    assertThat(se.getErrorResponse().getCode()).isEqualTo(ErrorCodes.PROPOSAL_REJECTED);
                    assertThat(se.isRetryable()).isFalse();
                });
    }
}
