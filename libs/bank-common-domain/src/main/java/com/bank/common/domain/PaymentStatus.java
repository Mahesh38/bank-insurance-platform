package com.bank.common.domain;

/**
 * Payment session lifecycle states.
 */
public enum PaymentStatus {
    AWAITING_PAYMENT,
    INTIMATION_SENT,
    PAYMENT_SUCCESS,
    PAYMENT_FAILURE
}
