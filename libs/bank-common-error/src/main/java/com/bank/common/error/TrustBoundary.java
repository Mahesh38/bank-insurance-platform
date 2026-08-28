package com.bank.common.error;

/**
 * Where a service sits relative to the trust boundary, and therefore whether it redacts.
 *
 * <p>See {@code docs/journey-execution/07-PLATFORM-ERROR-CONTRACT.md §4.4}: L4 is the last hop that
 * may hold a diagnostic and the first that must never emit one.
 */
public enum TrustBoundary {

    /** Faces a client. Diagnostics are stripped and wording comes from the catalogue. */
    PUBLIC,

    /** Cluster-private. Diagnostics travel to the calling service, which redacts in its turn. */
    INTERNAL
}
