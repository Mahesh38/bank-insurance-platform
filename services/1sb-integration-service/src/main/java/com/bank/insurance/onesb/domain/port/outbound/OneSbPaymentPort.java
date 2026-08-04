package com.bank.insurance.onesb.domain.port.outbound;

import com.bank.insurance.onesb.domain.command.CreatePaymentCommand;
import com.bank.insurance.onesb.domain.model.OneSbPaymentUrlResult;

/**
 * Outbound port for payment operations via 1SB.
 */
public interface OneSbPaymentPort {

    OneSbPaymentUrlResult createPaymentUrl(CreatePaymentCommand command);

    /**
     * FUNC-008 (P1, out of scope this phase) — deferred, see TD-022.
     *
     * @throws UnsupportedOperationException always, until FUNC-008 is implemented
     */
    void sendPaymentIntimation(String sessionId);
}
