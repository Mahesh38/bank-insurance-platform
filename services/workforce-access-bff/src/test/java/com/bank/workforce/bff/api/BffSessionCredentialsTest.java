package com.bank.workforce.bff.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

@Tag("unit")
@Tag("FUNC-031")
class BffSessionCredentialsTest {

  @Test
  void csrfSkipRequiresBearerOrSessionHandle() {
    MockHttpServletRequest bearer = new MockHttpServletRequest();
    bearer.addHeader("Authorization", "Bearer opaque-handle");
    assertThat(BffSessionCredentials.skipsCsrf(bearer)).isTrue();

    MockHttpServletRequest handle = new MockHttpServletRequest();
    handle.addHeader("X-Session-Handle", "opaque-handle");
    assertThat(BffSessionCredentials.skipsCsrf(handle)).isTrue();

    MockHttpServletRequest basic = new MockHttpServletRequest();
    basic.addHeader("Authorization", "Basic dXNlcjpwYXNz");
    assertThat(BffSessionCredentials.skipsCsrf(basic)).isFalse();

    MockHttpServletRequest empty = new MockHttpServletRequest();
    assertThat(BffSessionCredentials.skipsCsrf(empty)).isFalse();
  }
}
