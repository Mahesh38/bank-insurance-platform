package com.bank.identity.provider.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("IAM-001")
@Tag("unit")
class InternalAuthPropertiesTest {

  private static final String SECRET = "local-idp-adapter-internal-key";

  @Test
  void matchesEqualSecretAndRejectsMissingOrDifferent() {
    InternalAuthProperties properties = new InternalAuthProperties(SECRET);
    assertThat(properties.matches(SECRET)).isTrue();
    assertThat(properties.matches("wrong-wrong-wrong-x")).isFalse();
    assertThat(properties.matches("short")).isFalse();
    assertThat(properties.matches(null)).isFalse();
    assertThat(properties.matches("")).isFalse();
    assertThat(properties.matches("   ")).isFalse();
  }

  @Test
  void toStringDoesNotContainTheSecret() {
    assertThat(new InternalAuthProperties(SECRET).toString())
        .isEqualTo("InternalAuthProperties[sharedSecret=redacted]")
        .doesNotContain(SECRET);
  }
}
