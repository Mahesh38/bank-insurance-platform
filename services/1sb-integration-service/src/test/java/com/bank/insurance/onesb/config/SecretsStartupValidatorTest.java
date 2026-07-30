package com.bank.insurance.onesb.config;

import com.bank.common.secrets.SecretProvider;
import com.bank.common.secrets.SecretUnavailableException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("unit")
@Tag("QA-006")
class SecretsStartupValidatorTest {

    private final SecretProvider secretProvider = mock(SecretProvider.class);
    private final Environment environment = mock(Environment.class);

    @Test
    void run_testProfile_skipsSecretLookup() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"test"});

        SecretsStartupValidator validator = new SecretsStartupValidator(secretProvider, environment);

        assertThatCode(() -> validator.run(new DefaultApplicationArguments()))
                .doesNotThrowAnyException();

        verify(secretProvider, never()).getApiKey();
        verify(secretProvider, never()).getApiSecret();
        verify(secretProvider, never()).getDistributorId();
    }

    @Test
    void run_integrationTestProfile_skipsSecretLookup() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"integration-test"});

        SecretsStartupValidator validator = new SecretsStartupValidator(secretProvider, environment);

        assertThatCode(() -> validator.run(new DefaultApplicationArguments()))
                .doesNotThrowAnyException();

        verify(secretProvider, never()).getApiKey();
    }

    @Test
    void run_nonTestProfile_missingSecret_failsFast() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"default"});
        when(secretProvider.getApiKey()).thenThrow(new SecretUnavailableException("api-key", "ENV"));

        SecretsStartupValidator validator = new SecretsStartupValidator(secretProvider, environment);

        assertThatThrownBy(() -> validator.run(new DefaultApplicationArguments()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("api-key")
                .hasMessageContaining("unavailable")
                .hasCauseInstanceOf(SecretUnavailableException.class);
    }

    @Test
    void run_nonTestProfile_blankSecret_failsFast() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        when(secretProvider.getApiKey()).thenReturn("key");
        when(secretProvider.getApiSecret()).thenReturn("   ");

        SecretsStartupValidator validator = new SecretsStartupValidator(secretProvider, environment);

        assertThatThrownBy(() -> validator.run(new DefaultApplicationArguments()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("api-secret")
                .hasMessageContaining("blank");
    }

    @Test
    void run_nonTestProfile_allSecretsPresent_completes() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"default"});
        when(secretProvider.getApiKey()).thenReturn("key");
        when(secretProvider.getApiSecret()).thenReturn("secret");
        when(secretProvider.getDistributorId()).thenReturn("DIST-1");

        SecretsStartupValidator validator = new SecretsStartupValidator(secretProvider, environment);

        assertThatCode(() -> validator.run(new DefaultApplicationArguments()))
                .doesNotThrowAnyException();

        verify(secretProvider).getApiKey();
        verify(secretProvider).getApiSecret();
        verify(secretProvider).getDistributorId();
    }
}
