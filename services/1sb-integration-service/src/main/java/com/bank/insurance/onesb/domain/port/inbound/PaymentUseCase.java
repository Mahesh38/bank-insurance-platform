package com.bank.insurance.onesb.domain.port.inbound;

import com.bank.insurance.onesb.domain.command.CreatePaymentCommand;
import com.bank.insurance.onesb.domain.model.PaymentSession;

/**
 * Inbound use-case for payment session creation (FUNC-007).
 */
public interface PaymentUseCase {

    /**
     * Creates a 1SB payment session for a payable application.
     *
     * @throws com.bank.common.error.ServiceException 409 {@code PROPOSAL_NOT_PAYABLE} when the
     *         originating proposal job is not in a payable state
     */
    PaymentSession createPaymentSession(CreatePaymentCommand command);
}
