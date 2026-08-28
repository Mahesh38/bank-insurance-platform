package com.bank.common.error;

import lombok.Getter;

import java.util.List;

/**
 * Unchecked exception carrying a {@link ServiceErrorResponse} from service and domain layers up to
 * the shared exception handler.
 *
 * <p>Built from the catalogue, which supplies status, wording and retryability so no throw site
 * decides them:
 *
 * <pre>{@code
 * throw errors.error(ErrorCodes.SUITABILITY_REQUIRED)
 *         .component("QuoteService").operation("createQuote")
 *         .reason("assessment " + assessmentId + " expired at " + expiry)
 *         .build();
 * }</pre>
 *
 * <p>The pre-catalogue factories ({@code upstreamAuth}, {@code validation}, {@code forbidden} and
 * the rest) are gone. They had no production call sites left after every throw site moved to the
 * catalogue, and they were the defect this contract removes preserved as public API: each one
 * hand-built an envelope with a literal title and status, which is how one condition acquires three
 * different responses. The ArchUnit rule stops a service reaching for
 * {@code ServiceErrorResponse.builder()}; leaving these would have left the same door open one
 * method along.
 */
@Getter
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

    public int getHttpStatus() {
        return errorResponse.getStatus();
    }

    public boolean isRetryable() {
        return errorResponse.isRetryable();
    }

    /** The engineer-facing half. */
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
     * <p>Prefer the injected {@link ServiceErrors#error(String)}, which also stamps the service and
     * layer; this is the entry point it delegates to.
     *
     * @throws IllegalArgumentException if the code is not registered
     */
    public static Builder of(String code) {
        return new Builder(code);
    }

    /**
     * Builds a {@link ServiceException} and its {@link ErrorDiagnostic} together.
     *
     * <p>The context methods forward to the diagnostic builder. They are kept as a flat fluent API
     * rather than exposing the diagnostic builder directly because a throw site reads better as one
     * chain, and because {@code reason} and {@code incidentId} need behaviour of their own.
     */
    public static final class Builder {

        private final String code;
        private final ErrorDefinition definition;
        private final ErrorDiagnostic.Builder diagnostic;
        private String correlationId;
        private Throwable cause;
        private List<ServiceError> errors = List.of();
        private String reason;
        private Integer statusOverride;
        private String statusOverrideAuthority;

        private Builder(String code) {
            this.definition = ErrorCatalogue.require(code);
            this.code = code;
            this.diagnostic = ErrorDiagnostic.builder(code);
        }

        public Builder service(String v)       { diagnostic.service(v); return this; }
        public Builder layer(PlatformLayer v)  { diagnostic.layer(v); return this; }
        public Builder component(String v)     { diagnostic.component(v); return this; }
        public Builder operation(String v)     { diagnostic.operation(v); return this; }
        public Builder remediation(String v)   { diagnostic.remediation(v); return this; }
        public Builder origin(ErrorOrigin v)   { diagnostic.origin(v); return this; }
        public Builder correlationId(String v) { this.correlationId = v; return this; }
        public Builder errors(List<ServiceError> v) { this.errors = v; return this; }

        public Builder upstream(String system, String upstreamCode, Integer status) {
            diagnostic.upstream(system, upstreamCode, status);
            return this;
        }

        /** Held rather than forwarded, because a status override appends to it at build time. */
        public Builder reason(String v) {
            this.reason = v;
            return this;
        }

        /**
         * Adopts an upstream's incident id. Ignores a null, so a peer that sent none leaves the
         * generated id in place rather than blanking the only token support can search on.
         */
        public Builder incidentId(String v) {
            if (v != null) {
                diagnostic.incidentId(v);
            }
            return this;
        }

        public Builder cause(Throwable t) {
            this.cause = t;
            diagnostic.cause(t);
            return this;
        }

        /**
         * Emits a status other than the catalogue's, to preserve a behaviour a human already
         * ratified.
         *
         * <p><strong>This is not a convenience.</strong> It exists for one situation: two ratified
         * documents disagree about a code, and neither may be silently overruled by a library. The
         * live example is {@code QUOTE_EXPIRED} — catalogue 04 §6, {@code VR-082} and
         * {@code INV-QUO-04} say 409 for an offer selected past its validity window, while
         * {@code FUNC-004} AC-2 (TL + QA approved) says 410 for a proposal schema requested against
         * a quote job that is gone. Two conditions wearing one code; deciding which keeps the name
         * is the catalogue owner's call, not this library's.
         *
         * <p>{@code ratifiedBy} is mandatory, so {@code grep -rn statusOverride} enumerates every
         * outstanding discrepancy. Every use belongs in
         * {@code 07-PLATFORM-ERROR-CONTRACT.md §13}.
         *
         * @throws IllegalArgumentException if no authority is named
         */
        public Builder statusOverride(int status, String ratifiedBy) {
            if (ratifiedBy == null || ratifiedBy.isBlank()) {
                throw new IllegalArgumentException(
                    "statusOverride requires the document that ratified the departure from the "
                        + "catalogue — an unattributed override is just the defect the catalogue removes");
            }
            this.statusOverride = status;
            this.statusOverrideAuthority = ratifiedBy;
            return this;
        }

        public ServiceException build() {
            ErrorDiagnostic d = diagnostic.reason(resolvedReason()).build();

            ServiceErrorResponse response = ServiceErrorResponse.builder()
                .title(definition.publicTitle())
                .status(statusOverride != null ? statusOverride : definition.httpStatus())
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

        private String resolvedReason() {
            if (statusOverride == null) {
                return reason;
            }
            String note = "status overridden to " + statusOverride + " per " + statusOverrideAuthority;
            return reason == null || reason.isBlank() ? note : reason + " (" + note + ")";
        }
    }
}
