package com.bank.platform.consent.api;

import com.bank.common.error.PlatformErrorHandler;
import com.bank.common.error.ServiceErrorResponse;
import com.bank.common.error.ServiceErrors;
import com.bank.common.error.ServiceException;
import com.bank.platform.consent.domain.ConsentRefusal;
import com.bank.platform.consent.domain.SensitiveText;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Maps a domain refusal onto the platform error envelope. The reason is scrubbed first. */
@RestControllerAdvice
public class ConsentRefusalAdvice {

  private final PlatformErrorHandler handler;
  private final ServiceErrors errors;

  public ConsentRefusalAdvice(PlatformErrorHandler handler, ServiceErrors errors) {
    this.handler = handler;
    this.errors = errors;
  }

  @ExceptionHandler(ConsentRefusal.class)
  public ResponseEntity<ServiceErrorResponse> handle(ConsentRefusal refusal) {
    ServiceException mapped =
        errors
            .error(refusal.code())
            .component("Consent")
            .reason(SensitiveText.scrub(refusal.getMessage()))
            .build();
    return handler.handleServiceException(mapped);
  }
}
