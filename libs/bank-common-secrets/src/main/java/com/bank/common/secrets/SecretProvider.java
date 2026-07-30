package com.bank.common.secrets;

/**
 * Contract for resolving credentials at runtime.
 * Implementations must never return null; they throw if a credential is unavailable.
 */
public interface SecretProvider {

    /**
     * Returns the API key.
     *
     * @throws SecretUnavailableException if the secret cannot be resolved
     */
    String getApiKey();

    /**
     * Returns the API secret.
     *
     * @throws SecretUnavailableException if the secret cannot be resolved
     */
    String getApiSecret();

    /**
     * Returns the distributor ID.
     *
     * @throws SecretUnavailableException if the secret cannot be resolved
     */
    String getDistributorId();
}
