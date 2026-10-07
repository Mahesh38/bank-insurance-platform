package com.bank.workforce.bff.api;

import com.bank.common.error.ErrorCodes;
import com.bank.common.error.ServiceErrors;
import com.bank.workforce.bff.config.WorkforceSessionProperties;
import com.bank.workforce.bff.session.SessionModels.WorkforceSession;
import com.bank.workforce.bff.session.SessionStore;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Backup session bind for MVC. Spring Security default-deny ({@code BffSessionAuthenticationFilter})
 * is the primary control (SEC-C4).
 */
@Component
public class BffSessionInterceptor implements HandlerInterceptor {

  public static final String SESSION_ATTR = "bff.session";

  private final SessionStore sessions;
  private final WorkforceSessionProperties properties;
  private final ServiceErrors errors;

  public BffSessionInterceptor(
      SessionStore sessions, WorkforceSessionProperties properties, ServiceErrors errors) {
    this.sessions = sessions;
    this.properties = properties;
    this.errors = errors;
  }

  @Override
  public boolean preHandle(
      HttpServletRequest request, HttpServletResponse response, Object handler) {
    if (request.getAttribute(SESSION_ATTR) instanceof WorkforceSession) {
      return true;
    }
    String path = request.getRequestURI();
    if (path.startsWith("/api/v1/auth") || path.startsWith("/actuator")) {
      return true;
    }
    if (!path.startsWith("/api/v1/")) {
      return true;
    }
    String handle = BffSessionCredentials.handle(request, properties);
    if (handle == null) {
      throw errors
          .error(ErrorCodes.SESSION_INVALID)
          .component("BffSessionInterceptor")
          .operation("preHandle")
          .reason("session handle is required")
          .build();
    }
    WorkforceSession session =
        sessions
            .getSession(handle)
            .orElseThrow(
                () ->
                    errors
                        .error(ErrorCodes.SESSION_EXPIRED)
                        .component("BffSessionInterceptor")
                        .operation("preHandle")
                        .reason("session is not active")
                        .build());
    request.setAttribute(SESSION_ATTR, session);
    return true;
  }

  public static WorkforceSession requireSession(HttpServletRequest request) {
    Object value = request.getAttribute(SESSION_ATTR);
    if (value instanceof WorkforceSession session) {
      return session;
    }
    throw new IllegalStateException("BFF session was not bound");
  }
}
