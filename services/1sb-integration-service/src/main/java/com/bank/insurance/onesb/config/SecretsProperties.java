package com.bank.insurance.onesb.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Binds {@code insurance.secrets.*} configuration.
 */
@ConfigurationProperties(prefix = "insurance.secrets")
public record SecretsProperties(
        @DefaultValue("PROPERTIES") SecretSource source
) {
    public enum SecretSource {
        PROPERTIES,
        ENV,
        AWS_SECRETS_MANAGER
    }
}
