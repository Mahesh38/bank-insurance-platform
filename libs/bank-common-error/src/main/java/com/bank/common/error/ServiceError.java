package com.bank.common.error;

import java.util.Map;

/**
 * A single error within a {@link ServiceErrorResponse}.
 */
public record ServiceError(
        String code,
        String message,
        String field,
        Map<String, Object> extensions
) {
    public static ServiceError of(String code, String message) {
        return new ServiceError(code, message, null, null);
    }

    public static ServiceError ofField(String code, String message, String field) {
        return new ServiceError(code, message, field, null);
    }
}
