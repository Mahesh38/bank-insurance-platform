package com.bank.insurance.onesb.domain.port.outbound;

import com.bank.insurance.onesb.domain.model.Lob;
import com.bank.insurance.onesb.domain.model.PaymentSession;
import com.bank.insurance.onesb.domain.model.PaymentStatus;

import java.time.Instant;

/**
 * Port for persisting payment sessions. Implemented by
 * {@code adapter.persistence.HttpPaymentSessionStoreAdapter} (HTTP → bank-persistence-service).
 */
public interface PaymentSessionStorePort {

    PaymentSession createSession(String jobId, String applicationNumber, Lob lob, String paymentUrl,
                                 String redirectUrl, PaymentStatus status, String externalTxnId,
                                 Instant expiresAt, String actorId);
}
