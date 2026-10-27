package com.bank.identity.authz.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bank.common.security.internal.InternalCallerAuth;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class InternalAuthPropertiesTest {

  private final InternalAuthProperties properties =
      new InternalAuthProperties(
          Map.of(
              "workforce-access-bff", "bff-key-at-least-16",
              "lead-service", "lead-key-at-least-16"),
          Set.of("workforce-access-bff"),
          Map.of(
              "workforce-access-bff",
              Set.of(InternalCallerAuth.ALL_PRIVILEGES),
              "lead-service",
              Set.of(
                  InternalCallerAuth.PRIVILEGE_AUTHORIZATION_DECIDE,
                  InternalCallerAuth.PRIVILEGE_IDENTITIES_PROFILE)));

  @Test
  void identifiesTheCallerByItsKey() {
    assertThat(properties.authenticate("bff-key-at-least-16")).contains("workforce-access-bff");
    assertThat(properties.authenticate("lead-key-at-least-16")).contains("lead-service");
    assertThat(properties.authenticate("unknown-key-xxxxxxxx")).isEmpty();
    assertThat(properties.authenticate(null)).isEmpty();
    assertThat(properties.authenticate(" ")).isEmpty();
  }

  @Test
  void onlyActorCallersMayAssertAnActor() {
    assertThat(properties.mayAssertActor("workforce-access-bff")).isTrue();
    assertThat(properties.mayAssertActor("lead-service")).isFalse();
    assertThat(properties.mayAssertActor(null)).isFalse();
  }

  @Test
  void leadHasDecideAndProfileButNotLoginOrchestrate() {
    assertThat(
            properties.hasPrivilege(
                "lead-service", InternalCallerAuth.PRIVILEGE_AUTHORIZATION_DECIDE))
        .isTrue();
    assertThat(
            properties.hasPrivilege(
                "lead-service", InternalCallerAuth.PRIVILEGE_IDENTITIES_PROFILE))
        .isTrue();
    assertThat(
            properties.hasPrivilege("lead-service", InternalCallerAuth.PRIVILEGE_LOGIN_ORCHESTRATE))
        .isFalse();
    assertThat(
            properties.hasPrivilege(
                "workforce-access-bff", InternalCallerAuth.PRIVILEGE_LOGIN_ORCHESTRATE))
        .isTrue();
  }

  @Test
  void shortKeysAreRejectedAndKeysAreRedacted() {
    InternalAuthProperties shortKey =
        new InternalAuthProperties(Map.of("x", "short"), Set.of(), Map.of());
    assertThatThrownBy(shortKey::credentials).isInstanceOf(IllegalArgumentException.class);
    assertThat(properties.toString()).doesNotContain("key-at-least-16");
  }
}
