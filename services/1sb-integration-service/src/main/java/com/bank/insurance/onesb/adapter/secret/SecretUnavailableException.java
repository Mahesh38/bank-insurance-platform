package com.bank.insurance.onesb.adapter.secret;

/**
 * Thrown when a required secret cannot be resolved from the configured source.
 */
public class SecretUnavailableException extends RuntimeException {

    public SecretUnavailableException(String secretName, String source) {
        super(String.format(
                "Required secret '%s' is not available from source '%s'. "
                        + "Configure the secret before starting the application.",
                secretName, source));
    }

    public SecretUnavailableException(String secretName, String source, Throwable cause) {
        super(String.format(
                "Failed to retrieve secret '%s' from source '%s': %s",
                secretName, source, cause.getMessage()), cause);
    }
}
