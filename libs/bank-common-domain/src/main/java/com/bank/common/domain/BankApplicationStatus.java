package com.bank.common.domain;

/**
 * Bank-normalised application stage, mapped from 1SB {@code applicationStatus} values
 * per {@code field-guides/application-status.md}.
 */
public enum BankApplicationStatus {
    QUOTING,
    PROPOSAL,
    UW_REQUIREMENTS,
    UW_IN_PROGRESS,
    CUSTOMER_DECISION,
    PAYMENT,
    ISSUED,
    CLOSED,
    UNKNOWN
}
