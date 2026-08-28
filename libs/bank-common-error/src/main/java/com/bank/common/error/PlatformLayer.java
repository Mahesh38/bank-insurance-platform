package com.bank.common.error;

/**
 * The rung of the request ladder at which a refusal was produced.
 *
 * <p>Definitions are in
 * {@code docs/journey-execution/01-REQUEST-LIFECYCLE-STANDARD.md §1}. Recording the layer is what
 * makes "a rule enforced only at L4 is not enforced" checkable after the fact: a gate that only
 * ever reports {@link #L4} is a gate every service-to-service path bypasses.
 */
public enum PlatformLayer {

    /** CloudFront + WAF. Refusals here never reach the application and have no platform record. */
    L1,

    /** API Gateway — request size, route schema, throttle. */
    L2,

    /** Internal ALB — routing only; never terminates a business decision. */
    L3,

    /** BFF — session authentication, CSRF, PEP call, aggregation. The redaction boundary. */
    L4,

    /** Domain service — service identity, PDP re-check, cross-aggregate validation, orchestration. */
    L5,

    /** Aggregate — domain invariants in one transaction. Most gates live here. */
    L6,

    /** Store — constraints, insert-only roles, visibility predicates, object lock. */
    L7
}
