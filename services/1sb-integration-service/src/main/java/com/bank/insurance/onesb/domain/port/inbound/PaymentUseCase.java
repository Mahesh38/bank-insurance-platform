package com.bank.insurance.onesb.domain.port.inbound;

import com.bank.insurance.onesb.domain.command.CreatePaymentCommand;
import com.bank.insurance.onesb.domain.model.PaymentSession;

/**
 * Inbound use-case for payment URL creation (FUNC-007).
 */
public interface PaymentUseCase {

    /**
     * Creates a payment session and returns the HTTPS payment URL.
     * Never logs the full {@code paymentUrl} — only {@code paymentRef} (session id).
     */
    PaymentSession createPayment(CreatePaymentCommand command);
}
