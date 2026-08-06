package com.bank.workforce.bff.session;

import com.bank.workforce.bff.config.WorkforceSessionProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class TokenVaultCipher {

    private static final int IV_LENGTH = 12;
    private static final int TAG_BITS = 128;

    private final SecretKeySpec key;
    private final ObjectMapper objectMapper;
    private final SecureRandom secureRandom = new SecureRandom();

    public TokenVaultCipher(WorkforceSessionProperties properties, ObjectMapper objectMapper) {
        byte[] decoded = Base64.getDecoder().decode(properties.encryptionKey());
        if (decoded.length != 32) {
            throw new IllegalArgumentException("workforce.session.encryption-key must decode to 32 bytes");
        }
        this.key = new SecretKeySpec(decoded, "AES");
        this.objectMapper = objectMapper;
    }

    public String encrypt(Object value) {
        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] ciphertext = cipher.doFinal(objectMapper.writeValueAsBytes(value));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(
                ByteBuffer.allocate(iv.length + ciphertext.length).put(iv).put(ciphertext).array());
        } catch (GeneralSecurityException | IOException exception) {
            throw new IllegalStateException("Unable to encrypt workforce session", exception);
        }
    }

    public <T> T decrypt(String encrypted, Class<T> type) {
        try {
            byte[] combined = Base64.getUrlDecoder().decode(encrypted);
            ByteBuffer buffer = ByteBuffer.wrap(combined);
            byte[] iv = new byte[IV_LENGTH];
            buffer.get(iv);
            byte[] ciphertext = new byte[buffer.remaining()];
            buffer.get(ciphertext);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            return objectMapper.readValue(cipher.doFinal(ciphertext), type);
        } catch (GeneralSecurityException | IOException | IllegalArgumentException exception) {
            throw new IllegalStateException("Unable to decrypt workforce session", exception);
        }
    }
}
