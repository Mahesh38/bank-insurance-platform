package com.bank.identity.authz.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
          Set.of("workforce-access-bff"));

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
  void shortKeysAreRejectedAndKeysAreRedacted() {
    assertThatThrownBy(() -> new InternalAuthProperties(Map.of("x", "short"), Set.of()))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(properties.toString()).doesNotContain("key-at-least-16");
  }
}
