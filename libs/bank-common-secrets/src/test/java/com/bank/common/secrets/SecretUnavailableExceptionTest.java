package com.bank.common.secrets;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SecretUnavailableExceptionTest {

  @Test
  void messageIncludesSecretNameAndSource() {
    SecretUnavailableException ex = new SecretUnavailableException("onesb.api-key", "PROPERTIES");

    assertThat(ex.getMessage()).contains("onesb.api-key").contains("PROPERTIES");
    assertThat(ex.getCause()).isNull();
  }

  @Test
  void messageIncludesCauseWhenProvided() {
    RuntimeException cause = new RuntimeException("timeout");
    SecretUnavailableException ex =
        new SecretUnavailableException("ONESB_API_KEY", "AWS_SECRETS_MANAGER", cause);

    assertThat(ex.getMessage())
        .contains("ONESB_API_KEY")
        .contains("AWS_SECRETS_MANAGER")
        .contains("timeout");
    assertThat(ex.getCause()).isSameAs(cause);
  }
}
