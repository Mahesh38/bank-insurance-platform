package com.bank.platform.consent.api;

import com.bank.common.error.PlatformErrorHandler;
import com.bank.common.error.ServiceError;
import com.bank.common.error.ServiceErrorResponse;
import com.bank.common.error.ServiceErrors;
import com.bank.common.error.ServiceException;
import com.bank.platform.consent.domain.ConsentRefusal;
import com.bank.platform.consent.domain.SensitiveText;
import java.util.List;
import java.util.Map;
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
    var builder =
        errors
            .error(refusal.code())
            .component("Consent")
            .reason(SensitiveText.scrub(refusal.getMessage()));
    if (refusal.attemptsRemaining() != null) {
      builder.errors(
          List.of(
              new ServiceError(
                  refusal.code(),
                  "attempts remaining",
                  "otp",
                  Map.of("attemptsRemaining", refusal.attemptsRemaining()))));
    }
    ServiceException mapped = builder.build();
    return handler.handleServiceException(mapped);
  }
}
