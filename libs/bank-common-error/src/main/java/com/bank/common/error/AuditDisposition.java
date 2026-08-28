package com.bank.common.error;

/**
 * What evidence a refusal must leave behind.
 *
 * <p>Derived from the "Audit" column of
 * {@code docs/journey-execution/04-ERROR-AND-DEGRADED-STATE-CATALOGUE.md}. Declaring this on the
 * catalogue entry rather than at the throw site is what stops a compliance-bearing refusal from
 * being emitted as a silent 403: for {@link #COMPLIANCE_EVENT} the refusal is itself the evidence
 * a regulator asks for.
 */
public enum AuditDisposition {

    /** No audit record. Ordinary client-caused refusals. */
    NONE,

    /** A platform audit event is written. */
    AUDIT,

    /** A security event is written — the occurrence may indicate a client defect or an intrusion. */
    SECURITY_EVENT,

    /** A compliance event is written. The refusal is regulatory evidence and is never silent. */
    COMPLIANCE_EVENT
}
