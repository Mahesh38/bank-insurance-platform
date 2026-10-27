package com.bank.common.security.internal;

import com.bank.common.error.ServiceErrors;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.HandlerExceptionResolver;

/** Registers {@link InternalCallerAuthFilter} for a path prefix. */
public final class InternalCallerAuthSupport {

  private InternalCallerAuthSupport() {}

  public static FilterRegistrationBean<InternalCallerAuthFilter> registration(
      String pathPattern,
      String pathPrefix,
      InternalCallerCredentials credentials,
      ServiceErrors errors,
      HandlerExceptionResolver resolver) {
    FilterRegistrationBean<InternalCallerAuthFilter> registration =
        new FilterRegistrationBean<>(
            new InternalCallerAuthFilter(pathPrefix, credentials, errors, resolver));
    registration.addUrlPatterns(pathPattern);
    registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
    registration.setName("internalCallerAuthFilter");
    return registration;
  }
}
