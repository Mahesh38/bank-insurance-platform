package com.bank.common.error;

/**
 * Whether — and under what condition — a caller may repeat a failed request.
 *
 * <p>This replaces the boolean {@code retryable} as the source of truth. A boolean cannot
 * distinguish "retry now with backoff" from "retry once the RM has done something", and the two
 * demand opposite client behaviour. {@link #toBoolean()} preserves the existing wire field.
 *
 * <p>Column source: {@code docs/journey-execution/04-ERROR-AND-DEGRADED-STATE-CATALOGUE.md}
 * "Retryable".
 */
public enum Retryability {

    /** The same request will always fail. Do not repeat it. */
    NO(false),

    /** Repeating works only after the caller corrects the request. */
    AFTER_FIX(false),

    /** Repeating works only after a human completes a missing step (consent, suitability, MFA). */
    AFTER_REMEDIATION(false),

    /** Repeating works only after the caller re-authenticates. */
    AFTER_REAUTH(false),

    /** Transient. Repeating the identical request may succeed. Honour {@code Retry-After} if present. */
    YES(true);

    private final boolean automaticallyRetryable;

    Retryability(boolean automaticallyRetryable) {
        this.automaticallyRetryable = automaticallyRetryable;
    }

    /**
     * The value carried in the {@code retryable} wire field.
     *
     * <p>True only for {@link #YES}: it means "a machine may repeat this unchanged". Every other
     * value needs someone to change something first, and a client that auto-retries them creates
     * duplicate submissions — the failure mode {@code 04 §7 SUBMISSION_FAILED} exists to prevent.
     */
    public boolean toBoolean() {
        return automaticallyRetryable;
    }
}
