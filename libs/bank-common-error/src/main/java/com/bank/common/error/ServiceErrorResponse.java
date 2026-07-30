package com.bank.common.error;

import lombok.Builder;
import lombok.NonNull;
import lombok.ToString;
import lombok.Value;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * RFC 7807 "Problem Details for HTTP APIs" response envelope, extended with
 * bank-platform fields ({@code code}, {@code retryable}, {@code upstreamCode}).
 *
 * <p>Intended to be serialised as the HTTP response body for all error responses.
 * Controllers must not return raw upstream error JSON.
 */
@Value
@Builder
@ToString(of = {"status", "code", "detail"})
public class ServiceErrorResponse {

    String    type;
    @NonNull String title;
    int       status;
    String    detail;
    @NonNull String code;
    boolean   retryable;
    String    upstreamCode;
    Instant   timestamp;
    List<ServiceError> errors;

    // --- Factory shortcuts ---

    public static ServiceErrorResponse validation(String detail, List<ServiceError> fieldErrors) {
        return builder()
            .type("about:blank")
            .title("Validation Failed")
            .status(400)
            .detail(detail)
            .code(ErrorCodes.VALIDATION_ERROR)
            .retryable(false)
            .errors(fieldErrors)
            .build();
    }

    public static ServiceErrorResponse upstreamBusiness(String detail, String upstreamCode) {
        return builder()
            .type("about:blank")
            .title("Upstream Business Error")
            .status(422)
            .detail(detail)
            .code(ErrorCodes.UPSTREAM_BUSINESS_ERROR)
            .retryable(false)
            .upstreamCode(upstreamCode)
            .build();
    }

    public static ServiceErrorResponse upstreamUnavailable(String detail) {
        return builder()
            .type("about:blank")
            .title("Upstream Unavailable")
            .status(503)
            .detail(detail)
            .code(ErrorCodes.UPSTREAM_UNAVAILABLE)
            .retryable(true)
            .build();
    }

    public static ServiceErrorResponse unauthorized() {
        return builder()
            .type("about:blank")
            .title("Unauthorized")
            .status(401)
            .detail("Authentication required")
            .code(ErrorCodes.UNAUTHORIZED)
            .retryable(false)
            .build();
    }

    public static ServiceErrorResponse forbidden() {
        return builder()
            .type("about:blank")
            .title("Forbidden")
            .status(403)
            .detail("Insufficient permissions")
            .code(ErrorCodes.FORBIDDEN)
            .retryable(false)
            .build();
    }

    public static ServiceErrorResponse internalError(String detail) {
        return builder()
            .type("about:blank")
            .title("Internal Server Error")
            .status(500)
            .detail(detail)
            .code(ErrorCodes.INTERNAL_ERROR)
            .retryable(false)
            .build();
    }

    public static class ServiceErrorResponseBuilder {
        private String type = "about:blank";
        private boolean retryable = false;
        private List<ServiceError> errors = new ArrayList<>();

        public ServiceErrorResponseBuilder errors(List<ServiceError> errs) {
            this.errors.addAll(errs);
            return this;
        }

        public ServiceErrorResponseBuilder addError(ServiceError e) {
            this.errors.add(e);
            return this;
        }

        public ServiceErrorResponse build() {
            Objects.requireNonNull(title, "title must not be null");
            Objects.requireNonNull(code, "code must not be null");
            Instant resolvedTimestamp = timestamp != null ? timestamp : Instant.now();
            List<ServiceError> resolvedErrors =
                Collections.unmodifiableList(new ArrayList<>(errors != null ? errors : List.of()));
            return new ServiceErrorResponse(
                type, title, status, detail, code, retryable, upstreamCode,
                resolvedTimestamp, resolvedErrors);
        }
    }
}
