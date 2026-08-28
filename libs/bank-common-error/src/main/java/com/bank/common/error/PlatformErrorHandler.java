package com.bank.common.error;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.List;

/**
 * The one place an error becomes an HTTP response — work item {@code ERR-002}.
 *
 * <p>Before this class each service ran its own advice, and the three had drifted into three
 * contracts: two shapes of the platform envelope with different statuses for the same condition,
 * and a bare Spring {@code ProblemDetail} at the BFF with no {@code code} at all (defect D6).
 * A service now extends this and declares only what is genuinely local to it.
 *
 * <pre>{@code
 * @RestControllerAdvice
 * public class GlobalExceptionHandler extends PlatformErrorHandler {
 *     public GlobalExceptionHandler() {
 *         super("onesb", PlatformLayer.L5, Boundary.INTERNAL);
 *     }
 * }
 * }</pre>
 *
 * <h2>Redaction</h2>
 * The boundary is declared, and it <strong>defaults to {@link Boundary#PUBLIC}</strong>: a service
 * that says nothing redacts. A new service that forgets to declare its position leaks nothing,
 * which is the only safe direction for that mistake to fall.
 *
 * <p>The diagnostic is always logged <em>before</em> redaction. Redaction that ran first would
 * destroy the evidence it exists to protect, and support would be left with an incident id
 * pointing at nothing.
 *
 * <p>Contract: {@code docs/journey-execution/07-PLATFORM-ERROR-CONTRACT.md §4.4}.
 */
public abstract class PlatformErrorHandler {

    /** Where this service sits relative to the trust boundary. */
    public enum Boundary {
        /** Faces a client. Diagnostics are stripped and wording comes from the catalogue. */
        PUBLIC,
        /** Cluster-private. Diagnostics travel to the calling service, which redacts in its turn. */
        INTERNAL
    }

    private static final Logger log = LoggerFactory.getLogger(PlatformErrorHandler.class);

    private final String serviceId;
    private final PlatformLayer layer;
    private final Boundary boundary;

    /** Declares a public-facing service. */
    protected PlatformErrorHandler(String serviceId, PlatformLayer layer) {
        this(serviceId, layer, Boundary.PUBLIC);
    }

    protected PlatformErrorHandler(String serviceId, PlatformLayer layer, Boundary boundary) {
        this.serviceId = serviceId;
        this.layer = layer;
        this.boundary = boundary != null ? boundary : Boundary.PUBLIC;
    }

    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<ServiceErrorResponse> handleServiceException(ServiceException ex) {
        return emit(ex.getErrorResponse(), ex);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ServiceErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<ServiceError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
            .map(fe -> ServiceError.ofField(
                ErrorCodes.MISSING_REQUIRED_FIELD,
                fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "invalid",
                fe.getField()))
            .toList();

        ServiceErrorResponse body = ServiceErrorResponse.builder()
            .type("about:blank")
            .title("Validation Failed")
            .status(validationStatus())
            .detail("Request validation failed")
            .code(ErrorCodes.VALIDATION_ERROR)
            .category(ErrorCategory.VALIDATION)
            .retryable(false)
            .errors(fieldErrors)
            .incidentId(IncidentId.generate())
            .build();

        return emit(body, null);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ServiceErrorResponse> handleUnreadable(HttpMessageNotReadableException ex) {
        // The parser's message names offsets, classes and sometimes the payload. It is a reason,
        // never a detail.
        ServiceErrorResponse body = ServiceErrorResponse.builder()
            .type("about:blank")
            .title("Validation Failed")
            .status(malformedBodyStatus())
            .detail("Malformed or incomplete request body")
            .code(ErrorCodes.INVALID_REQUEST)
            .category(ErrorCategory.VALIDATION)
            .retryable(false)
            .diagnostic(ErrorDiagnostic.builder(ErrorCodes.INVALID_REQUEST)
                .service(serviceId)
                .layer(layer)
                .reason("request body could not be parsed")
                .cause(ex)
                .build())
            .build();

        return emit(body, null);
    }

    /**
     * The status this service returns for a bean-validation failure.
     *
     * <p>Overridable because the two existing services genuinely differ and both are published:
     * {@code 1sb-integration-service} answers 422 for FUNC-001/FUNC-002, {@code bank-persistence-service}
     * answers 400. Unifying them here would be a silent breaking change to a live contract, which
     * is exactly what {@code ADR-017}'s additive-only constraint forbids.
     */
    protected int validationStatus() {
        return 422;
    }

    /** The status for an unparseable body. Defaults to {@link #validationStatus()}. */
    protected int malformedBodyStatus() {
        return validationStatus();
    }

    /**
     * Stamps identity, records the diagnostic, then redacts if this service faces a client.
     *
     * <p>Order matters and is the point of the method: log, then redact.
     */
    protected ResponseEntity<ServiceErrorResponse> emit(ServiceErrorResponse response, Throwable cause) {
        ServiceErrorResponse stamped = response
            .withService(serviceId)
            .withIncidentId(IncidentId.generate());

        record(stamped, cause);

        ServiceErrorResponse body = boundary == Boundary.PUBLIC ? stamped.toPublic() : stamped;

        return ResponseEntity.status(body.getStatus())
            .contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .body(body);
    }

    /**
     * Writes the engineer-facing half.
     *
     * <p>Level follows the category, not the status: a caller's invalid request is a normal
     * outcome and logs at {@code WARN} with no stack, while a dependency or platform failure logs
     * at {@code ERROR} with one. {@code ERR-004} replaces this with the full structured record and
     * the {@code bank.error.count} series; the shape of the message is already the six questions.
     */
    protected void record(ServiceErrorResponse body, Throwable cause) {
        ErrorDiagnostic d = body.getDiagnostic();
        boolean clientCaused = body.getCategory() != null && body.getCategory().clientCaused();

        String message = "error incidentId={} code={} category={} service={} layer={} "
            + "originService={} status={} correlationId={} runbook={} reason={}";
        Object[] args = {
            body.getIncidentId(),
            body.getCode(),
            body.getCategory(),
            body.getService(),
            d != null && d.getLayer() != null ? d.getLayer() : layer,
            d != null ? d.effectiveOriginService() : body.getService(),
            body.getStatus(),
            body.getCorrelationId(),
            d != null ? d.getRunbook() : ErrorCatalogue.find(body.getCode())
                .map(ErrorDefinition::runbook).orElse(null),
            d != null ? d.getReason() : null
        };

        if (clientCaused) {
            log.warn(message, args);
        } else if (cause != null) {
            log.error(message, appendCause(args, cause));
        } else {
            log.error(message, args);
        }
    }

    private static Object[] appendCause(Object[] args, Throwable cause) {
        Object[] withCause = new Object[args.length + 1];
        System.arraycopy(args, 0, withCause, 0, args.length);
        withCause[args.length] = cause;
        return withCause;
    }

    /** The service id stamped on every response this handler emits. */
    protected String serviceId() {
        return serviceId;
    }

    /** The layer this service occupies. */
    protected PlatformLayer layer() {
        return layer;
    }

    /** Whether this handler redacts before responding. */
    protected Boundary boundary() {
        return boundary;
    }
}
