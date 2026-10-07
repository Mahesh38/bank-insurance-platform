package com.bank.workforce.bff.api;

import com.bank.common.error.ErrorCodes;
import com.bank.common.error.ServiceErrors;
import com.bank.workforce.bff.config.WorkforceSessionProperties;
import com.bank.workforce.bff.session.SessionModels.WorkforceSession;
import com.bank.workforce.bff.session.SessionStore;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Opaque BFF session is the only credential Flutter may present ({@code ADR-015}). Lead and
 * reference APIs refuse the call when that handle is missing or expired.
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
    String path = request.getRequestURI();
    if (path.startsWith("/api/v1/auth") || path.startsWith("/actuator")) {
      return true;
    }
    if (!path.startsWith("/api/v1/")) {
      return true;
    }
    String handle = sessionHandle(request);
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

  private String sessionHandle(HttpServletRequest request) {
    String header = request.getHeader("X-Session-Handle");
    if (header != null && !header.isBlank()) {
      return header;
    }
    String authorization = request.getHeader("Authorization");
    if (authorization != null && authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
      return authorization.substring(7).trim();
    }
    Cookie[] cookies = request.getCookies();
    if (cookies == null) {
      return null;
    }
    for (Cookie cookie : cookies) {
      if (properties.cookieName().equals(cookie.getName())
          && cookie.getValue() != null
          && !cookie.getValue().isBlank()) {
        return cookie.getValue();
      }
    }
    return null;
  }
}
