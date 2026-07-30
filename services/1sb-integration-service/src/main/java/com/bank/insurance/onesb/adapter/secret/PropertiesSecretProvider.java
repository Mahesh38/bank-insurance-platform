package com.bank.insurance.onesb.adapter.secret;

import org.springframework.core.env.Environment;

/**
 * Resolves 1SB credentials from Spring {@link Environment} properties.
 * Used for {@code local} and {@code test} profiles only.
 * <p>
 * Keys are configured in {@code application-local.properties} (gitignored).
 * See {@code application-local.properties.example} for the required property names.
 */
public class PropertiesSecretProvider implements SecretProvider {

    static final String KEY_API_KEY = "onesb.api-key";
    static final String KEY_API_SECRET = "onesb.api-secret";
    static final String KEY_DISTRIBUTOR_ID = "onesb.distributor-id";

    private final Environment environment;

    public PropertiesSecretProvider(Environment environment) {
        this.environment = environment;
    }

    @Override
    public String getApiKey() {
        return resolve(KEY_API_KEY);
    }

    @Override
    public String getApiSecret() {
        return resolve(KEY_API_SECRET);
    }

    @Override
    public String getDistributorId() {
        return resolve(KEY_DISTRIBUTOR_ID);
    }

    private String resolve(String key) {
        String value = environment.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new SecretUnavailableException(key, "PROPERTIES");
        }
        return value;
    }
}
