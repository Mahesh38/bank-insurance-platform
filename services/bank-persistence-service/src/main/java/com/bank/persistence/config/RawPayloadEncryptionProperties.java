package com.bank.persistence.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * AES-256-GCM key (base64, decodes to 32 bytes) used to encrypt {@code raw_payload.payload_enc}
 * at rest (COMP-003). Bound from {@code raw-payload.encryption.key}
 * / {@code RAW_PAYLOAD_ENCRYPTION_KEY}. No default in uat/prod — see application-uat.yml
 * / application-prod.yml; missing/invalid key fails service startup
 * ({@link com.bank.persistence.crypto.RawPayloadEncryptionService}).
 */
@ConfigurationProperties(prefix = "raw-payload.encryption")
public record RawPayloadEncryptionProperties(String key) {}
