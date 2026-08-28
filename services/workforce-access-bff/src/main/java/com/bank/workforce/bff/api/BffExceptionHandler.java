package com.bank.workforce.bff.api;

import com.bank.common.error.ErrorCodes;
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
 * <p>L4 is the last hop that may hold a diagnostic and the first that must never emit one
 * ({@code 07-PLATFORM-ERROR-CONTRACT.md §4.4}), so this handler is declared {@code PUBLIC} and
 * every response it produces goes through {@code toPublic()} after the diagnostic is logged.
 *
 * <p>The asymmetry this class used to encode — a validation failure explains itself, an
 * authentication failure deliberately does not — has become a single rule that covers both.
 * Neither path echoes an internal exception message any more, because the validation path could
 * not keep that promise: the {@link IllegalArgumentException}s reaching it include
 * {@code "workforce.session.encryption-key must decode to 32 bytes"}, which is an internal
 * configuration detail, and nothing in the type system stopped it from reaching a device.
 *
 * <p>Nothing is lost for debugging. The message moves to the diagnostic, is logged with the
 * incident id, and the caller is handed that id — so support can find the exact text the caller
 * was not shown.
 */
@RestControllerAdvice
public class BffExceptionHandler extends PlatformErrorHandler {

    public BffExceptionHandler() {
        super("bff", PlatformLayer.L4, Boundary.PUBLIC);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ServiceErrorResponse> invalidRequest(IllegalArgumentException exception) {
        return emit(ServiceException.of(ErrorCodes.SCHEMA_INVALID)
            .service(serviceId())
            .layer(PlatformLayer.L4)
            .component("AuthenticationController")
            .reason(exception.getMessage())
            .cause(exception)
            .build()
            .getErrorResponse(), exception);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ServiceErrorResponse> authenticationDenied(IllegalStateException exception) {
        return emit(ServiceException.of(ErrorCodes.AUTHENTICATION_FAILED)
            .service(serviceId())
            .layer(PlatformLayer.L4)
            .component("LoginService")
            .reason(exception.getMessage())
            .cause(exception)
            .build()
            .getErrorResponse(), exception);
    }
}
