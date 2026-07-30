package com.bank.insurance.onesb.domain.port.outbound;

import com.bank.insurance.onesb.domain.model.PaymentSession;

/**
 * Outbound port for payment session operations via 1SB.
 */
public interface OneSbPaymentPort {

    PaymentSession createPaymentSession(String jobId, String applicationNumber, String lob,
                                        String redirectUrl, String actorId);

    void sendPaymentIntimation(String sessionId);
}
