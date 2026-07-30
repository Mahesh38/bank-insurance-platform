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

    // --- Upstream / downstream partner errors ---
    public static final String UPSTREAM_BUSINESS_ERROR  = "UPSTREAM_BUSINESS_ERROR";
    public static final String UPSTREAM_AUTH_FAILURE    = "UPSTREAM_AUTH_FAILURE";
    public static final String UPSTREAM_UNAVAILABLE     = "UPSTREAM_UNAVAILABLE";
    public static final String UPSTREAM_TIMEOUT         = "UPSTREAM_TIMEOUT";
    public static final String UPSTREAM_BAD_RESPONSE    = "UPSTREAM_BAD_RESPONSE";

    // --- Resource / entity ---
    public static final String RESOURCE_NOT_FOUND    = "RESOURCE_NOT_FOUND";
    public static final String CONFLICT              = "CONFLICT";
    public static final String IDEMPOTENCY_CONFLICT  = "IDEMPOTENCY_CONFLICT";

    // --- Auth ---
    public static final String UNAUTHORIZED          = "UNAUTHORIZED";
    public static final String FORBIDDEN             = "FORBIDDEN";

    // --- Internal ---
    public static final String INTERNAL_ERROR        = "INTERNAL_ERROR";
}
