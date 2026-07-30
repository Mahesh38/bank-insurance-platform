package com.bank.common.secrets;

import java.util.Objects;
import java.util.function.Function;

/**
 * Resolves credentials from OS environment variables.
 * Used for {@code uat} profile and as a fallback for lightweight deployments.
 * <p>
 * Expected variables:
 * <ul>
 *   <li>{@code ONESB_API_KEY}</li>
 *   <li>{@code ONESB_API_SECRET}</li>
 *   <li>{@code ONESB_DISTRIBUTOR_ID}</li>
 * </ul>
 */
public class EnvSecretProvider implements SecretProvider {

    static final String ENV_API_KEY = "ONESB_API_KEY";
    static final String ENV_API_SECRET = "ONESB_API_SECRET";
    static final String ENV_DISTRIBUTOR_ID = "ONESB_DISTRIBUTOR_ID";

    private final Function<String, String> envLookup;

    public EnvSecretProvider() {
        this(System::getenv);
    }

    /**
     * Package-visible for unit tests — inject a fake env map without mutating the JVM process env.
     */
    EnvSecretProvider(Function<String, String> envLookup) {
        this.envLookup = Objects.requireNonNull(envLookup, "envLookup");
    }

    @Override
    public String getApiKey() {
        return resolve(ENV_API_KEY);
    }

    @Override
    public String getApiSecret() {
        return resolve(ENV_API_SECRET);
    }

    @Override
    public String getDistributorId() {
        return resolve(ENV_DISTRIBUTOR_ID);
    }

    private String resolve(String envVar) {
        String value = envLookup.apply(envVar);
        if (value == null || value.isBlank()) {
            throw new SecretUnavailableException(envVar, "ENV");
        }
        return value;
    }
}
