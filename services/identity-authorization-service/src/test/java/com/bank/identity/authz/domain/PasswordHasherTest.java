package com.bank.identity.authz.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PasswordHasherTest {

  private final PasswordHasher hasher = new PasswordHasher();

  @Test
  void hashMatchesOnlyTheSamePassword() {
    String stored = hasher.hash("Horse#99");
    assertThat(stored).startsWith("pbkdf2$");
    assertThat(stored).doesNotContain("Horse#99");
    assertThat(hasher.matches("Horse#99", stored)).isTrue();
    assertThat(hasher.matches("Horse#98", stored)).isFalse();
    assertThat(hasher.matches("Horse#99", null)).isFalse();
    assertThat(hasher.matches(null, stored)).isFalse();
  }
}
