package com.bank.insurance.onesb.domain.port.outbound;

import com.bank.insurance.onesb.domain.model.PaymentSession;

/**
 * Port for persisting payment sessions via bank-persistence-service.
 */
public interface PaymentSessionStorePort {

    /**
     * Persists a payment session. Returns the stored session id (may equal {@code session.sessionId()}).
     */
    String save(PaymentSession session, String actorId);
}
