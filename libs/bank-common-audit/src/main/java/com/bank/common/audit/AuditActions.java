package com.bank.common.audit;

/**
 * Canonical audit action names for the 1SB insurance integration service.
 *
 * <p>String constants are used (rather than an enum) so that adapter modules
 * can extend the vocabulary without requiring a lib change. All values in this
 * class are part of the shared contract — changing them is a breaking change.
 *
 * <p>New actions should follow the pattern {@code NOUN_PAST_TENSE_VERB}.
 */
public final class AuditActions {

    private AuditActions() {}

    // --- Quote lifecycle ---
    public static final String QUOTE_JOB_CREATED    = "QUOTE_JOB_CREATED";
    public static final String QUOTE_CREATED        = "QUOTE_CREATED";
    public static final String QUOTE_COMPLETED      = "QUOTE_COMPLETED";

    // --- Proposal lifecycle ---
    public static final String PROPOSAL_SUBMITTED        = "PROPOSAL_SUBMITTED";
    public static final String PROPOSAL_STATUS_UPDATED   = "PROPOSAL_STATUS_UPDATED";

    // --- Payment ---
    public static final String PAYMENT_URL_RETRIEVED     = "PAYMENT_URL_RETRIEVED";
    public static final String PAYMENT_SESSION_CREATED   = "PAYMENT_SESSION_CREATED";
    public static final String PAYMENT_INTIMATION_SENT   = "PAYMENT_INTIMATION_SENT";

    // --- Application status ---
    public static final String APPLICATION_STATUS_CHECKED = "APPLICATION_STATUS_CHECKED";

    // --- Document ---
    public static final String DOCUMENT_UPLOADED         = "DOCUMENT_UPLOADED";

    // --- Data retention ---
    public static final String PAYLOAD_RETENTION_DELETED = "PAYLOAD_RETENTION_DELETED";
}
