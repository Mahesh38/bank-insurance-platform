package com.bank.common.secrets;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EnvSecretProviderTest {

    @Test
    void resolvesSecretsFromEnvLookup() {
        Map<String, String> env = Map.of(
                EnvSecretProvider.ENV_API_KEY, "env-key",
                EnvSecretProvider.ENV_API_SECRET, "env-secret",
                EnvSecretProvider.ENV_DISTRIBUTOR_ID, "DIST_ENV"
        );
        EnvSecretProvider provider = new EnvSecretProvider(env::get);

        assertThat(provider.getApiKey()).isEqualTo("env-key");
        assertThat(provider.getApiSecret()).isEqualTo("env-secret");
        assertThat(provider.getDistributorId()).isEqualTo("DIST_ENV");
    }

    @Test
    void throwsWhenApiKeyMissing() {
        EnvSecretProvider provider = new EnvSecretProvider(key -> null);

        assertThatThrownBy(provider::getApiKey)
                .isInstanceOf(SecretUnavailableException.class)
                .hasMessageContaining(EnvSecretProvider.ENV_API_KEY)
                .hasMessageContaining("ENV");
    }

    @Test
    void throwsWhenApiSecretBlank() {
        EnvSecretProvider provider = new EnvSecretProvider(key ->
                EnvSecretProvider.ENV_API_SECRET.equals(key) ? "  " : null);

        assertThatThrownBy(provider::getApiSecret)
                .isInstanceOf(SecretUnavailableException.class)
                .hasMessageContaining(EnvSecretProvider.ENV_API_SECRET);
    }

    @Test
    void throwsWhenDistributorIdMissing() {
        EnvSecretProvider provider = new EnvSecretProvider(key -> null);

        assertThatThrownBy(provider::getDistributorId)
                .isInstanceOf(SecretUnavailableException.class)
                .hasMessageContaining(EnvSecretProvider.ENV_DISTRIBUTOR_ID);
    }

    @Test
    void defaultConstructor_usesSystemGetenv() {
        // Smoke: constructing the production path does not throw; missing vars throw on resolve.
        EnvSecretProvider provider = new EnvSecretProvider();
        assertThat(provider).isNotNull();
    }
}
