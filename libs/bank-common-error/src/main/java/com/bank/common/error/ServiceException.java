package com.bank.common.error;

import java.util.List;

/**
 * Unchecked exception carrying a {@link ServiceErrorResponse} for propagation
 * from service/domain layers up to controller/exception-handler advice.
 *
 * <p>Prefer the catalogue-driven form, which takes status, wording and retryability from
 * {@link ErrorCatalogue} and carries the engineer-facing half separately:
 * <pre>{@code
 * throw ServiceException.of(ErrorCodes.SUITABILITY_REQUIRED)
 *         .service("onesb")
 *         .layer(PlatformLayer.L6)
 *         .component("QuoteService").operation("createQuote")
 *         .reason("assessment " + assessmentId + " expired at " + expiry)
 *         .build();
 * }</pre>
 *
 * <p>The older form remains for call sites not yet migrated:
 * <pre>{@code
 * throw ServiceException.upstreamAuth("1SB returned 401 – check API key");
 * }</pre>
 */
public class ServiceException extends RuntimeException {

    private final ServiceErrorResponse errorResponse;

    public ServiceException(ServiceErrorResponse errorResponse) {
        super(errorResponse.getDetail());
        this.errorResponse = errorResponse;
    }

    public ServiceException(ServiceErrorResponse errorResponse, Throwable cause) {
        super(errorResponse.getDetail(), cause);
        this.errorResponse = errorResponse;
    }

    public ServiceErrorResponse getErrorResponse() {
        return errorResponse;
    }

    public int getHttpStatus() {
        return errorResponse.getStatus();
    }

    public boolean isRetryable() {
        return errorResponse.isRetryable();
    }


    /** The engineer-facing half, when this exception was built from the catalogue. */
    public ErrorDiagnostic getDiagnostic() {
        return errorResponse.getDiagnostic();
    }

    /** Names this failure across every hop and every log line. */
    public String getIncidentId() {
        return errorResponse.getIncidentId();
    }

    /**
     * Starts a catalogue-driven exception for {@code code}.
     *
     * <p>The response wording, status and retryability come from {@link ErrorCatalogue}; the
     * caller supplies only what the catalogue cannot know — where it happened, why, and the cause.
     *
     * @throws IllegalArgumentException if the code is not registered
     */
    public static Builder of(String code) {
        return new Builder(code);
    }

    /** Builds a {@link ServiceException} and its {@link ErrorDiagnostic} together. */
    public static final class Builder {
        private final String code;
        private final ErrorDefinition definition;
        private final ErrorDiagnostic.Builder diagnostic;
        private String correlationId;
        private Throwable cause;
        private java.util.List<ServiceError> errors = java.util.List.of();

        private Builder(String code) {
            this.definition = ErrorCatalogue.require(code);
            this.code = code;
            this.diagnostic = ErrorDiagnostic.builder(code);
        }

        public Builder service(String v)              { diagnostic.service(v); return this; }
        public Builder layer(PlatformLayer v)         { diagnostic.layer(v); return this; }
        public Builder component(String v)            { diagnostic.component(v); return this; }
        public Builder operation(String v)            { diagnostic.operation(v); return this; }
        public Builder reason(String v)               { diagnostic.reason(v); return this; }
        public Builder remediation(String v)          { diagnostic.remediation(v); return this; }
        public Builder origin(ErrorOrigin v)          { diagnostic.origin(v); return this; }
        public Builder incidentId(String v)           { diagnostic.incidentId(v); return this; }
        public Builder correlationId(String v)        { this.correlationId = v; return this; }
        public Builder errors(java.util.List<ServiceError> v) { this.errors = v; return this; }

        public Builder upstream(String system, String upstreamCode, Integer status) {
            diagnostic.upstream(system, upstreamCode, status);
            return this;
        }

        public Builder cause(Throwable t) {
            this.cause = t;
            diagnostic.cause(t);
            return this;
        }

        public ServiceException build() {
            ErrorDiagnostic d = diagnostic.build();
            ServiceErrorResponse response = ServiceErrorResponse.builder()
                .type("about:blank")
                .title(definition.publicTitle())
                .status(definition.httpStatus())
                .detail(definition.publicDetail())
                .code(code)
                .category(definition.category())
                .retryable(definition.retryability().toBoolean())
                .correlationId(correlationId)
                .upstreamCode(d.getUpstreamCode())
                .errors(errors)
                .diagnostic(d)
                .build();
            return cause != null
                ? new ServiceException(response, cause)
                : new ServiceException(response);
        }
    }

    // --- Factory shortcuts ---

    public static ServiceException validation(String detail, List<ServiceError> errors) {
        return new ServiceException(ServiceErrorResponse.validation(detail, errors));
    }

    public static ServiceException upstreamBusiness(String detail, String upstreamCode) {
        return new ServiceException(ServiceErrorResponse.upstreamBusiness(detail, upstreamCode));
    }

    public static ServiceException upstreamUnavailable(String detail, Throwable cause) {
        return new ServiceException(ServiceErrorResponse.upstreamUnavailable(detail), cause);
    }

    public static ServiceException upstreamAuth(String detail) {
        return new ServiceException(
            ServiceErrorResponse.builder()
                .title("Upstream Authentication Failure")
                .status(502)
                .detail(detail)
                .code(ErrorCodes.UPSTREAM_AUTH_FAILURE)
                .retryable(false)
                .build()
        );
    }

    public static ServiceException unauthorized() {
        return new ServiceException(ServiceErrorResponse.unauthorized());
    }

    public static ServiceException forbidden() {
        return new ServiceException(ServiceErrorResponse.forbidden());
    }

    public static ServiceException internal(String detail, Throwable cause) {
        return new ServiceException(ServiceErrorResponse.internalError(detail), cause);
    }
}
