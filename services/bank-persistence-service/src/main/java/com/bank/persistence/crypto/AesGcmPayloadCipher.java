package com.bank.persistence.crypto;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * AES-256-GCM helper. Wire format: {@code iv (12 bytes) || ciphertext+tag}.
 */
public final class AesGcmPayloadCipher {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LENGTH_BYTES = 12;
    private static final int TAG_LENGTH_BITS = 128;
    private static final int TAG_LENGTH_BYTES = TAG_LENGTH_BITS / 8;

    private final SecretKey key;
    private final SecureRandom secureRandom;

    public AesGcmPayloadCipher(byte[] keyBytes) {
        if (keyBytes == null || keyBytes.length != 32) {
            throw new IllegalArgumentException("AES-256-GCM key must be exactly 32 bytes");
        }
        this.key = new SecretKeySpec(Arrays.copyOf(keyBytes, keyBytes.length), "AES");
        this.secureRandom = new SecureRandom();
    }

    public byte[] encrypt(byte[] plaintext) {
        if (plaintext == null) {
            throw new IllegalArgumentException("plaintext must not be null");
        }
        try {
            byte[] iv = new byte[IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] ciphertextWithTag = cipher.doFinal(plaintext);

            byte[] blob = new byte[iv.length + ciphertextWithTag.length];
            System.arraycopy(iv, 0, blob, 0, iv.length);
            System.arraycopy(ciphertextWithTag, 0, blob, iv.length, ciphertextWithTag.length);
            return blob;
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("AES-GCM encrypt failed", ex);
        }
    }

    public byte[] decrypt(byte[] blob) {
        if (blob == null || blob.length < IV_LENGTH_BYTES + TAG_LENGTH_BYTES) {
            throw new IllegalArgumentException("ciphertext blob too short");
        }
        try {
            byte[] iv = Arrays.copyOfRange(blob, 0, IV_LENGTH_BYTES);
            byte[] ciphertextWithTag = Arrays.copyOfRange(blob, IV_LENGTH_BYTES, blob.length);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            return cipher.doFinal(ciphertextWithTag);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("AES-GCM decrypt failed", ex);
        }
    }
}
