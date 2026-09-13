package com.bank.insurance.onesb.adapter.onesb.productui;

import com.bank.insurance.onesb.adapter.onesb.client.OneSbHttpClient;
import com.bank.insurance.onesb.domain.model.ProductUiData;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("FUNC-027")
class OneSbProductUiDataAdapterTest {

    private static final String PATH = "/insurance/lifeterm/v1/master/getproductuidata";

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    private OneSbProductUiDataAdapter adapter;

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
        adapter = new OneSbProductUiDataAdapter(new OneSbHttpClient(restClient));
    }

    @Test
    void getProductUiData_usesDocumentedTermPath_andQueryParams() {
        wireMock.stubFor(get(urlPathEqualTo(PATH))
                .withQueryParam("productId", equalTo("345"))
                .withQueryParam("manufacturerId", equalTo("BALIC"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "reqId": "REQ-UI",
                                  "data": { "screens": [{"id": "quote"}] }
                                }
                                """)));

        ProductUiData result = adapter.getProductUiData("345", "BALIC");

        assertThat(result.productId()).isEqualTo("345");
        assertThat(result.manufacturerId()).isEqualTo("BALIC");
        assertThat(result.reqId()).isEqualTo("REQ-UI");
        assertThat(result.data()).containsKey("screens");
        wireMock.verify(exactly(1), getRequestedFor(urlPathEqualTo(PATH))
                .withQueryParam("productId", equalTo("345"))
                .withQueryParam("manufacturerId", equalTo("BALIC")));
        wireMock.verify(0, getRequestedFor(urlPathEqualTo(
                "/insurance/lifesave/v1/master/getproductuidata")));
    }

    @Test
    void wrap_nullBodyAndRequestIdFallback() {
        assertThat(OneSbProductUiDataAdapter.wrap("p", "m", null).data()).isEmpty();
        ProductUiData fromAlias = OneSbProductUiDataAdapter.wrap("p", "m", Map.of("requestId", "R2", "ok", true));
        assertThat(fromAlias.reqId()).isEqualTo("R2");
        assertThat(fromAlias.data()).containsEntry("ok", true);
        ProductUiData blankReq = OneSbProductUiDataAdapter.wrap("p", "m", Map.of("reqId", " ", "requestId", ""));
        assertThat(blankReq.reqId()).isNull();
        assertThat(new ProductUiData("p", "m", "r", null).data()).isEmpty();
    }
}
