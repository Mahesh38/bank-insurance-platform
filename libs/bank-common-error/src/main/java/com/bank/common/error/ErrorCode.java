package com.bank.common.error;

/**
 * Common error codes shared across bank services.
 * Service-specific codes are defined in each service's own enum/constants.
 */
public final class ErrorCode {

    public static final String VALIDATION_FAILED = "VALIDATION_FAILED";
    public static final String REQUIRED_FIELD = "REQUIRED_FIELD";
    public static final String INVALID_ENUM = "INVALID_ENUM";
    public static final String NOT_FOUND = "NOT_FOUND";
    public static final String DUPLICATE_IDEMPOTENCY_KEY = "DUPLICATE_IDEMPOTENCY_KEY";
    public static final String MISSING_IDEMPOTENCY_KEY = "MISSING_IDEMPOTENCY_KEY";
    public static final String DOWNSTREAM_UNAVAILABLE = "DOWNSTREAM_UNAVAILABLE";
    public static final String INTERNAL_ERROR = "INTERNAL_ERROR";
    public static final String UNAUTHORIZED = "UNAUTHORIZED";
    public static final String FORBIDDEN = "FORBIDDEN";

    private ErrorCode() {}
}
