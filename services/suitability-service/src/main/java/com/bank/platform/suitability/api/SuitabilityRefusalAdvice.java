package com.bank.platform.suitability.api;

import com.bank.common.error.PlatformErrorHandler;
import com.bank.common.error.ServiceError;
import com.bank.common.error.ServiceErrorResponse;
import com.bank.common.error.ServiceErrors;
import com.bank.platform.suitability.domain.SuitabilityRefusal;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Maps a journey refusal onto the platform envelope. The BRD sentence stays in errors[]. */
@RestControllerAdvice
public class SuitabilityRefusalAdvice {

  private final PlatformErrorHandler handler;
  private final ServiceErrors errors;

  public SuitabilityRefusalAdvice(PlatformErrorHandler handler, ServiceErrors errors) {
    this.handler = handler;
    this.errors = errors;
  }

  @ExceptionHandler(SuitabilityRefusal.class)
  public ResponseEntity<ServiceErrorResponse> handle(SuitabilityRefusal refusal) {
    var builder =
        errors.error(refusal.code()).component("Suitability").reason(refusal.getMessage());
    String id = refusal.valId() == null ? refusal.code() : refusal.valId();
    builder.errors(List.of(ServiceError.ofField(id, refusal.getMessage(), refusal.field())));
    return handler.handleServiceException(builder.build());
  }
}
