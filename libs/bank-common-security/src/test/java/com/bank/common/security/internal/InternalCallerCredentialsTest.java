package com.bank.common.security.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class InternalCallerCredentialsTest {

  @Test
  void authenticate_matchesCallerInConstantTimeAcrossKeys() {
    InternalCallerCredentials credentials =
        new InternalCallerCredentials(
            Map.of("bff", "local-authz-bff-internal-key", "lead", "local-authz-lead-internal-key"),
            Set.of("bff"),
            Map.of(
                "bff",
                Set.of(InternalCallerAuth.ALL_PRIVILEGES),
                "lead",
                Set.of(InternalCallerAuth.PRIVILEGE_AUTHORIZATION_DECIDE)));

    assertThat(credentials.authenticate("local-authz-lead-internal-key")).contains("lead");
    assertThat(credentials.authenticate("wrong-key-that-is-long")).isEmpty();
    assertThat(credentials.mayAssertActor("bff")).isTrue();
    assertThat(credentials.mayAssertActor("lead")).isFalse();
    assertThat(credentials.hasPrivilege("lead", InternalCallerAuth.PRIVILEGE_AUTHORIZATION_DECIDE))
        .isTrue();
    assertThat(credentials.hasPrivilege("lead", InternalCallerAuth.PRIVILEGE_LOGIN_ORCHESTRATE))
        .isFalse();
    assertThat(credentials.hasPrivilege("bff", InternalCallerAuth.PRIVILEGE_LOGIN_ORCHESTRATE))
        .isTrue();
  }

  @Test
  void shortKeysAreRejected() {
    assertThatThrownBy(
            () -> new InternalCallerCredentials(Map.of("bff", "too-short"), Set.of(), Map.of()))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
