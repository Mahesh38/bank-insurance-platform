package com.bank.workforce.bff.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.workforce.bff.config.WorkforceSessionProperties;
import java.time.Duration;
import java.util.Set;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;

@Tag("unit")
class WorkforceSessionCookieTest {

  @Test
  void secureCookieIsTheDefaultShape() {
    ResponseCookie cookie = WorkforceSessionCookie.issue(properties(true), "handle");

    assertThat(cookie.isHttpOnly()).isTrue();
    assertThat(cookie.isSecure()).isTrue();
    assertThat(cookie.getSameSite()).isEqualTo("Strict");
    assertThat(cookie.getPath()).isEqualTo("/");
    assertThat(cookie.toString()).contains("Secure");
  }

  @Test
  void devProfileOmitsSecureAndKeepsTheOtherAttributes() {
    ResponseCookie issued = WorkforceSessionCookie.issue(properties(false), "handle");
    ResponseCookie cleared = WorkforceSessionCookie.clear(properties(false));

    assertThat(issued.isSecure()).isFalse();
    assertThat(issued.isHttpOnly()).isTrue();
    assertThat(issued.getSameSite()).isEqualTo("Strict");
    assertThat(issued.toString()).doesNotContain("Secure");
    assertThat(cleared.isSecure()).isFalse();
    assertThat(cleared.getMaxAge()).isEqualTo(Duration.ZERO);
  }

  private static WorkforceSessionProperties properties(boolean cookieSecure) {
    return new WorkforceSessionProperties(
        "memory",
        "WORKFORCE_SESSION",
        "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
        Duration.ofMinutes(5),
        Duration.ofMinutes(1),
        Duration.ofHours(8),
        Set.of("http://localhost:3000/auth/complete"),
        cookieSecure);
  }
}
