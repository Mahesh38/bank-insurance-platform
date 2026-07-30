package com.bank.insurance.onesb.adapter.secret;

/**
 * Contract for resolving 1SB credentials at runtime.
 * Implementations must never return null; they throw if a credential is unavailable.
 */
public interface SecretProvider {

    /**
     * Returns the 1SB API key.
     *
     * @throws SecretUnavailableException if the secret cannot be resolved
     */
    String getApiKey();

    /**
     * Returns the 1SB API secret.
     *
     * @throws SecretUnavailableException if the secret cannot be resolved
     */
    String getApiSecret();

    /**
     * Returns the distributor ID registered with 1SB.
     *
     * @throws SecretUnavailableException if the secret cannot be resolved
     */
    String getDistributorId();
}
