package com.bank.common.error;

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
public final class ServiceErrorResponse {

    private final String    type;
    private final String    title;
    private final int       status;
    private final String    detail;
    private final String    code;
    private final boolean   retryable;
    private final String    upstreamCode;
    private final Instant   timestamp;
    private final List<ServiceError> errors;

    private ServiceErrorResponse(Builder b) {
        this.type         = b.type;
        this.title        = Objects.requireNonNull(b.title,  "title must not be null");
        this.status       = b.status;
        this.detail       = b.detail;
        this.code         = Objects.requireNonNull(b.code,   "code must not be null");
        this.retryable    = b.retryable;
        this.upstreamCode = b.upstreamCode;
        this.timestamp    = b.timestamp != null ? b.timestamp : Instant.now();
        this.errors       = Collections.unmodifiableList(new ArrayList<>(b.errors));
    }

    // --- Accessors ---

    public String  getType()         { return type; }
    public String  getTitle()        { return title; }
    public int     getStatus()       { return status; }
    public String  getDetail()       { return detail; }
    public String  getCode()         { return code; }
    public boolean isRetryable()     { return retryable; }
    public String  getUpstreamCode() { return upstreamCode; }
    public Instant getTimestamp()    { return timestamp; }
    public List<ServiceError> getErrors() { return errors; }

    // --- Factory shortcuts ---

    public static ServiceErrorResponse validation(String detail, List<ServiceError> fieldErrors) {
        return new Builder()
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
        return new Builder()
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
        return new Builder()
            .type("about:blank")
            .title("Upstream Unavailable")
            .status(503)
            .detail(detail)
            .code(ErrorCodes.UPSTREAM_UNAVAILABLE)
            .retryable(true)
            .build();
    }

    public static ServiceErrorResponse unauthorized() {
        return new Builder()
            .type("about:blank")
            .title("Unauthorized")
            .status(401)
            .detail("Authentication required")
            .code(ErrorCodes.UNAUTHORIZED)
            .retryable(false)
            .build();
    }

    public static ServiceErrorResponse forbidden() {
        return new Builder()
            .type("about:blank")
            .title("Forbidden")
            .status(403)
            .detail("Insufficient permissions")
            .code(ErrorCodes.FORBIDDEN)
            .retryable(false)
            .build();
    }

    public static ServiceErrorResponse internalError(String detail) {
        return new Builder()
            .type("about:blank")
            .title("Internal Server Error")
            .status(500)
            .detail(detail)
            .code(ErrorCodes.INTERNAL_ERROR)
            .retryable(false)
            .build();
    }

    // --- Builder ---

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String type = "about:blank";
        private String title;
        private int    status;
        private String detail;
        private String code;
        private boolean retryable = false;
        private String upstreamCode;
        private Instant timestamp;
        private final List<ServiceError> errors = new ArrayList<>();

        private Builder() {}

        public Builder type(String type)               { this.type = type;               return this; }
        public Builder title(String title)             { this.title = title;             return this; }
        public Builder status(int status)              { this.status = status;           return this; }
        public Builder detail(String detail)           { this.detail = detail;           return this; }
        public Builder code(String code)               { this.code = code;               return this; }
        public Builder retryable(boolean retryable)    { this.retryable = retryable;     return this; }
        public Builder upstreamCode(String upstream)   { this.upstreamCode = upstream;   return this; }
        public Builder timestamp(Instant ts)           { this.timestamp = ts;            return this; }
        public Builder errors(List<ServiceError> errs) { this.errors.addAll(errs);       return this; }
        public Builder addError(ServiceError e)        { this.errors.add(e);             return this; }

        public ServiceErrorResponse build() { return new ServiceErrorResponse(this); }
    }

    @Override
    public String toString() {
        return "ServiceErrorResponse{status=" + status + ", code='" + code + "', detail='" + detail + "'}";
    }
}
