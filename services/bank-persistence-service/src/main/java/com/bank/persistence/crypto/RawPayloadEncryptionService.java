package com.bank.persistence.crypto;

import com.bank.persistence.config.RawPayloadEncryptionProperties;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * AES-256-GCM encryption for {@code raw_payload.payload_enc} (COMP-003 — raw 1SB request/response
 * bodies encrypted at rest for dispute/audit evidence). Each call encrypts with a fresh random
 * 96-bit IV, stored as a prefix of the returned ciphertext (IV || ciphertext || 128-bit GCM tag).
 * <p>
 * Fails fast at startup (bean construction) if the configured key is missing or not 32 bytes —
 * see {@link RawPayloadEncryptionProperties}.
 */
@Component
public class RawPayloadEncryptionService {

    /** Logical key identifier persisted per row, so a future key rotation can be tracked. */
    public static final String KEY_ID = "raw-payload-key-v1";

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final int GCM_IV_LENGTH_BYTES = 12;

    private final SecretKeySpec key;
    private final SecureRandom secureRandom = new SecureRandom();

    public RawPayloadEncryptionService(RawPayloadEncryptionProperties properties) {
        if (properties.key() == null || properties.key().isBlank()) {
            throw new IllegalStateException(
                    "raw-payload.encryption.key (RAW_PAYLOAD_ENCRYPTION_KEY) is not set. "
                            + "Raw payload storage requires an AES-256 key at startup — refusing to start.");
        }
        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(properties.key());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(
                    "raw-payload.encryption.key must be valid base64", e);
        }
        if (keyBytes.length != 32) {
            throw new IllegalStateException(
                    "raw-payload.encryption.key must decode to 32 bytes (AES-256); got "
                            + keyBytes.length + " bytes");
        }
        this.key = new SecretKeySpec(keyBytes, "AES");
    }

    public String keyId() {
        return KEY_ID;
    }

    public byte[] encrypt(String plaintext) {
        try {
            byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return ByteBuffer.allocate(iv.length + ciphertext.length)
                    .put(iv)
                    .put(ciphertext)
                    .array();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to encrypt raw payload", e);
        }
    }

    public String decrypt(byte[] encrypted) {
        if (encrypted == null || encrypted.length <= GCM_IV_LENGTH_BYTES) {
            throw new IllegalArgumentException("Encrypted payload too short to contain an IV");
        }
        try {
            byte[] iv = Arrays.copyOfRange(encrypted, 0, GCM_IV_LENGTH_BYTES);
            byte[] ciphertext = Arrays.copyOfRange(encrypted, GCM_IV_LENGTH_BYTES, encrypted.length);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] plaintext = cipher.doFinal(ciphertext);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to decrypt raw payload", e);
        }
    }
}
