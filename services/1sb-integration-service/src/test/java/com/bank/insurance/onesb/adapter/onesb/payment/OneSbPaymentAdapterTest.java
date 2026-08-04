package com.bank.insurance.onesb.adapter.onesb.payment;

import com.bank.common.secrets.SecretProvider;
import com.bank.insurance.onesb.adapter.onesb.client.OneSbHttpClient;
import com.bank.insurance.onesb.domain.command.CreatePaymentCommand;
import com.bank.insurance.onesb.domain.model.Lob;
import com.bank.insurance.onesb.domain.model.OneSbPaymentUrlResult;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@Tag("FUNC-007")
class OneSbPaymentAdapterTest {

    private static final Instant NOW = Instant.parse("2026-08-04T10:00:00Z");

    private static final WireMockServer ONESB = new WireMockServer(wireMockConfig().dynamicPort());

    static {
        ONESB.start();
    }

    @AfterAll
    static void stop() {
        ONESB.stop();
    }

    private OneSbPaymentAdapter adapter;

    @BeforeEach
    void setUp() {
        ONESB.resetAll();
        RestClient restClient = RestClient.builder()
                .baseUrl(ONESB.baseUrl())
                .requestFactory(http1Factory())
                .build();
        SecretProvider secretProvider = org.mockito.Mockito.mock(SecretProvider.class);
        when(secretProvider.getDistributorId()).thenReturn("BCIBL");
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        adapter = new OneSbPaymentAdapter(new OneSbHttpClient(restClient), secretProvider, clock, 1800L);
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
    void createPaymentUrl_happyPath_buildsPayloadAndParsesResponse() {
        ONESB.stubFor(post(urlEqualTo("/v1/payment/url"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"paymentUrl":"https://pay.example.com/abc","transactionId":"TXN-1"}
                                """)));

        OneSbPaymentUrlResult result = adapter.createPaymentUrl(command());

        assertThat(result.paymentUrl()).isEqualTo("https://pay.example.com/abc");
        assertThat(result.externalTxnId()).isEqualTo("TXN-1");
        assertThat(result.expiresAt()).isEqualTo(NOW.plusSeconds(1800));

        ONESB.verify(exactly(1), postRequestedFor(urlEqualTo("/v1/payment/url"))
                .withRequestBody(equalToJson("""
                        {
                          "AdditionalSetup": {"currency":"INR"},
                          "Distributor": {"distributorID":"BCIBL","agentID":"AGT-1"},
                          "insuranceCompanyCode":"HDFC",
                          "productCode":"T1",
                          "applicationNo":"APP-123",
                          "redirectUrl":"https://bank.com/return",
                          "UITrackingRefNo":"j-1",
                          "MemberDetails": {
                            "firstName":"Anil","lastName":"Kumar",
                            "mobileNumber":"9876543210","email":"anil@example.com"
                          },
                          "PaymentDetails": {
                            "premiumPaymentFrequency":"YEARLY","amountToBePaid":12300
                          }
                        }
                        """, true, true)));
    }

    @Test
    void createPaymentUrl_nestedPaymentDetailsUrl_parsed() {
        ONESB.stubFor(post(urlEqualTo("/v1/payment/url"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"PaymentDetails":{"paymentUrl":"https://pay.example.com/nested","txnId":"TXN-2"}}
                                """)));

        OneSbPaymentUrlResult result = adapter.createPaymentUrl(command());

        assertThat(result.paymentUrl()).isEqualTo("https://pay.example.com/nested");
        assertThat(result.externalTxnId()).isEqualTo("TXN-2");
    }

    @Test
    void sendPaymentIntimation_notYetImplemented_throwsUnsupported() {
        assertThatThrownBy(() -> adapter.sendPaymentIntimation("sess-1"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    private static CreatePaymentCommand command() {
        return new CreatePaymentCommand(
                Lob.TERM, "APP-123", null, "HDFC", "T1", "https://bank.com/return", "j-1", "INR",
                new CreatePaymentCommand.Payer("Anil", "Kumar", "9876543210", "anil@example.com"),
                new BigDecimal("12300"), "YEARLY", "E123", "AGT-1", "actor-1");
    }
}
