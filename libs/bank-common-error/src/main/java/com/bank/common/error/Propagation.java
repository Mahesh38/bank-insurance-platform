package com.bank.common.error;

/**
 * How a calling service must re-present this error to <em>its</em> caller.
 *
 * <p>See {@code docs/journey-execution/07-PLATFORM-ERROR-CONTRACT.md §5}. This is the rule that
 * decides whether an orchestrator's compliance refusal reaches the RM as that refusal, or as an
 * unactionable 502.
 */
public enum Propagation {

    /**
     * Re-emit the same code to the caller. Use when the caller can act on it — a compliance gate,
     * a validation failure, an expiry the caller can resolve.
     */
    PROPAGATE,

    /**
     * Wrap as an {@code UPSTREAM_*} code. Use when the caller can do nothing about it — a
     * dependency being down, a malformed upstream response, an internal defect.
     */
    WRAP
}
