package com.bank.workforce.bff.api;

import com.bank.common.error.ErrorCodes;
import com.bank.common.error.ServiceErrors;
import com.bank.workforce.bff.config.WorkforceSessionProperties;
import com.bank.workforce.bff.session.SessionModels.WorkforceSession;
import com.bank.workforce.bff.session.SessionStore;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * Binds the opaque BFF session into Spring Security so {@code /api/v1/**} is default-deny (SEC-C4)
 * rather than {@code permitAll} plus an MVC interceptor.
 */
@Component
public class BffSessionAuthenticationFilter extends OncePerRequestFilter {

  private final SessionStore sessions;
  private final WorkforceSessionProperties properties;
  private final ServiceErrors errors;
  private final HandlerExceptionResolver resolver;

  public BffSessionAuthenticationFilter(
      SessionStore sessions,
      WorkforceSessionProperties properties,
      ServiceErrors errors,
      @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) {
    this.sessions = sessions;
    this.properties = properties;
    this.errors = errors;
    this.resolver = resolver;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getRequestURI();
    return path.startsWith("/api/v1/auth")
        || path.startsWith("/actuator")
        || path.startsWith("/error");
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String path = request.getRequestURI();
    if (!path.startsWith("/api/v1/")) {
      filterChain.doFilter(request, response);
      return;
    }
    String handle = BffSessionCredentials.handle(request, properties);
    if (handle == null || handle.isBlank()) {
      filterChain.doFilter(request, response);
      return;
    }
    WorkforceSession session = sessions.getSession(handle).orElse(null);
    if (session == null) {
      resolver.resolveException(
          request,
          response,
          null,
          errors
              .error(ErrorCodes.SESSION_EXPIRED)
              .component("BffSessionAuthenticationFilter")
              .operation("doFilterInternal")
              .reason("session is not active")
              .build());
      return;
    }
    request.setAttribute(BffSessionInterceptor.SESSION_ATTR, session);
    UsernamePasswordAuthenticationToken authentication =
        UsernamePasswordAuthenticationToken.authenticated(
            session.businessUserId().toString(), handle, List.of());
    SecurityContextHolder.getContext().setAuthentication(authentication);
    try {
      filterChain.doFilter(request, response);
    } finally {
      SecurityContextHolder.clearContext();
    }
  }
}
