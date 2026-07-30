package com.bank.insurance.onesb.config;

import com.bank.common.secrets.SecretProvider;
import com.bank.common.secrets.SecretUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Fail-fast startup validator for 1SB credentials.
 * <p>
 * In non-test profiles, the service refuses to start if any required credential
 * (api-key, api-secret, distributor-id) is missing. This prevents silent misconfiguration
 * from causing runtime failures during the first real request.
 * <p>
 * Test profiles (test, integration-test) skip validation to allow unit/integration tests
 * to run without real credentials.
 */
@Component
@Order(1)
public class SecretsStartupValidator implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SecretsStartupValidator.class);

    private final SecretProvider secretProvider;
    private final Environment environment;

    public SecretsStartupValidator(SecretProvider secretProvider, Environment environment) {
        this.secretProvider = secretProvider;
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        String[] activeProfiles = environment.getActiveProfiles();
        boolean isTestProfile = Arrays.stream(activeProfiles)
                .anyMatch(p -> p.equals("test") || p.equals("integration-test"));

        if (isTestProfile) {
            log.info("Skipping secrets validation in test profile");
            return;
        }

        log.info("Validating 1SB credentials at startup (profiles: {})...",
                Arrays.toString(activeProfiles));

        validateSecret("api-key", secretProvider::getApiKey);
        validateSecret("api-secret", secretProvider::getApiSecret);
        validateSecret("distributor-id", secretProvider::getDistributorId);

        log.info("1SB credentials validated successfully.");
    }

    private void validateSecret(String name, SecretSupplier supplier) {
        try {
            String value = supplier.get();
            if (value == null || value.isBlank()) {
                throw new IllegalStateException(
                        "Required 1SB credential '" + name + "' resolved to blank. "
                                + "Check insurance.secrets.source and the corresponding secret store.");
            }
        } catch (SecretUnavailableException e) {
            log.error("STARTUP FAILED: Missing required credential '{}': {}", name, e.getMessage());
            throw new IllegalStateException(
                    "Service startup aborted: required 1SB credential '" + name + "' is unavailable. "
                            + e.getMessage(), e);
        }
    }

    @FunctionalInterface
    private interface SecretSupplier {
        String get();
    }
}
