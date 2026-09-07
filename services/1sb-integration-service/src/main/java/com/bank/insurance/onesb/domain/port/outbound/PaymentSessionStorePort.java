package com.bank.insurance.onesb.domain.port.outbound;

import com.bank.common.domain.Lob;
import com.bank.common.domain.PaymentSession;
import com.bank.common.domain.PaymentStatus;

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
