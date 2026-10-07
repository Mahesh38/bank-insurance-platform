package com.bank.workforce.bff.api;

import com.bank.common.error.ErrorCodes;
import com.bank.common.error.ServiceErrors;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;

@Component
public class BffSessionAuthenticationEntryPoint implements AuthenticationEntryPoint {

  private final ServiceErrors errors;
  private final HandlerExceptionResolver resolver;

  public BffSessionAuthenticationEntryPoint(
      ServiceErrors errors,
      @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) {
    this.errors = errors;
    this.resolver = resolver;
  }

  @Override
  public void commence(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException authException) {
    resolver.resolveException(
        request,
        response,
        null,
        errors
            .error(ErrorCodes.SESSION_INVALID)
            .component("BffSessionAuthenticationEntryPoint")
            .operation("commence")
            .reason("session handle is required")
            .cause(authException)
            .build());
  }
}
