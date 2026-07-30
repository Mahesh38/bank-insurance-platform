package com.bank.common.audit;

/**
 * Canonical outcome values for {@link AuditEvent#getOutcome()}.
 */
public final class AuditOutcomes {

    private AuditOutcomes() {}

    /** Operation completed successfully. */
    public static final String SUCCESS = "SUCCESS";

    /** Operation failed with a known, handled error. */
    public static final String FAILURE = "FAILURE";

    /** Operation was rejected by an upstream system. */
    public static final String REJECTED = "REJECTED";

    /** Operation timed out before completion. */
    public static final String TIMEOUT = "TIMEOUT";

    /** Result is pending — async operation has not completed yet. */
    public static final String PENDING = "PENDING";
}
