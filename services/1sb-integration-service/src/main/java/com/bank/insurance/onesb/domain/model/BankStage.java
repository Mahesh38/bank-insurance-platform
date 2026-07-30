package com.bank.insurance.onesb.domain.model;

/**
 * Bank-normalised application journey stage (FUNC-009).
 * Mapped from 1SB {@code applicationStatus} via {@code StatusNormaliser}.
 */
public enum BankStage {
    QUOTING,
    QUOTE_SELECTED,
    PROPOSAL,
    UW_REQUIREMENTS,
    UW_IN_PROGRESS,
    CUSTOMER_DECISION,
    PAYMENT,
    ISSUED,
    CLOSED,
    UNKNOWN
}
