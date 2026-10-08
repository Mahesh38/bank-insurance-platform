package com.bank.workforce.bff.api;

import com.bank.workforce.bff.config.WorkforceSessionProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

/** Opaque BFF session handle from header, bearer token, or cookie ({@code ADR-015}). */
public final class BffSessionCredentials {

  private BffSessionCredentials() {}

  public static String handle(HttpServletRequest request, WorkforceSessionProperties properties) {
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

  /**
   * Cookie-only browser calls keep CSRF. Header or Bearer session transport is not cookie CSRF
   * (SEC-C1) — {@code Authorization: Basic} does not skip.
   */
  public static boolean skipsCsrf(HttpServletRequest request) {
    String header = request.getHeader("X-Session-Handle");
    if (header != null && !header.isBlank()) {
      return true;
    }
    String authorization = request.getHeader("Authorization");
    return authorization != null && authorization.regionMatches(true, 0, "Bearer ", 0, 7);
  }
}
