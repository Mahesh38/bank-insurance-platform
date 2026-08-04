package com.bank.insurance.onesb.adapter.persistence;

import com.bank.insurance.onesb.domain.model.Lob;
import com.bank.insurance.onesb.domain.model.PaymentSession;
import com.bank.insurance.onesb.domain.model.PaymentStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@Tag("FUNC-007")
class HttpPaymentSessionStoreAdapterTest {

    private static final String BASE = "http://localhost:8081";

    private MockRestServiceServer server;
    private HttpPaymentSessionStoreAdapter adapter;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        adapter = new HttpPaymentSessionStoreAdapter(builder.baseUrl(BASE).build());
    }

    @AfterEach
    void verify() {
        server.verify();
    }

    @Test
    void createSession_postsToPersistenceAndReturnsSession() {
        Instant expiresAt = Instant.parse("2026-08-04T11:00:00Z");
        server.expect(requestTo(BASE + "/internal/v1/payment-sessions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.applicationNumber").value("APP-123"))
                .andExpect(jsonPath("$.lob").value("TERM"))
                .andExpect(jsonPath("$.paymentUrl").value("https://pay.example.com/abc"))
                .andExpect(jsonPath("$.status").value("AWAITING_PAYMENT"))
                .andRespond(withSuccess("""
                        {
                          "sessionId": "sess-1",
                          "jobId": null,
                          "applicationNumber": "APP-123",
                          "lob": "TERM",
                          "paymentUrl": "https://pay.example.com/abc",
                          "redirectUrl": "https://bank.com/return",
                          "status": "AWAITING_PAYMENT",
                          "externalTxnId": "TXN-1",
                          "createdAt": "2026-08-04T10:00:00Z",
                          "expiresAt": "2026-08-04T11:00:00Z",
                          "updatedAt": "2026-08-04T10:00:00Z",
                          "createdByActor": "actor-1"
                        }
                        """, MediaType.APPLICATION_JSON));

        PaymentSession session = adapter.createSession(
                null, "APP-123", Lob.TERM, "https://pay.example.com/abc",
                "https://bank.com/return", PaymentStatus.AWAITING_PAYMENT, "TXN-1",
                expiresAt, "actor-1");

        assertThat(session.sessionId()).isEqualTo("sess-1");
        assertThat(session.status()).isEqualTo(PaymentStatus.AWAITING_PAYMENT);
        assertThat(session.expiresAt()).isEqualTo(expiresAt);
    }

    @Test
    void createSession_emptyResponse_throwsIllegalState() {
        server.expect(requestTo(BASE + "/internal/v1/payment-sessions"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> adapter.createSession(
                        null, "APP-1", Lob.TERM, "https://pay.example.com/x",
                        "https://bank.com/return", PaymentStatus.AWAITING_PAYMENT, null,
                        Instant.now(), "actor-1"))
                .isInstanceOf(IllegalStateException.class);
    }
}
