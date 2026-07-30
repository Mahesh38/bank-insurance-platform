package com.bank.insurance.onesb.adapter.secret;

/**
 * Stub AWS Secrets Manager provider.
 * <p>
 * Phase 1 placeholder — real AWS SDK integration is deferred to Phase 2.
 * Configuring {@code insurance.secrets.source=AWS_SECRETS_MANAGER} in
 * {@code prod} profile without completing the Phase 2 wiring will cause
 * startup to fail with a clear message.
 * <p>
 * <strong>Do not use in production until TECH-002 Phase 2 is implemented.</strong>
 */
public class AwsSecretsManagerSecretProvider implements SecretProvider {

    private static final String NOT_IMPLEMENTED_MSG =
            "AWS Secrets Manager provider is not yet implemented (Phase 2). "
                    + "Use PROPERTIES or ENV source for Phase 1. "
                    + "Set insurance.secrets.source=PROPERTIES or ENV.";

    @Override
    public String getApiKey() {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_MSG);
    }

    @Override
    public String getApiSecret() {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_MSG);
    }

    @Override
    public String getDistributorId() {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED_MSG);
    }
}
