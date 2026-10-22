package com.bank.platform.lead.adapter.crypto;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * AES-256-GCM for the RESTRICTED columns of {@code lead_lms} ({@code note_enc}, {@code
 * meeting_link_enc}). Layout: 12-byte IV, then ciphertext + tag. The key id is stored beside the
 * ciphertext so a rotated key can still read old rows.
 *
 * <p>The default key in {@code application.yml} is 32 zero bytes for local runs; every shared
 * environment injects a KMS-managed key ({@code LEAD_FIELD_KEY}, {@code LEAD_FIELD_KEY_ID}).
 */
@Component
public class FieldCipher {

  private static final int IV_BYTES = 12;
  private static final int TAG_BITS = 128;

  private final SecretKeySpec key;
  private final String keyId;
  private final SecureRandom random = new SecureRandom();

  public FieldCipher(
      @Value("${lead.crypto.field-key}") String base64Key,
      @Value("${lead.crypto.key-id}") String keyId) {
    byte[] raw = Base64.getDecoder().decode(base64Key);
    if (raw.length != 32) {
      throw new IllegalStateException("lead.crypto.field-key must be 32 bytes (AES-256)");
    }
    this.key = new SecretKeySpec(raw, "AES");
    this.keyId = keyId;
  }

  public String keyId() {
    return keyId;
  }

  public byte[] encrypt(String plaintext) {
    try {
      byte[] iv = new byte[IV_BYTES];
      random.nextBytes(iv);
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
      byte[] sealed = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
      // Math.addExact: sealed.length is cipher output — CodeQL flags plain int sum as overflow risk.
      int size = Math.addExact(iv.length, sealed.length);
      return ByteBuffer.allocate(size).put(iv).put(sealed).array();
    } catch (GeneralSecurityException ex) {
      throw new IllegalStateException("field encryption failed", ex);
    }
  }

  public String decrypt(byte[] stored) {
    try {
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, stored, 0, IV_BYTES));
      byte[] plain = cipher.doFinal(stored, IV_BYTES, stored.length - IV_BYTES);
      return new String(plain, StandardCharsets.UTF_8);
    } catch (GeneralSecurityException ex) {
      throw new IllegalStateException("field decryption failed", ex);
    }
  }
}
