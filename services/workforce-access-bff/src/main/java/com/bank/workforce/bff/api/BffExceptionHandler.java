package com.bank.workforce.bff.api;

import com.bank.common.error.ErrorCodes;
import com.bank.common.error.ErrorHandlingSettings;
import com.bank.common.error.ErrorRecorder;
import com.bank.common.error.PlatformErrorHandler;
import com.bank.common.error.PlatformLayer;
import com.bank.common.error.ServiceErrorResponse;
import com.bank.common.error.ServiceException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * The BFF error contract. <strong>This service is the redaction boundary.</strong>
 *
 * <p>It keeps a subclass where the other services no longer need one, because it has handlers
 * nothing else has: the authentication paths throw plain {@link IllegalArgumentException} and
 * {@link IllegalStateException}, and those must be mapped rather than escaping as a bare 500.
 * Identity, layer and boundary come from configuration like everywhere else.
 *
 * <p>The asymmetry this class used to encode — a validation failure explains itself, an
 * authentication failure deliberately does not — became a single rule covering both. The validation
 * path could not keep the promise: the {@link IllegalArgumentException}s reaching it include
 * {@code "workforce.session.encryption-key must decode to 32 bytes"}, an internal configuration
 * detail, and nothing in the type system stopped it reaching a device.
 *
 * <p>Nothing is lost for debugging. The message moves to the diagnostic, is logged with the
 * incident id, and the caller is handed that id.
 */
@RestControllerAdvice
public class BffExceptionHandler extends PlatformErrorHandler {

    public BffExceptionHandler(ErrorHandlingSettings settings, ErrorRecorder recorder) {
        super(settings, recorder);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ServiceErrorResponse> invalidRequest(IllegalArgumentException exception) {
        return emit(authFailure(ErrorCodes.SCHEMA_INVALID, "AuthenticationController", exception), exception);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ServiceErrorResponse> authenticationDenied(IllegalStateException exception) {
        return emit(authFailure(ErrorCodes.AUTHENTICATION_FAILED, "LoginService", exception), exception);
    }

    private ServiceErrorResponse authFailure(String code, String component, RuntimeException exception) {
        return ServiceException.of(code)
            .service(settings().serviceId())
            .layer(PlatformLayer.L4)
            .component(component)
            .reason(exception.getMessage())
            .cause(exception)
            .build()
            .getErrorResponse();
    }
}
