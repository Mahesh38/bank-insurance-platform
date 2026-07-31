package com.bank.persistence.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Base64;

/**
 * AES-256-GCM key material for {@code raw_payload.payload_enc}.
 * Local/test stand-in for Vault-sourced key + version id.
 */
@ConfigurationProperties(prefix = "bank.persistence.payload-encryption")
public record PayloadEncryptionProperties(
        String keyId,
        String keyBase64,
        Integer retainYears
) {
    public PayloadEncryptionProperties {
        if (keyId == null || keyId.isBlank()) {
            throw new IllegalStateException(
                    "bank.persistence.payload-encryption.key-id is required");
        }
        if (keyBase64 == null || keyBase64.isBlank()) {
            throw new IllegalStateException(
                    "bank.persistence.payload-encryption.key-base64 is required");
        }
        byte[] key = decodeKey(keyBase64);
        if (key.length != 32) {
            throw new IllegalStateException(
                    "bank.persistence.payload-encryption.key-base64 must decode to exactly 32 bytes, got "
                            + key.length);
        }
        if (retainYears == null || retainYears < 1) {
            retainYears = 7;
        }
    }

    public byte[] decodedKey() {
        return decodeKey(keyBase64);
    }

    public int retainYearsOrDefault() {
        return retainYears != null ? retainYears : 7;
    }

    private static byte[] decodeKey(String keyBase64) {
        try {
            return Base64.getDecoder().decode(keyBase64);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException(
                    "bank.persistence.payload-encryption.key-base64 is not valid Base64", ex);
        }
    }
}
