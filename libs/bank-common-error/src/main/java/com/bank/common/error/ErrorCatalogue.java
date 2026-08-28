package com.bank.common.error;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * The single place where every error code's behaviour is declared — work item {@code ERR-005}.
 *
 * <p>Seeded from {@code docs/journey-execution/04-ERROR-AND-DEGRADED-STATE-CATALOGUE.md}. Look a
 * code up here instead of hand-writing a status, a title and a retry flag at a throw site:
 *
 * <pre>{@code
 * throw ServiceException.of(ErrorCodes.SUITABILITY_REQUIRED)
 *         .reason("assessment " + id + " expired at " + expiry)
 *         .build();
 * }</pre>
 *
 * <p>Two engineers throwing {@code QUOTE_EXPIRED} in two services cannot produce two different
 * responses, because neither of them writes the response.
 *
 * <p><strong>Public text is fixed.</strong> Every {@code publicTitle} and {@code publicDetail}
 * below is a constant with no interpolation, which is what makes the {@code S08-G7} PII assertion
 * a finite check over this class rather than an attempt to prove a negative over every log
 * statement in the codebase.
 */
public final class ErrorCatalogue {

    private ErrorCatalogue() {}

    private static final Map<String, ErrorDefinition> REGISTRY = buildRegistry();

    /**
     * The definition for {@code code}.
     *
     * @throws IllegalArgumentException if the code is not registered — a code with no entry is a
     *         defect, not a runtime condition to tolerate, because it would be emitted with no
     *         declared status, wording or retryability
     */
    public static ErrorDefinition require(String code) {
        ErrorDefinition definition = REGISTRY.get(code);
        if (definition == null) {
            throw new IllegalArgumentException(
                "Unregistered error code '" + code + "'. Add it to ErrorCatalogue and to "
                    + "docs/journey-execution/04-ERROR-AND-DEGRADED-STATE-CATALOGUE.md.");
        }
        return definition;
    }

    /** The definition for {@code code}, empty when unregistered. */
    public static Optional<ErrorDefinition> find(String code) {
        return Optional.ofNullable(REGISTRY.get(code));
    }

    /** True when {@code code} has a definition. */
    public static boolean isRegistered(String code) {
        return REGISTRY.containsKey(code);
    }

    /** Every registered code. Used by the CI check that diffs this registry against catalogue 04. */
    public static Set<String> codes() {
        return REGISTRY.keySet();
    }

    /** Every registered definition, in declaration order. */
    public static Map<String, ErrorDefinition> all() {
        return REGISTRY;
    }

