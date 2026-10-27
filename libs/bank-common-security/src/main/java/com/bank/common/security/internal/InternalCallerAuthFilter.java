package com.bank.common.security.internal;

import com.bank.common.error.ErrorCodes;
import com.bank.common.error.ServiceErrors;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * Authenticates the calling service before a protected path prefix runs. The authenticated caller
 * id is bound as {@link InternalCallerAuth#CALLER_ATTRIBUTE}.
 */
public class InternalCallerAuthFilter extends OncePerRequestFilter {

  private final String pathPrefix;
  private final InternalCallerCredentials credentials;
  private final ServiceErrors errors;
  private final HandlerExceptionResolver resolver;

  public InternalCallerAuthFilter(
      String pathPrefix,
      InternalCallerCredentials credentials,
      ServiceErrors errors,
      @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) {
    this.pathPrefix = Objects.requireNonNull(pathPrefix, "pathPrefix");
    this.credentials = Objects.requireNonNull(credentials, "credentials");
    this.errors = Objects.requireNonNull(errors, "errors");
    this.resolver = Objects.requireNonNull(resolver, "resolver");
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String uri = request.getRequestURI();
    return uri == null || !uri.startsWith(pathPrefix);
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    Optional<String> caller =
        credentials.authenticate(request.getHeader(InternalCallerAuth.HEADER));
    if (caller.isEmpty()) {
      resolver.resolveException(
          request,
          response,
          null,
          errors
              .error(ErrorCodes.UNAUTHORIZED)
              .component("InternalCallerAuthFilter")
              .operation("doFilterInternal")
              .reason("internal caller is not authenticated")
              .build());
      return;
    }
    request.setAttribute(InternalCallerAuth.CALLER_ATTRIBUTE, caller.get());
    filterChain.doFilter(request, response);
  }
}
