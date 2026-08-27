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
 *
 * <h2>Two renderings</h2>
 * This type carries both halves of a failure. Below the redaction boundary — service to service —
 * it may carry {@link #getOrigin() origin} and {@link #getDiagnostic() diagnostic}, so a calling
 * service can see which service actually failed. At the boundary, {@link #toPublic()} strips them
 * and replaces the wording with the catalogue's safe text.
 *
 * <p><strong>The BFF (L4) is that boundary</strong> — the last hop that may hold a diagnostic and
 * the first that must never emit one
 * ({@code docs/journey-execution/07-PLATFORM-ERROR-CONTRACT.md §4.4}).
 *
 * <p>Prefer {@link #of(String)} over the raw builder: it takes status, title, detail, retryability
 * and category from {@link ErrorCatalogue}, so the same code cannot be worded two ways in two
 * services.
 */
@Value
@Builder
@ToString(of = {"status", "code", "detail", "incidentId", "service"})
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

    // --- Added by ERR-001. Additive: existing fields and their values are unchanged. ---

    /** How the caller must treat this failure. Null only on responses built before the catalogue. */
    ErrorCategory category;

    /** The service that produced this response. */
    String service;

    /** Names this failure across every hop and every log line. Safe to show an end user. */
    String incidentId;

    /** Groups every hop of the request this failure belongs to. */
    String correlationId;

    /** Where the failure first occurred. <strong>Diagnostic — stripped at the boundary.</strong> */
    ErrorOrigin origin;

    /** The engineer-facing half. <strong>Diagnostic — stripped at the boundary.</strong> */
    ErrorDiagnostic diagnostic;

    // --- Catalogue-driven construction ---

    /**
     * A response for {@code code}, with status, title, detail, retryability and category taken
     * from {@link ErrorCatalogue}, and a fresh incident id.
     *
     * @throws IllegalArgumentException if the code is not registered
     */
    public static ServiceErrorResponseBuilder of(String code) {
        ErrorDefinition d = ErrorCatalogue.require(code);
        return builder()
            .type("about:blank")
            .title(d.publicTitle())
            .status(d.httpStatus())
            .detail(d.publicDetail())
            .code(d.code())
            .category(d.category())
            .retryable(d.retryability().toBoolean())
            .incidentId(IncidentId.generate());
    }

    /**
     * The rendering safe to send across a trust boundary.
     *
     * <p>Drops {@code origin} and {@code diagnostic}, and — when the code is registered — replaces
     * {@code title} and {@code detail} with the catalogue's fixed text, so no upstream body,
     * internal route or vendor name can survive in the wording. {@code incidentId} is kept
     * deliberately: it is what lets support find the diagnostic that was <em>not</em> sent.
     */
    public ServiceErrorResponse toPublic() {
        ErrorDefinition d = ErrorCatalogue.find(code).orElse(null);
        return new ServiceErrorResponse(
            type,
            d != null ? d.publicTitle() : title,
            status,
            d != null ? d.publicDetail() : detail,
            code,
            retryable,
            upstreamCode,
            timestamp,
            errors,
            category,
            service,
            incidentId,
            correlationId,
            null,
            null);
    }

    /** True when nothing on this response may cross a trust boundary unchanged. */
    public boolean carriesDiagnostics() {
        return origin != null || diagnostic != null;
    }

    // --- Factory shortcuts ---

    public static ServiceErrorResponse validation(String detail, List<ServiceError> fieldErrors) {
        return builder()
            .type("about:blank")
            .title("Validation Failed")
            .status(400)
            .detail(detail)
            .code(ErrorCodes.VALIDATION_ERROR)
            .category(ErrorCategory.VALIDATION)
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
            .category(ErrorCategory.UPSTREAM)
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
            .category(ErrorCategory.UPSTREAM)
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
            .category(ErrorCategory.AUTHENTICATION)
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
            .category(ErrorCategory.AUTHORIZATION)
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
            .category(ErrorCategory.INTERNAL)
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

        /**
         * Attaches the diagnostic and adopts its incident id and origin, so the two halves of one
         * failure cannot drift apart.
         */
        public ServiceErrorResponseBuilder diagnostic(ErrorDiagnostic d) {
            this.diagnostic = d;
            if (d != null) {
                if (this.incidentId == null) {
                    this.incidentId = d.getIncidentId();
                }
                if (this.origin == null) {
                    this.origin = d.getOrigin();
                }
                if (this.service == null) {
                    this.service = d.getService();
                }
            }
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
                resolvedTimestamp, resolvedErrors,
                category, service, incidentId, correlationId, origin, diagnostic);
        }
    }
}
