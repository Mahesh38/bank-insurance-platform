package com.bank.insurance.onesb.adapter.persistence;

import com.bank.insurance.onesb.adapter.persistence.dto.PersistenceApiDtos.CreatePaymentSessionRequest;
import com.bank.insurance.onesb.adapter.persistence.dto.PersistenceApiDtos.PaymentSessionResponse;
import com.bank.insurance.onesb.domain.model.PaymentSession;
import com.bank.insurance.onesb.domain.port.outbound.PaymentSessionStorePort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * HTTP adapter persisting payment sessions to bank-persistence-service
 * {@code POST /internal/v1/payment-sessions}.
 */
@Component
public class HttpPaymentSessionStoreAdapter implements PaymentSessionStorePort {

    private final RestClient persistenceRestClient;

    @Autowired
    public HttpPaymentSessionStoreAdapter(
            @Qualifier("persistenceRestClient") RestClient persistenceRestClient) {
        this.persistenceRestClient = persistenceRestClient;
    }

    @Override
    public String save(PaymentSession session, String actorId) {
        PaymentSessionResponse response = persistenceRestClient.post()
                .uri("/internal/v1/payment-sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new CreatePaymentSessionRequest(
                        session.sessionId(),
                        session.jobId(),
                        session.applicationNumber(),
                        session.lob() != null ? session.lob().name() : "TERM",
                        session.paymentUrl(),
                        session.redirectUrl(),
                        session.status() != null ? session.status().name() : "AWAITING_PAYMENT",
                        session.externalTxnId(),
                        session.expiresAt(),
                        actorId != null && !actorId.isBlank() ? actorId : "system"
                ))
                .retrieve()
                .body(PaymentSessionResponse.class);
        if (response == null || response.sessionId() == null || response.sessionId().isBlank()) {
            throw new IllegalStateException("Persistence createPaymentSession returned empty response");
        }
        return response.sessionId();
    }
}
