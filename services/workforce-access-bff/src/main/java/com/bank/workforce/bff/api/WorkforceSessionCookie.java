package com.bank.workforce.bff.api;

import com.bank.workforce.bff.config.WorkforceSessionProperties;
import java.time.Duration;
import org.springframework.http.ResponseCookie;

/** {@code WORKFORCE_SESSION} attributes shared by login and logout. */
public final class WorkforceSessionCookie {

  private WorkforceSessionCookie() {}

  public static ResponseCookie issue(WorkforceSessionProperties properties, String value) {
    return cookie(properties, value, properties.sessionTtl());
  }

  public static ResponseCookie clear(WorkforceSessionProperties properties) {
    return cookie(properties, "", Duration.ZERO);
  }

  private static ResponseCookie cookie(
      WorkforceSessionProperties properties, String value, Duration maxAge) {
    return ResponseCookie.from(properties.cookieName(), value)
        .httpOnly(true)
        .secure(properties.cookieSecure())
        .sameSite("Strict")
        .path("/")
        .maxAge(maxAge)
        .build();
  }
}
