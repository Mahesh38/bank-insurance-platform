package com.bank.insurance.onesb.adapter.persistence;

import com.bank.insurance.onesb.adapter.persistence.dto.PersistenceApiDtos.CreatePaymentSessionRequest;
import com.bank.insurance.onesb.adapter.persistence.dto.PersistenceApiDtos.PaymentSessionResponse;
import com.bank.insurance.onesb.domain.model.Lob;
import com.bank.insurance.onesb.domain.model.PaymentSession;
import com.bank.insurance.onesb.domain.model.PaymentStatus;
import com.bank.insurance.onesb.domain.port.outbound.PaymentSessionStorePort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Instant;

/**
 * HTTP adapter implementing {@link PaymentSessionStorePort} against bank-persistence-service
 * (controller + entity already exist there — {@code PaymentSessionController}).
 */
@Component
public class HttpPaymentSessionStoreAdapter implements PaymentSessionStorePort {

    private final RestClient persistenceRestClient;

    @Autowired
    public HttpPaymentSessionStoreAdapter(@Qualifier("persistenceRestClient") RestClient persistenceRestClient) {
        this.persistenceRestClient = persistenceRestClient;
    }

    @Override
    public PaymentSession createSession(String jobId, String applicationNumber, Lob lob, String paymentUrl,
                                        String redirectUrl, PaymentStatus status, String externalTxnId,
                                        Instant expiresAt, String actorId) {
        PaymentSessionResponse response = persistenceRestClient.post()
                .uri("/internal/v1/payment-sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new CreatePaymentSessionRequest(
                        null,
                        jobId,
                        applicationNumber,
                        lob.name(),
                        paymentUrl,
                        redirectUrl,
                        status.name(),
                        externalTxnId,
                        expiresAt,
                        actorId
                ))
                .retrieve()
                .body(PaymentSessionResponse.class);
        if (response == null || response.sessionId() == null) {
            throw new IllegalStateException("Persistence createPaymentSession returned empty response");
        }
        return toPaymentSession(response);
    }

    private static PaymentSession toPaymentSession(PaymentSessionResponse r) {
        return new PaymentSession(
                r.sessionId(),
                r.jobId(),
                r.applicationNumber(),
                Lob.valueOf(r.lob()),
                r.paymentUrl(),
                r.redirectUrl(),
                PaymentStatus.valueOf(r.status()),
                r.externalTxnId(),
                r.createdAt(),
                r.expiresAt()
        );
    }
}
