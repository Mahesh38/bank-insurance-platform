package com.bank.insurance.onesb.adapter.persistence;

import com.bank.insurance.onesb.domain.model.Lob;
import com.bank.insurance.onesb.domain.model.PaymentSession;
import com.bank.insurance.onesb.domain.model.PaymentStatus;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.net.http.HttpClient;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("FUNC-007")
class HttpPaymentSessionStoreAdapterTest {

    private static final WireMockServer PERSISTENCE = new WireMockServer(wireMockConfig().dynamicPort());

    static {
        PERSISTENCE.start();
    }

    @AfterAll
    static void stop() {
        PERSISTENCE.stop();
    }

    private HttpPaymentSessionStoreAdapter adapter;

    @BeforeEach
    void setUp() {
        PERSISTENCE.resetAll();
        HttpClient httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();
        RestClient client = RestClient.builder()
                .baseUrl(PERSISTENCE.baseUrl())
                .requestFactory(new JdkClientHttpRequestFactory(httpClient))
                .build();
        adapter = new HttpPaymentSessionStoreAdapter(client);
    }

    @Test
    void save_postsPaymentSession_andReturnsSessionId() {
        PERSISTENCE.stubFor(post(urlEqualTo("/internal/v1/payment-sessions"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "sessionId": "pay-stored-1",
                                  "jobId": "job-1",
                                  "applicationNumber": "APP-1",
                                  "lob": "TERM",
                                  "paymentUrl": "https://pay.test/u",
                                  "redirectUrl": "https://bank.test/cb",
                                  "status": "AWAITING_PAYMENT",
                                  "createdAt": "2026-07-30T12:00:00Z",
                                  "expiresAt": "2026-07-30T12:15:00Z",
                                  "updatedAt": "2026-07-30T12:00:00Z",
                                  "createdByActor": "rm-1"
                                }
                                """)));

        PaymentSession session = new PaymentSession(
                "pay-stored-1", "job-1", "APP-1", Lob.TERM,
                "https://pay.test/u", "https://bank.test/cb",
                PaymentStatus.AWAITING_PAYMENT, "txn-1",
                Instant.parse("2026-07-30T12:00:00Z"),
                Instant.parse("2026-07-30T12:15:00Z"));

        String id = adapter.save(session, "rm-1");
        assertThat(id).isEqualTo("pay-stored-1");

        PERSISTENCE.verify(postRequestedFor(urlEqualTo("/internal/v1/payment-sessions"))
                .withRequestBody(containing("\"applicationNumber\":\"APP-1\""))
                .withRequestBody(containing("\"paymentUrl\":\"https://pay.test/u\"")));
    }
}
