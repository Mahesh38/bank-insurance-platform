package com.bank.workforce.bff.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.workforce.bff.config.WorkforceSessionProperties;
import jakarta.servlet.http.Cookie;
import java.time.Duration;
import java.util.Set;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

@Tag("unit")
@Tag("FUNC-031")
class BffSessionCredentialsTest {

  private final WorkforceSessionProperties properties =
      new WorkforceSessionProperties(
          "memory",
          "WORKFORCE_SESSION",
          "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
          Duration.ofMinutes(10),
          Duration.ofMinutes(5),
          Duration.ofHours(8),
          Set.of("https://bank.example/"),
          true);

  @Test
  void csrfSkipRequiresBearerOrSessionHandleWithoutCookie() {
    MockHttpServletRequest bearer = new MockHttpServletRequest();
    bearer.addHeader("Authorization", "Bearer opaque-handle");
    assertThat(BffSessionCredentials.skipsCsrf(bearer, properties)).isTrue();

    MockHttpServletRequest handle = new MockHttpServletRequest();
    handle.addHeader("X-Session-Handle", "opaque-handle");
    assertThat(BffSessionCredentials.skipsCsrf(handle, properties)).isTrue();

    MockHttpServletRequest basic = new MockHttpServletRequest();
    basic.addHeader("Authorization", "Basic dXNlcjpwYXNz");
    assertThat(BffSessionCredentials.skipsCsrf(basic, properties)).isFalse();

    MockHttpServletRequest empty = new MockHttpServletRequest();
    assertThat(BffSessionCredentials.skipsCsrf(empty, properties)).isFalse();
  }

  @Test
  void sessionCookieForcesCsrfEvenWithBearer() {
    MockHttpServletRequest both = new MockHttpServletRequest();
    both.addHeader("Authorization", "Bearer opaque-handle");
    both.setCookies(new Cookie("WORKFORCE_SESSION", "cookie-handle"));
    assertThat(BffSessionCredentials.skipsCsrf(both, properties)).isFalse();
  }
}
