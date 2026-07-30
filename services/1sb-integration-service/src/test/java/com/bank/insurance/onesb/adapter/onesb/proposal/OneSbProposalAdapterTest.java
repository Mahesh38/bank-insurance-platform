package com.bank.insurance.onesb.adapter.onesb.proposal;

import com.bank.common.error.ErrorCodes;
import com.bank.common.error.ServiceException;
import com.bank.insurance.onesb.adapter.onesb.client.OneSbHttpClient;
import com.bank.insurance.onesb.domain.model.Lob;
import com.bank.insurance.onesb.domain.model.ProposalSchema;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
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
        RestClient restClient = RestClient.builder().baseUrl(ONESB.baseUrl()).build();
        adapter = new OneSbProposalAdapter(new OneSbHttpClient(restClient), 3600L);
    }

    @Test
    void getSchema_passThroughFields() {
        String path = "/insurance/lifeterm/v1/proposal/form?productCode=T1&manufacturerId=HDFC&version=1";
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
        String path = "/insurance/lifeterm/v1/proposal/form?productCode=C1&manufacturerId=M1&version=v";
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
        String path = "/insurance/lifeterm/v1/proposal/form?productCode=ERR";
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
}
