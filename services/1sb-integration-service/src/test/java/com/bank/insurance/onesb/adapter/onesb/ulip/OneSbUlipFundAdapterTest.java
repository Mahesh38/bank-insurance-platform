package com.bank.insurance.onesb.adapter.onesb.ulip;

import com.bank.insurance.onesb.adapter.onesb.client.OneSbHttpClient;
import com.bank.insurance.onesb.domain.model.UlipFundResult;
import com.bank.insurance.onesb.lob.life.payload.LifeQuoteRequest;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("FUNC-027")
class OneSbUlipFundAdapterTest {

    private static final String LIST_PATH = "/insurance/lifesave/v1/fund/list";
    private static final String PERF_PATH = "/insurance/lifesave/v1/fund/performance";

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    private OneSbUlipFundAdapter adapter;

    @BeforeEach
    void setUp() {
        wireMock.resetAll();
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(10));
        RestClient restClient = RestClient.builder()
                .baseUrl(wireMock.baseUrl())
                .requestFactory(factory)
                .build();
        adapter = new OneSbUlipFundAdapter(new OneSbHttpClient(restClient));
    }

    @Test
    void listFunds_postsDocumentedPath_notGuessedUlipList() {
        wireMock.stubFor(post(urlEqualTo(LIST_PATH))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"reqId\":\"REQ-LIST\",\"data\":{\"funds\":[{\"code\":\"EQ\"}]}}")));

        LifeQuoteRequest body = sampleBody(false);
        UlipFundResult result = adapter.listFunds(body);

        assertThat(result.reqId()).isEqualTo("REQ-LIST");
        assertThat(result.data()).containsKey("funds");
        wireMock.verify(exactly(1), postRequestedFor(urlEqualTo(LIST_PATH))
                .withRequestBody(matchingJsonPath("$.product.productType", equalTo("LifeSave")))
                .withRequestBody(matchingJsonPath("$.product.savingsProductType[0]", equalTo("ULIP"))));
        wireMock.verify(0, postRequestedFor(urlPathEqualTo("/insurance/lifesave/v1/quote/ulipList")));
        wireMock.verify(0, postRequestedFor(urlPathEqualTo("/insurance/lifesave/v1/quote/ulipPerformance")));
    }

    @Test
    void fundPerformance_postsDocumentedPath_withProductPin() {
        wireMock.stubFor(post(urlEqualTo(PERF_PATH))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"reqId\":\"REQ-PERF\",\"data\":{\"performance\":[]}}")));

        UlipFundResult result = adapter.fundPerformance(sampleBody(true));

        assertThat(result.reqId()).isEqualTo("REQ-PERF");
        wireMock.verify(exactly(1), postRequestedFor(urlEqualTo(PERF_PATH))
                .withRequestBody(matchingJsonPath(
                        "$.product.insuranceAndProducts[0].insuranceCompanyCode", equalTo("BALIC")))
                .withRequestBody(matchingJsonPath(
                        "$.product.insuranceAndProducts[0].productCode[0]", equalTo("345"))));
        wireMock.verify(0, postRequestedFor(urlPathEqualTo("/insurance/lifesave/v1/quote/ulipPerformance")));
    }

    @Test
    void wrap_nullBodyAndRequestIdFallback() {
        assertThat(OneSbUlipFundAdapter.wrap(null).data()).isEmpty();
        UlipFundResult fromAlias = OneSbUlipFundAdapter.wrap(java.util.Map.of("requestId", "R9", "ok", true));
        assertThat(fromAlias.reqId()).isEqualTo("R9");
        UlipFundResult blankReq = OneSbUlipFundAdapter.wrap(java.util.Map.of("reqId", " ", "requestId", ""));
        assertThat(blankReq.reqId()).isNull();
        assertThat(new UlipFundResult("x", null).data()).isEmpty();
    }

    private static LifeQuoteRequest sampleBody(boolean pin) {
        LifeQuoteRequest.Product product = LifeQuoteRequest.Product.saving("LifeSave", List.of("ULIP"));
        if (pin) {
            product = product.withPin(
                    List.of(new LifeQuoteRequest.InsuranceAndProduct("BALIC", List.of("345"))),
                    null, null, null, null, null, null, null);
        }
        return new LifeQuoteRequest(
                "Multi-Quote",
                "Sum Assured",
                "withoutBI",
                "Yes",
                new LifeQuoteRequest.AdditionalSetup("INR", "IN"),
                new LifeQuoteRequest.Distributor("TEST_DIST", "109337", "B2B", "Online"),
                new LifeQuoteRequest.PersonalInformation(List.of()),
                product
        );
    }
}
