package com.bank.common.secrets;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class AwsSecretsManagerSecretProviderTest {

  private final AwsSecretsManagerSecretProvider provider = new AwsSecretsManagerSecretProvider();

  @Test
  void getApiKeyThrowsUnsupportedOperation() {
    assertThatThrownBy(provider::getApiKey)
        .isInstanceOf(UnsupportedOperationException.class)
        .hasMessageContaining("Phase 2");
  }

  @Test
  void getApiSecretThrowsUnsupportedOperation() {
    assertThatThrownBy(provider::getApiSecret).isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void getDistributorIdThrowsUnsupportedOperation() {
    assertThatThrownBy(provider::getDistributorId)
        .isInstanceOf(UnsupportedOperationException.class);
  }
}
