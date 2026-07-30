package com.bank.insurance.onesb.config;

import com.bank.common.secrets.AwsSecretsManagerSecretProvider;
import com.bank.common.secrets.EnvSecretProvider;
import com.bank.common.secrets.PropertiesSecretProvider;
import com.bank.common.secrets.SecretProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * Creates the appropriate {@link SecretProvider} bean based on
 * {@code insurance.secrets.source} configuration.
 */
@Configuration
public class SecretProviderConfig {

    @Bean
    public SecretProvider secretProvider(SecretsProperties properties, Environment environment) {
        return switch (properties.source()) {
            case PROPERTIES -> new PropertiesSecretProvider(environment);
            case ENV -> new EnvSecretProvider();
            case AWS_SECRETS_MANAGER -> new AwsSecretsManagerSecretProvider();
        };
    }
}
