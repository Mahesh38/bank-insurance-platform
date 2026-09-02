package com.bank.common.error;

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
 *
 * <p>Most services need no subclass at all — {@code PlatformErrorAutoConfiguration} registers this
 * advice from configuration. Extend it only to add handlers for exception types specific to one
 * service, as the BFF does for its authentication paths.
 *
 * <h2>Two things this class deliberately does not do</h2>
 * It does not decide <em>what</em> an error means — {@link ErrorCatalogue} does — and it does not
 * write the log line or the metric — {@link ErrorRecorder} does. What is left is the HTTP contract:
 * map, stamp, record, redact, respond, in that order.
 *
 * <p>The order is the point. Redaction runs <strong>after</strong> recording, because redacting
 * first would destroy the evidence redaction exists to protect and leave support holding an
 * incident id that points at nothing.
 */
public class PlatformErrorHandler {

    private final ErrorHandlingSettings settings;
    private final ErrorRecorder recorder;

    public PlatformErrorHandler(ErrorHandlingSettings settings, ErrorRecorder recorder) {
        this.settings = settings;
        this.recorder = recorder != null ? recorder : ErrorRecorder.NONE;
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

        return emit(ServiceErrorResponse.of(ErrorCodes.VALIDATION_ERROR)
            .status(settings.validationStatus())
            .errors(fieldErrors)
            .build(), null);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ServiceErrorResponse> handleUnreadable(HttpMessageNotReadableException ex) {
        // The parser's message names offsets, classes and sometimes the payload. It is a reason,
        // never a detail.
        return emit(ServiceErrorResponse.of(ErrorCodes.INVALID_REQUEST)
            .status(settings.malformedBodyStatus())
            .diagnostic(ErrorDiagnostic.builder(ErrorCodes.INVALID_REQUEST)
                .service(settings.serviceId())
                .layer(settings.layer())
                .reason("request body could not be parsed")
                .cause(ex)
                .build())
            .build(), null);
    }

    /** Stamps identity, records the diagnostic, then redacts if this service faces a client. */
    protected ResponseEntity<ServiceErrorResponse> emit(ServiceErrorResponse response, Throwable cause) {
        ServiceErrorResponse stamped = response
            .withService(settings.serviceId())
            .withIncidentId(IncidentId.generate())
            // Seeded at the edge by RequestDiagnosticFilter. Stamping it here is what lets a
            // caller quote one id and support pull every line of the request, not just the failure.
            .withCorrelationId(org.slf4j.MDC.get(com.bank.common.observability.MdcKeys.CORRELATION_ID));

        recorder.record(stamped, cause);

        ServiceErrorResponse body = settings.redacts() ? stamped.toPublic() : stamped;

        return ResponseEntity.status(body.getStatus())
            .contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .body(body);
    }

    /** This service's identity, layer and boundary. */
    protected ErrorHandlingSettings settings() {
        return settings;
    }
}
