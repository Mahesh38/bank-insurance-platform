package com.bank.common.error;

/**
 * Canonical error codes used across all bank platform services.
 * Keep values stable — they appear in API responses consumed by partners.
 */
public final class ErrorCodes {

    private ErrorCodes() {}

    // --- Validation ---
    public static final String VALIDATION_ERROR       = "VALIDATION_ERROR";
    public static final String INVALID_REQUEST        = "INVALID_REQUEST";
    public static final String MISSING_REQUIRED_FIELD = "MISSING_REQUIRED_FIELD";
    public static final String MISSING_IDEMPOTENCY_KEY = "MISSING_IDEMPOTENCY_KEY";
    public static final String UNSUPPORTED_LOB        = "UNSUPPORTED_LOB";

    // --- Upstream / downstream partner errors ---
    public static final String UPSTREAM_BUSINESS_ERROR  = "UPSTREAM_BUSINESS_ERROR";
    public static final String UPSTREAM_AUTH_FAILURE    = "UPSTREAM_AUTH_FAILURE";
    public static final String UPSTREAM_UNAVAILABLE     = "UPSTREAM_UNAVAILABLE";
    public static final String UPSTREAM_TIMEOUT         = "UPSTREAM_TIMEOUT";
    public static final String UPSTREAM_BAD_RESPONSE    = "UPSTREAM_BAD_RESPONSE";

    // --- Quote / job lifecycle ---
    public static final String QUOTE_TIMEOUT         = "QUOTE_TIMEOUT";
    public static final String QUOTE_EXPIRED         = "QUOTE_EXPIRED";

    // --- Proposal / attribution ---
    public static final String AGENT_ATTRIBUTION_MISSING = "AGENT_ATTRIBUTION_MISSING";
    public static final String PROPOSAL_REJECTED         = "PROPOSAL_REJECTED";

    // --- Payment ---
    public static final String PROPOSAL_NOT_PAYABLE      = "PROPOSAL_NOT_PAYABLE";

    // --- Resource / entity ---
    public static final String RESOURCE_NOT_FOUND    = "RESOURCE_NOT_FOUND";
    public static final String CONFLICT              = "CONFLICT";
    public static final String IDEMPOTENCY_CONFLICT  = "IDEMPOTENCY_CONFLICT";

    // --- Auth ---
    public static final String UNAUTHORIZED          = "UNAUTHORIZED";
    public static final String FORBIDDEN             = "FORBIDDEN";

    // --- Internal ---
    public static final String INTERNAL_ERROR        = "INTERNAL_ERROR";

    // ------------------------------------------------------------------------
    // Codes below are seeded from docs/journey-execution/04-ERROR-AND-DEGRADED-
    // STATE-CATALOGUE.md (work item ERR-005). Everything above this line predates
    // the catalogue and keeps its published value unchanged — ErrorCodes is
    // partner-consumed, so this file is additive only (review trigger G9).
    // ------------------------------------------------------------------------

    // --- Envelope and protocol (04 section 2) ---
    public static final String PAYLOAD_TOO_LARGE  = "PAYLOAD_TOO_LARGE";
    public static final String SCHEMA_INVALID     = "SCHEMA_INVALID";
    public static final String RATE_LIMITED       = "RATE_LIMITED";
    public static final String REQUEST_IN_PROGRESS = "REQUEST_IN_PROGRESS";
    public static final String LOB_REQUIRED       = "LOB_REQUIRED";

    // --- Session and authentication (04 section 3) ---
    public static final String SESSION_INVALID    = "SESSION_INVALID";
    public static final String SESSION_EXPIRED    = "SESSION_EXPIRED";
    public static final String SESSION_REVOKED    = "SESSION_REVOKED";
    public static final String AUTHENTICATION_FAILED = "AUTHENTICATION_FAILED";
    public static final String STEP_UP_REQUIRED   = "STEP_UP_REQUIRED";
    public static final String INVALID_STATE      = "INVALID_STATE";
    public static final String CODE_ALREADY_CONSUMED = "CODE_ALREADY_CONSUMED";
    public static final String RETURN_LOCATION_NOT_ALLOWED = "RETURN_LOCATION_NOT_ALLOWED";
    public static final String CSRF_REJECTED      = "CSRF_REJECTED";
    public static final String IDENTITY_PROVIDER_UNAVAILABLE = "IDENTITY_PROVIDER_UNAVAILABLE";

    // --- Authorization (04 section 4) ---
    public static final String DEFAULT_DENY       = "DEFAULT_DENY";
    public static final String EXPLICIT_DENY      = "EXPLICIT_DENY";
    public static final String ACCOUNT_SUSPENDED  = "ACCOUNT_SUSPENDED";
    public static final String ORIGINATION_RM_ONLY = "ORIGINATION_RM_ONLY";
    public static final String ASSIST_ONLY_ACTOR  = "ASSIST_ONLY_ACTOR";
    public static final String CROSS_INSURER_DENIED = "CROSS_INSURER_DENIED";
    public static final String OUT_OF_BRANCH_SCOPE = "OUT_OF_BRANCH_SCOPE";
    public static final String SP_CERTIFICATION_REQUIRED = "SP_CERTIFICATION_REQUIRED";
    public static final String BREAK_GLASS_INVALID = "BREAK_GLASS_INVALID";
    public static final String AUTHORIZATION_UNAVAILABLE = "AUTHORIZATION_UNAVAILABLE";
    public static final String SERVICE_IDENTITY_REJECTED = "SERVICE_IDENTITY_REJECTED";

    // --- Compliance hard gates (04 section 5) ---
    public static final String SUITABILITY_REQUIRED = "SUITABILITY_REQUIRED";
    public static final String CONSENT_REQUIRED   = "CONSENT_REQUIRED";
    public static final String ATTRIBUTION_NOT_CALLER_SUPPLIED = "ATTRIBUTION_NOT_CALLER_SUPPLIED";
    public static final String PAYMENT_DEVICE_ISOLATION = "PAYMENT_DEVICE_ISOLATION";
    public static final String PAYMENT_NOT_RECONCILED = "PAYMENT_NOT_RECONCILED";
    public static final String CONFIG_VERSION_REQUIRED = "CONFIG_VERSION_REQUIRED";

    // --- Domain state (04 section 6) ---
    public static final String ILLEGAL_TRANSITION = "ILLEGAL_TRANSITION";
    public static final String ASSESSMENT_IMMUTABLE = "ASSESSMENT_IMMUTABLE";
    public static final String WITHDRAWAL_NOT_PERMITTED = "WITHDRAWAL_NOT_PERMITTED";
    public static final String PAYMENT_ALREADY_IN_PROGRESS = "PAYMENT_ALREADY_IN_PROGRESS";
    public static final String PAYMENT_STATE_UNCERTAIN = "PAYMENT_STATE_UNCERTAIN";
    public static final String CUSTOMER_NOT_IN_BOOK = "CUSTOMER_NOT_IN_BOOK";
    public static final String RM_NOT_CERTIFIED   = "RM_NOT_CERTIFIED";
    public static final String OPPORTUNITY_REQUIRED = "OPPORTUNITY_REQUIRED";
    public static final String INVALID_OFFER_REFERENCE = "INVALID_OFFER_REFERENCE";
    public static final String PREMIUM_MISMATCH   = "PREMIUM_MISMATCH";

    // --- Configuration (04 section 8) ---
    public static final String CONFIGURATION_UNRESOLVABLE = "CONFIGURATION_UNRESOLVABLE";
}
