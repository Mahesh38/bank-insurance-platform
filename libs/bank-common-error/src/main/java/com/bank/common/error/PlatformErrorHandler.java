package com.bank.common.error;

import com.bank.common.observability.ErrorMetrics;
import com.bank.common.observability.MdcContext;
import com.bank.common.observability.MdcKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
    private final ErrorMetrics metrics;

    /** Declares a public-facing service. */
    protected PlatformErrorHandler(String serviceId, PlatformLayer layer) {
        this(serviceId, layer, Boundary.PUBLIC);
    }

    protected PlatformErrorHandler(String serviceId, PlatformLayer layer, Boundary boundary) {
        this(serviceId, layer, boundary, null);
    }

    /**
     * @param metrics records the {@code bank.error.count} series; null disables metric emission
     *                without disabling anything else, so a service can adopt the contract before
     *                it has a registry wired
     */
    protected PlatformErrorHandler(String serviceId, PlatformLayer layer, Boundary boundary,
                                   ErrorMetrics metrics) {
        this.serviceId = serviceId;
        this.layer = layer;
        this.boundary = boundary != null ? boundary : Boundary.PUBLIC;
        this.metrics = metrics;
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
            .withIncidentId(IncidentId.generate())
            // Seeded at the edge by RequestDiagnosticFilter. Stamping it here is what lets a
            // caller quote one id and support pull every line of the request, not just the failure.
            .withCorrelationId(org.slf4j.MDC.get(MdcKeys.CORRELATION_ID));

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
        ErrorCategory category = body.getCategory();
        boolean clientCaused = category != null && category.clientCaused();

        PlatformLayer effectiveLayer = d != null && d.getLayer() != null ? d.getLayer() : layer;
        String originService = d != null ? d.effectiveOriginService() : body.getService();
        String runbook = d != null ? d.getRunbook()
            : ErrorCatalogue.find(body.getCode()).map(ErrorDefinition::runbook).orElse(null);

        // MDC, so the error line is findable by the same id the caller was handed — and so any
        // line logged inside this block carries it too.
        Map<String, String> context = new LinkedHashMap<>();
        context.put(MdcKeys.INCIDENT_ID, body.getIncidentId());
        context.put(MdcKeys.ERROR_CODE, body.getCode());
        context.put(MdcKeys.ERROR_CATEGORY, category != null ? category.name() : null);
        context.put(MdcKeys.SERVICE, body.getService());
        context.put(MdcKeys.ORIGIN_SERVICE, originService);
        context.put(MdcKeys.LAYER, effectiveLayer != null ? effectiveLayer.name() : null);
        if (body.getCorrelationId() != null) {
            context.put(MdcKeys.CORRELATION_ID, body.getCorrelationId());
        }

        MdcContext.with(context, () -> {
            String message = "error code={} category={} service={} layer={} originService={} "
                + "status={} runbook={} reason={}";
            Object[] args = {
                body.getCode(), category, body.getService(), effectiveLayer, originService,
                body.getStatus(), runbook, d != null ? d.getReason() : null
            };
            if (clientCaused) {
                // A caller's invalid request is a normal outcome of a public API. Logging it at
                // ERROR with a stack makes the dashboard unreadable within a week.
                log.warn(message, args);
            } else if (cause != null) {
                log.error(message, appendCause(args, cause));
            } else {
                log.error(message, args);
            }
        });

        if (metrics != null) {
            metrics.record(
                body.getService(),
                body.getCode(),
                category != null ? category.name() : null,
                effectiveLayer != null ? effectiveLayer.name() : null,
                originService,
                body.isRetryable(),
                body.getStatus());
        }
    }

    private static Object[] appendCause(Object[] args, Throwable cause) {
        Object[] withCause = new Object[args.length + 1];
        System.arraycopy(args, 0, withCause, 0, args.length);
        withCause[args.length] = cause;
        return withCause;
    }

    /**
     * Builds an {@link ErrorMetrics} from an optional registry.
     *
     * <p>A {@code @WebMvcTest} slice has no {@code MeterRegistry}, and a missing registry must not
     * stop the advice from loading — a service losing its error responses because it could not
     * count them would be the wrong failure by a wide margin. Metric emission is the optional
     * part; the response and the log are not.
     */
    protected static ErrorMetrics errorMetrics(
            org.springframework.beans.factory.ObjectProvider<io.micrometer.core.instrument.MeterRegistry> registries) {
        io.micrometer.core.instrument.MeterRegistry registry =
            registries != null ? registries.getIfAvailable() : null;
        return registry != null ? new ErrorMetrics(registry) : null;
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
