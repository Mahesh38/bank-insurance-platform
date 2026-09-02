package com.bank.common.error;

/**
 * The class of a failure — how a caller must treat it, and how the platform must log and count it.
 *
 * <p>A category, not an HTTP status, decides behaviour. Two codes that both return 403
 * ({@code DEFAULT_DENY} and {@code SUITABILITY_REQUIRED}) demand entirely different handling: one
 * is a permission answer, the other is a compliance gate the RM can clear.
 *
 * <p>{@link #clientCaused()} drives log level. A validation refusal is a normal outcome of a
 * public API; logging it at {@code ERROR} with a stack trace is what makes an error dashboard
 * unreadable within a week.
 *
 * <p>Specified in {@code docs/journey-execution/07-PLATFORM-ERROR-CONTRACT.md §2.1}.
 */
public enum ErrorCategory {

    /** The request is wrong and the caller can fix it. */
    VALIDATION(400, Retryability.AFTER_FIX, true, AuditDisposition.NONE),

    /** Who the caller is has not been established. */
    AUTHENTICATION(401, Retryability.AFTER_REAUTH, true, AuditDisposition.AUDIT),

    /** The caller is known and not permitted. */
    AUTHORIZATION(403, Retryability.NO, true, AuditDisposition.AUDIT),

    /** The referenced resource does not exist for this caller. */
    NOT_FOUND(404, Retryability.NO, true, AuditDisposition.NONE),

    /** State disagrees — idempotency, an illegal transition, an expiry. */
    CONFLICT(409, Retryability.NO, true, AuditDisposition.NONE),

    /** A regulator-mandated refusal. The refusal is itself evidence and is never silent. */
    COMPLIANCE_GATE(403, Retryability.AFTER_REMEDIATION, true, AuditDisposition.COMPLIANCE_EVENT),

    /** A dependency failed. Not our defect, still our incident. */
    UPSTREAM(502, Retryability.YES, false, AuditDisposition.NONE),

    /** The platform cannot resolve its own configuration. Fail closed. */
    CONFIG(503, Retryability.NO, false, AuditDisposition.NONE),

    /** Throttled. */
    RATE_LIMIT(429, Retryability.YES, true, AuditDisposition.NONE),

    /** Our defect. Always a bug, always alertable. */
    INTERNAL(500, Retryability.NO, false, AuditDisposition.NONE);

    private final int defaultHttpStatus;
    private final Retryability defaultRetryability;
    private final boolean clientCaused;
    private final AuditDisposition defaultAudit;

    ErrorCategory(int defaultHttpStatus,
                  Retryability defaultRetryability,
                  boolean clientCaused,
                  AuditDisposition defaultAudit) {
        this.defaultHttpStatus = defaultHttpStatus;
        this.defaultRetryability = defaultRetryability;
        this.clientCaused = clientCaused;
        this.defaultAudit = defaultAudit;
    }

    /** The status used when a catalogue entry does not state one. */
    public int defaultHttpStatus() {
        return defaultHttpStatus;
    }

    /** The retryability used when a catalogue entry does not state one. */
    public Retryability defaultRetryability() {
        return defaultRetryability;
    }

    /** The audit disposition used when a catalogue entry does not state one. */
    public AuditDisposition defaultAudit() {
        return defaultAudit;
    }

    /**
     * True when the caller caused this, false when the platform or a dependency did.
     *
     * <p>Client-caused failures log at {@code WARN} without a stack trace; the rest log at
     * {@code ERROR} with one.
     */
    public boolean clientCaused() {
        return clientCaused;
    }

    /** True when a failure in this category should be treated as an operational signal. */
    public boolean alertable() {
        return !clientCaused || this == COMPLIANCE_GATE;
    }
}