    private static Map<String, ErrorDefinition> buildRegistry() {
        Map<String, ErrorDefinition> m = new LinkedHashMap<>();

        // --- Pre-catalogue codes (kept at their published values) --------------------
        put(m, ErrorCodes.VALIDATION_ERROR, ErrorCategory.VALIDATION, 422,
            "Validation failed", "Some of the details supplied are not valid.", "pre-catalogue");
        put(m, ErrorCodes.INVALID_REQUEST, ErrorCategory.VALIDATION, 422,
            "Invalid request", "The request could not be read.", "pre-catalogue");
        put(m, ErrorCodes.MISSING_REQUIRED_FIELD, ErrorCategory.VALIDATION, 422,
            "Missing required detail", "A required detail was not supplied.", "pre-catalogue");
        put(m, ErrorCodes.MISSING_IDEMPOTENCY_KEY, ErrorCategory.VALIDATION, 400,
            "Missing idempotency key", "This request must carry an idempotency key.", "04 section 2");
        put(m, ErrorCodes.UNSUPPORTED_LOB, ErrorCategory.VALIDATION, 422,
            "Product line not supported", "This line of business is not available.", "pre-catalogue");
        put(m, ErrorCodes.AGENT_ATTRIBUTION_MISSING, ErrorCategory.VALIDATION, 422,
            "Attribution missing", "The request is missing required attribution.", "pre-catalogue");

        put(m, ErrorCodes.UPSTREAM_BUSINESS_ERROR, ErrorCategory.UPSTREAM, 422, Retryability.NO,
            "Request declined", "The request could not be completed at this time.",
            AuditDisposition.NONE, Propagation.WRAP, "pre-catalogue");
        put(m, ErrorCodes.UPSTREAM_AUTH_FAILURE, ErrorCategory.UPSTREAM, 502, Retryability.NO,
            "Service temporarily unavailable", "Please try again shortly.",
            AuditDisposition.SECURITY_EVENT, Propagation.WRAP, "pre-catalogue");
        put(m, ErrorCodes.UPSTREAM_UNAVAILABLE, ErrorCategory.UPSTREAM, 503, Retryability.YES,
            "Service temporarily unavailable", "Please try again shortly.",
            AuditDisposition.NONE, Propagation.WRAP, "pre-catalogue");
        put(m, ErrorCodes.UPSTREAM_TIMEOUT, ErrorCategory.UPSTREAM, 504, Retryability.YES,
            "Service temporarily unavailable", "Please try again shortly.",
            AuditDisposition.NONE, Propagation.WRAP, "pre-catalogue");
        // Retryable by ratified AC, not by inference: FUNC-007-ASSIGNMENT.md line 28 specifies
        // "a non-HTTPS upstream URL is rejected as UPSTREAM_BAD_RESPONSE (502, retryable)", and
        // FUNC-007-REVIEW.md records it as passed. Two call sites disagreed on this flag before
        // the registry existed — which is defect D4 in its purest form — and the documented AC
        // is the one that wins.
        put(m, ErrorCodes.UPSTREAM_BAD_RESPONSE, ErrorCategory.UPSTREAM, 502, Retryability.YES,
            "Service temporarily unavailable", "Please try again shortly.",
            AuditDisposition.NONE, Propagation.WRAP, "pre-catalogue · FUNC-007 AC");

        // Catalogue 04 section 7 has no code for this: it lists TIMED_OUT, a *journey state*, not a
        // refusal. QUOTE_TIMEOUT is the wire code that reports it, so the reference is
        // "pre-catalogue" rather than a section that does not name it.
        put(m, ErrorCodes.QUOTE_TIMEOUT, ErrorCategory.UPSTREAM, 504, Retryability.YES,
            "Quote took too long", "No insurer answered in time. You can request a new quote.",
            AuditDisposition.NONE, Propagation.PROPAGATE, "pre-catalogue (reports 04 section 7 TIMED_OUT)");
        put(m, ErrorCodes.QUOTE_EXPIRED, ErrorCategory.CONFLICT, 409, Retryability.AFTER_REMEDIATION,
            "Quote expired", "This quote is no longer valid. Please request a new one.",
            AuditDisposition.NONE, Propagation.PROPAGATE, "04 section 6");
        put(m, ErrorCodes.PROPOSAL_REJECTED, ErrorCategory.UPSTREAM, 422, Retryability.NO,
            "Proposal declined", "The proposal was not accepted.",
            AuditDisposition.AUDIT, Propagation.PROPAGATE, "pre-catalogue");
        put(m, ErrorCodes.PROPOSAL_NOT_PAYABLE, ErrorCategory.CONFLICT, 409, Retryability.NO,
            "Payment not available yet", "This proposal cannot be paid for in its current state.",
            AuditDisposition.NONE, Propagation.PROPAGATE, "pre-catalogue");

        put(m, ErrorCodes.RESOURCE_NOT_FOUND, ErrorCategory.NOT_FOUND, 404,
            "Not found", "The requested item could not be found.", "pre-catalogue");
        put(m, ErrorCodes.CONFLICT, ErrorCategory.CONFLICT, 409,
            "Conflict", "This action conflicts with the current state.", "pre-catalogue");
        // Catalogue 04 section 2 names this IDEMPOTENCY_KEY_CONFLICT. The published constant is
        // IDEMPOTENCY_CONFLICT and wins, because ErrorCodes is partner-consumed (G9). Raised for
        // correction in the catalogue rather than resolved by minting a second code for one
        // condition — two codes for one condition is the defect this registry exists to remove.
        put(m, ErrorCodes.IDEMPOTENCY_CONFLICT, ErrorCategory.CONFLICT, 409, Retryability.NO,
            "Duplicate request", "This request was already submitted with different details.",
            AuditDisposition.AUDIT, Propagation.PROPAGATE,
            "pre-catalogue (04 section 2 names it IDEMPOTENCY_KEY_CONFLICT — section 13 row 1)");
        put(m, ErrorCodes.UNAUTHORIZED, ErrorCategory.AUTHENTICATION, 401,
            "Sign-in required", "Please sign in and try again.", "pre-catalogue");
        put(m, ErrorCodes.FORBIDDEN, ErrorCategory.AUTHORIZATION, 403,
            "Not permitted", "You do not have access to this action.", "pre-catalogue");
        // WRAP, deliberately: INTERNAL means *our* defect. Propagating another service's
        // INTERNAL_ERROR unchanged would claim their bug as ours and destroy the one signal that
        // says whose it is.
        put(m, ErrorCodes.INTERNAL_ERROR, ErrorCategory.INTERNAL, 500, Retryability.NO,
            "Something went wrong", "Please try again. Quote the incident id if it persists.",
            AuditDisposition.NONE, Propagation.WRAP, "pre-catalogue");

        // --- 04 section 2 — envelope and protocol -----------------------------------
        put(m, ErrorCodes.PAYLOAD_TOO_LARGE, ErrorCategory.VALIDATION, 413,
            "Request too large", "The request is larger than allowed.", "04 section 2");
        put(m, ErrorCodes.SCHEMA_INVALID, ErrorCategory.VALIDATION, 400,
            "Invalid request", "The request does not match the expected format.", "04 section 2");
        put(m, ErrorCodes.RATE_LIMITED, ErrorCategory.RATE_LIMIT, 429, Retryability.YES,
            "Too many requests", "Please wait a moment and try again.",
            AuditDisposition.NONE, Propagation.PROPAGATE, "04 section 2");
        put(m, ErrorCodes.REQUEST_IN_PROGRESS, ErrorCategory.CONFLICT, 409, Retryability.YES,
            "Already in progress", "This request is still being processed.",
            AuditDisposition.NONE, Propagation.PROPAGATE, "04 section 2");
        put(m, ErrorCodes.LOB_REQUIRED, ErrorCategory.VALIDATION, 422,
            "Product line required", "A line of business must be selected.", "04 section 2");

        // --- 04 section 3 — session and authentication -------------------------------
        // Every message here is deliberately identical in substance: distinguishing the causes in
        // a response is a user-enumeration oracle (04 section 3). The causes separate in the event
        // stream, never on the wire.
        put(m, ErrorCodes.SESSION_INVALID, ErrorCategory.AUTHENTICATION, 401, Retryability.AFTER_REAUTH,
            "Sign-in required", "Please sign in and try again.",
            AuditDisposition.AUDIT, Propagation.PROPAGATE, "04 section 3");
        put(m, ErrorCodes.SESSION_EXPIRED, ErrorCategory.AUTHENTICATION, 401, Retryability.AFTER_REAUTH,
            "Session expired", "Please sign in again to continue.",
            AuditDisposition.AUDIT, Propagation.PROPAGATE, "04 section 3");
        put(m, ErrorCodes.SESSION_REVOKED, ErrorCategory.AUTHENTICATION, 401, Retryability.NO,
            "Signed out", "Your session was ended. Please sign in again.",
            AuditDisposition.SECURITY_EVENT, Propagation.PROPAGATE, "04 section 3");
        put(m, ErrorCodes.AUTHENTICATION_FAILED, ErrorCategory.AUTHENTICATION, 401, Retryability.AFTER_REAUTH,
            "Sign-in unsuccessful", "The sign-in could not be completed.",
            AuditDisposition.AUDIT, Propagation.PROPAGATE, "04 section 3");
        put(m, ErrorCodes.STEP_UP_REQUIRED, ErrorCategory.AUTHENTICATION, 401, Retryability.AFTER_REMEDIATION,
            "Additional verification required", "Please complete verification to continue.",
            AuditDisposition.AUDIT, Propagation.PROPAGATE, "04 section 3");
        put(m, ErrorCodes.INVALID_STATE, ErrorCategory.AUTHENTICATION, 400, Retryability.NO,
            "Sign-in unsuccessful", "Please start the sign-in again.",
            AuditDisposition.SECURITY_EVENT, Propagation.PROPAGATE, "04 section 3");
        put(m, ErrorCodes.CODE_ALREADY_CONSUMED, ErrorCategory.AUTHENTICATION, 400, Retryability.NO,
            "Sign-in unsuccessful", "Please start the sign-in again.",
            AuditDisposition.SECURITY_EVENT, Propagation.PROPAGATE, "04 section 3");
        put(m, ErrorCodes.RETURN_LOCATION_NOT_ALLOWED, ErrorCategory.AUTHENTICATION, 400, Retryability.NO,
            "Sign-in unsuccessful", "Please start the sign-in again.",
            AuditDisposition.SECURITY_EVENT, Propagation.PROPAGATE, "04 section 3");
        put(m, ErrorCodes.CSRF_REJECTED, ErrorCategory.AUTHORIZATION, 403, Retryability.AFTER_FIX,
            "Request could not be verified", "Please refresh the page and try again.",
            AuditDisposition.SECURITY_EVENT, Propagation.PROPAGATE, "04 section 3");
        put(m, ErrorCodes.IDENTITY_PROVIDER_UNAVAILABLE, ErrorCategory.UPSTREAM, 503, Retryability.YES,
            "Sign-in temporarily unavailable", "Please try again shortly.",
            AuditDisposition.AUDIT, Propagation.WRAP, "04 section 3");

        // --- 04 section 4 — authorization --------------------------------------------
        // DEFAULT_DENY and EXPLICIT_DENY carry the same message by design: the distinction is for
        // audit, not for the RM (04 section 4).
        put(m, ErrorCodes.DEFAULT_DENY, ErrorCategory.AUTHORIZATION, 403, Retryability.NO,
            "Not permitted", "You do not have access to this action.",
            AuditDisposition.AUDIT, Propagation.PROPAGATE, "04 section 4");
        put(m, ErrorCodes.EXPLICIT_DENY, ErrorCategory.AUTHORIZATION, 403, Retryability.NO,
            "Not permitted", "You do not have access to this action.",
            AuditDisposition.AUDIT, Propagation.PROPAGATE, "04 section 4");
        put(m, ErrorCodes.ACCOUNT_SUSPENDED, ErrorCategory.AUTHORIZATION, 403, Retryability.NO,
            "Account suspended", "Please contact your supervisor.",
            AuditDisposition.AUDIT, Propagation.PROPAGATE, "04 section 4");
        put(m, ErrorCodes.ORIGINATION_RM_ONLY, ErrorCategory.AUTHORIZATION, 403, Retryability.NO,
            "Not permitted", "Only a relationship manager can start this.",
            AuditDisposition.COMPLIANCE_EVENT, Propagation.PROPAGATE, "04 section 4");
        put(m, ErrorCodes.ASSIST_ONLY_ACTOR, ErrorCategory.AUTHORIZATION, 403, Retryability.NO,
            "Not permitted", "Your role can assist with this but not perform it.",
            AuditDisposition.COMPLIANCE_EVENT, Propagation.PROPAGATE, "04 section 4");
        put(m, ErrorCodes.CROSS_INSURER_DENIED, ErrorCategory.AUTHORIZATION, 403, Retryability.NO,
            "Not permitted", "You do not have access to this action.",
            AuditDisposition.COMPLIANCE_EVENT, Propagation.PROPAGATE, "04 section 4");
        put(m, ErrorCodes.OUT_OF_BRANCH_SCOPE, ErrorCategory.AUTHORIZATION, 403, Retryability.NO,
            "Outside your branch scope", "This record belongs to another branch.",
            AuditDisposition.AUDIT, Propagation.PROPAGATE, "04 section 4");
        put(m, ErrorCodes.SP_CERTIFICATION_REQUIRED, ErrorCategory.COMPLIANCE_GATE, 403, Retryability.AFTER_REMEDIATION,
            "Certification required", "A current certification is needed to sell this product.",
            AuditDisposition.COMPLIANCE_EVENT, Propagation.PROPAGATE, "04 section 4");
        put(m, ErrorCodes.BREAK_GLASS_INVALID, ErrorCategory.AUTHORIZATION, 403, Retryability.NO,
            "Not permitted", "You do not have access to this action.",
            AuditDisposition.SECURITY_EVENT, Propagation.PROPAGATE, "04 section 4");
        put(m, ErrorCodes.AUTHORIZATION_UNAVAILABLE, ErrorCategory.AUTHORIZATION, 403, Retryability.YES,
            "Temporarily unavailable", "Please try again shortly.",
            AuditDisposition.AUDIT, Propagation.WRAP, "04 section 4");
        put(m, ErrorCodes.SERVICE_IDENTITY_REJECTED, ErrorCategory.AUTHORIZATION, 403, Retryability.NO,
            "Not permitted", "You do not have access to this action.",
            AuditDisposition.SECURITY_EVENT, Propagation.WRAP, "04 section 4");

        // --- 04 section 5 — compliance hard gates ------------------------------------
        put(m, ErrorCodes.SUITABILITY_REQUIRED, ErrorCategory.COMPLIANCE_GATE, 403, Retryability.AFTER_REMEDIATION,
            "Suitability assessment required",
            "A completed suitability assessment is needed before this quote can be produced.",
            AuditDisposition.COMPLIANCE_EVENT, Propagation.PROPAGATE, "04 section 5");
        put(m, ErrorCodes.CONSENT_REQUIRED, ErrorCategory.COMPLIANCE_GATE, 403, Retryability.AFTER_REMEDIATION,
            "Customer consent required",
            "The customer must give consent on their own device before this can continue.",
            AuditDisposition.COMPLIANCE_EVENT, Propagation.PROPAGATE, "04 section 5");
        put(m, ErrorCodes.ATTRIBUTION_NOT_CALLER_SUPPLIED, ErrorCategory.COMPLIANCE_GATE, 422, Retryability.AFTER_FIX,
            "Request could not be accepted", "This request could not be accepted.",
            AuditDisposition.SECURITY_EVENT, Propagation.PROPAGATE, "04 section 5");
        put(m, ErrorCodes.PAYMENT_DEVICE_ISOLATION, ErrorCategory.COMPLIANCE_GATE, 403, Retryability.NO,
            "Payment happens on the customer's device",
            "The payment link goes to the customer. There is no payment path here, by design.",
            AuditDisposition.COMPLIANCE_EVENT, Propagation.PROPAGATE, "04 section 5");
        put(m, ErrorCodes.PAYMENT_NOT_RECONCILED, ErrorCategory.COMPLIANCE_GATE, 409, Retryability.AFTER_REMEDIATION,
            "Payment not yet confirmed", "The policy can be issued once the payment is confirmed.",
            AuditDisposition.COMPLIANCE_EVENT, Propagation.PROPAGATE, "04 section 5");
        put(m, ErrorCodes.CONFIG_VERSION_REQUIRED, ErrorCategory.CONFIG, 422, Retryability.NO,
            "Something went wrong", "Please try again. Quote the incident id if it persists.",
            AuditDisposition.NONE, Propagation.WRAP, "04 section 5");

        // --- 04 section 6 — domain state ---------------------------------------------
        put(m, ErrorCodes.ILLEGAL_TRANSITION, ErrorCategory.CONFLICT, 409, Retryability.AFTER_FIX,
            "Out of date", "This view is out of date. Please refresh and try again.",
            AuditDisposition.AUDIT, Propagation.PROPAGATE, "04 section 6");
        put(m, ErrorCodes.ASSESSMENT_IMMUTABLE, ErrorCategory.CONFLICT, 409, Retryability.AFTER_REMEDIATION,
            "Assessment cannot be changed", "Create a new assessment instead.",
            AuditDisposition.AUDIT, Propagation.PROPAGATE, "04 section 6");
        put(m, ErrorCodes.WITHDRAWAL_NOT_PERMITTED, ErrorCategory.CONFLICT, 409, Retryability.NO,
            "Cannot be withdrawn", "This cannot be withdrawn while payment is in progress.",
            AuditDisposition.AUDIT, Propagation.PROPAGATE, "04 section 6");
        put(m, ErrorCodes.PAYMENT_ALREADY_IN_PROGRESS, ErrorCategory.CONFLICT, 409, Retryability.NO,
            "Payment already in progress", "A payment attempt is already under way.",
            AuditDisposition.AUDIT, Propagation.PROPAGATE, "04 section 6");
        put(m, ErrorCodes.PAYMENT_STATE_UNCERTAIN, ErrorCategory.CONFLICT, 409, Retryability.NO,
            "Payment being confirmed", "Please wait while the payment is confirmed. Do not retry.",
            AuditDisposition.COMPLIANCE_EVENT, Propagation.PROPAGATE, "04 section 6");
        put(m, ErrorCodes.CUSTOMER_NOT_IN_BOOK, ErrorCategory.AUTHORIZATION, 422, Retryability.NO,
            "Customer not available", "This customer is not in your book.",
            AuditDisposition.AUDIT, Propagation.PROPAGATE, "04 section 6");
        put(m, ErrorCodes.RM_NOT_CERTIFIED, ErrorCategory.COMPLIANCE_GATE, 422, Retryability.AFTER_REMEDIATION,
            "Certification required", "This must be assigned to a certified relationship manager.",
            AuditDisposition.COMPLIANCE_EVENT, Propagation.PROPAGATE, "04 section 6");
        put(m, ErrorCodes.OPPORTUNITY_REQUIRED, ErrorCategory.VALIDATION, 422,
            "Lead required", "This action needs an existing lead.", "04 section 6");
        put(m, ErrorCodes.INVALID_OFFER_REFERENCE, ErrorCategory.VALIDATION, 422,
            "Offer no longer valid", "Please select again from a current quote.", "04 section 6");
        put(m, ErrorCodes.PREMIUM_MISMATCH, ErrorCategory.CONFLICT, 422, Retryability.NO,
            "Premium could not be confirmed", "Please start again from a fresh quote. Do not retry.",
            AuditDisposition.COMPLIANCE_EVENT, Propagation.PROPAGATE, "04 section 6");

        // --- 04 section 8 — configuration ---------------------------------------------
        put(m, ErrorCodes.CONFIGURATION_UNRESOLVABLE, ErrorCategory.CONFIG, 422, Retryability.NO,
            "Temporarily unavailable", "Please try again shortly.",
            AuditDisposition.NONE, Propagation.WRAP, "04 section 8");

        return Collections.unmodifiableMap(m);
    }

    private static void put(Map<String, ErrorDefinition> m, String code, ErrorCategory category,
                            int status, String title, String detail, String ref) {
        put(m, code, category, status, null, title, detail, null, null, ref);
    }

    private static void put(Map<String, ErrorDefinition> m, String code, ErrorCategory category,
                            int status, Retryability retryability, String title, String detail,
                            AuditDisposition audit, Propagation propagation, String ref) {
        ErrorDefinition previous = m.put(code, new ErrorDefinition(
            code, category, status, retryability, title, detail, audit, propagation, null, ref));
        if (previous != null) {
            throw new IllegalStateException("Duplicate catalogue entry for code " + code);
        }
    }
}
