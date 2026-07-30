package com.bank.common.secrets;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PropertiesSecretProviderTest {

    @Test
    void resolvesSecretsFromEnvironment() {
        MockEnvironment env = new MockEnvironment()
                .withProperty("onesb.api-key", "test-key")
                .withProperty("onesb.api-secret", "test-secret")
                .withProperty("onesb.distributor-id", "DIST001");

        PropertiesSecretProvider provider = new PropertiesSecretProvider(env);

        assertThat(provider.getApiKey()).isEqualTo("test-key");
        assertThat(provider.getApiSecret()).isEqualTo("test-secret");
        assertThat(provider.getDistributorId()).isEqualTo("DIST001");
    }

    @Test
    void throwsWhenApiKeyMissing() {
        MockEnvironment env = new MockEnvironment();
        PropertiesSecretProvider provider = new PropertiesSecretProvider(env);

        assertThatThrownBy(provider::getApiKey)
                .isInstanceOf(SecretUnavailableException.class)
                .hasMessageContaining("onesb.api-key")
                .hasMessageContaining("PROPERTIES");
    }

    @Test
    void throwsWhenApiSecretMissing() {
        MockEnvironment env = new MockEnvironment();
        PropertiesSecretProvider provider = new PropertiesSecretProvider(env);

        assertThatThrownBy(provider::getApiSecret)
                .isInstanceOf(SecretUnavailableException.class)
                .hasMessageContaining("onesb.api-secret");
    }

    @Test
    void throwsWhenDistributorIdMissing() {
        MockEnvironment env = new MockEnvironment();
        PropertiesSecretProvider provider = new PropertiesSecretProvider(env);

        assertThatThrownBy(provider::getDistributorId)
                .isInstanceOf(SecretUnavailableException.class)
                .hasMessageContaining("onesb.distributor-id");
    }

    @Test
    void throwsWhenApiKeyBlank() {
        MockEnvironment env = new MockEnvironment()
                .withProperty("onesb.api-key", "   ");

        PropertiesSecretProvider provider = new PropertiesSecretProvider(env);

        assertThatThrownBy(provider::getApiKey)
                .isInstanceOf(SecretUnavailableException.class);
    }
}
