package com.bank.common.audit;

/**
 * Bank-governed audit action codes.
 * Centrally defined to ensure compliance tooling can parse events uniformly.
 */
public final class AuditAction {

    public static final String QUOTE_JOB_CREATED = "QUOTE_JOB_CREATED";
    public static final String QUOTE_COMPLETED = "QUOTE_COMPLETED";
    public static final String PROPOSAL_SUBMITTED = "PROPOSAL_SUBMITTED";
    public static final String PROPOSAL_STATUS_UPDATED = "PROPOSAL_STATUS_UPDATED";
    public static final String PAYMENT_SESSION_CREATED = "PAYMENT_SESSION_CREATED";
    public static final String PAYMENT_INTIMATION_SENT = "PAYMENT_INTIMATION_SENT";
    public static final String APPLICATION_STATUS_CHECKED = "APPLICATION_STATUS_CHECKED";
    public static final String DOCUMENT_UPLOADED = "DOCUMENT_UPLOADED";

    private AuditAction() {}
}
