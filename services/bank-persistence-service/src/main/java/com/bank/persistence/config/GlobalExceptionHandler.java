package com.bank.persistence.config;

import com.bank.common.error.ErrorCodes;
import com.bank.common.error.ServiceErrorResponse;
import com.bank.common.error.ServiceException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

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
        ServiceErrorResponse body = ServiceErrorResponse.validation(
                "Request validation failed",
                ex.getBindingResult().getFieldErrors().stream()
                        .map(fe -> com.bank.common.error.ServiceError.ofField(
                                ErrorCodes.MISSING_REQUIRED_FIELD,
                                fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "invalid",
                                fe.getField()))
                        .toList()
        );
        return ResponseEntity.status(body.getStatus())
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(body);
    }
}
