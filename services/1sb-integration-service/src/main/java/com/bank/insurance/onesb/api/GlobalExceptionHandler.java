package com.bank.insurance.onesb.api;

import com.bank.common.error.ErrorCodes;
import com.bank.common.error.ServiceError;
import com.bank.common.error.ServiceErrorResponse;
import com.bank.common.error.ServiceException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Maps domain {@link ServiceException} and bean-validation failures to problem+json.
 * Validation failures use HTTP 422 (FUNC-001 master-data, FUNC-002 quotes).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<ServiceErrorResponse> handleServiceException(ServiceException ex) {
        ServiceErrorResponse body = ex.getErrorResponse();
        return ResponseEntity.status(body.getStatus())
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ServiceErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        ServiceErrorResponse body = ServiceErrorResponse.builder()
                .type("about:blank")
                .title("Validation Failed")
                .status(422)
                .detail("Request validation failed")
                .code(ErrorCodes.VALIDATION_ERROR)
                .retryable(false)
                .errors(ex.getBindingResult().getFieldErrors().stream()
                        .map(fe -> ServiceError.ofField(
                                ErrorCodes.MISSING_REQUIRED_FIELD,
                                fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "invalid",
                                fe.getField()))
                        .toList())
                .build();
        return ResponseEntity.status(422)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(body);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ServiceErrorResponse> handleUnreadable(HttpMessageNotReadableException ex) {
        ServiceErrorResponse body = ServiceErrorResponse.builder()
                .type("about:blank")
                .title("Validation Failed")
                .status(422)
                .detail("Malformed or incomplete request body")
                .code(ErrorCodes.INVALID_REQUEST)
                .retryable(false)
                .build();
        return ResponseEntity.status(422)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(body);
    }
}
