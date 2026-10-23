package com.bank.platform.consent.adapter.crypto;

import com.bank.platform.consent.domain.ConsentCodes;
import com.bank.platform.consent.domain.ConsentRefusal;
import com.bank.platform.consent.domain.FieldCipher;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * AES-GCM field encryption. The 12-byte IV is prepended to the ciphertext. The key is not logged.
 */
public final class AesFieldCipher implements FieldCipher {

  public static final String DEV_KEY = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=";
  private static final int IV_LENGTH = 12;
  private static final int TAG_BITS = 128;

  private final byte[] key;
  private final String keyId;
  private final SecureRandom random = new SecureRandom();

  public AesFieldCipher(String keyBase64, String keyId) {
    if (keyBase64 == null || keyBase64.isBlank()) {
      throw new IllegalArgumentException("field key is required");
    }
    if (keyId == null || keyId.isBlank()) {
      throw new IllegalArgumentException("key id is required");
    }
    this.key = Base64.getDecoder().decode(keyBase64);
    if (this.key.length != 32) {
      throw new IllegalArgumentException("field key must be 32 bytes");
    }
    this.keyId = keyId;
  }

  public static boolean isDevKey(String keyBase64) {
    return DEV_KEY.equals(keyBase64);
  }

  @Override
  public byte[] encrypt(String plaintext) {
    if (plaintext == null || plaintext.isBlank()) {
      throw new ConsentRefusal(ConsentCodes.INVALID_REQUEST, "value to protect is required");
    }
    try {
      byte[] iv = new byte[IV_LENGTH];
      random.nextBytes(iv);
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(
          Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(TAG_BITS, iv));
      byte[] body = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
      byte[] out = new byte[iv.length + body.length];
      System.arraycopy(iv, 0, out, 0, iv.length);
      System.arraycopy(body, 0, out, iv.length, body.length);
      return out;
    } catch (ConsentRefusal refusal) {
      throw refusal;
    } catch (Exception ex) {
      throw new ConsentRefusal(
          ConsentCodes.CONSENT_EVIDENCE_WRITE_FAILED, "field encryption failed");
    }
  }

  @Override
  public String decrypt(byte[] ciphertext) {
    if (ciphertext == null || ciphertext.length <= IV_LENGTH) {
      throw new ConsentRefusal(
          ConsentCodes.CONSENT_EVIDENCE_WRITE_FAILED, "stored subject could not be read");
    }
    try {
      byte[] iv = new byte[IV_LENGTH];
      System.arraycopy(ciphertext, 0, iv, 0, IV_LENGTH);
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      // Nonce recovered from this ciphertext. Decrypt must not mint a fresh one.
      // lgtm[java/static-initialization-vector]
      // codeql[java/static-initialization-vector]
      cipher.init(
          Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(TAG_BITS, iv));
      byte[] plain = cipher.doFinal(ciphertext, IV_LENGTH, ciphertext.length - IV_LENGTH);
      return new String(plain, StandardCharsets.UTF_8);
    } catch (ConsentRefusal refusal) {
      throw refusal;
    } catch (Exception ex) {
      throw new ConsentRefusal(
          ConsentCodes.CONSENT_EVIDENCE_WRITE_FAILED, "stored subject could not be read");
    }
  }

  @Override
  public String keyId() {
    return keyId;
  }
}
