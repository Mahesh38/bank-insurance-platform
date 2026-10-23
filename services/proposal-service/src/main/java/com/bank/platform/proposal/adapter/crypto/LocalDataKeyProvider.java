package com.bank.platform.proposal.adapter.crypto;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;

/**
 * Data keys wrapped by a static AES-256 key from configuration. {@code local} and {@code test}
 * profiles only — the configuration refuses it anywhere else, because a key in a property file is
 * not a KMS customer-managed key.
 */
public class LocalDataKeyProvider implements DataKeyProvider {

  private static final int IV_BYTES = 12;

  private final byte[] masterKey;
  private final String masterKeyId;
  private final SecureRandom random = new SecureRandom();

  public LocalDataKeyProvider(String base64Key, String keyId) {
    this.masterKey = Base64.getDecoder().decode(base64Key);
    if (masterKey.length != 32) {
      throw new IllegalArgumentException("local envelope key must be 32 bytes (AES-256)");
    }
    this.masterKeyId = "local:" + keyId;
  }

  @Override
  public DataKey current() {
    byte[] plaintext = new byte[32];
    random.nextBytes(plaintext);
    byte[] iv = new byte[IV_BYTES];
    random.nextBytes(iv);
    byte[] wrapped =
        AesGcmEnvelopeCipher.crypt(Cipher.ENCRYPT_MODE, masterKey, iv, masterKeyId, plaintext);
    byte[] encrypted = new byte[IV_BYTES + wrapped.length];
    System.arraycopy(iv, 0, encrypted, 0, IV_BYTES);
    System.arraycopy(wrapped, 0, encrypted, IV_BYTES, wrapped.length);
    return new DataKey(plaintext, encrypted, masterKeyId);
  }

  @Override
  public byte[] decrypt(byte[] encryptedKey, String keyId) {
    if (!masterKeyId.equals(keyId)) {
      throw new IllegalStateException("payload was sealed under " + keyId + ", not " + masterKeyId);
    }
    byte[] iv = Arrays.copyOfRange(encryptedKey, 0, IV_BYTES);
    byte[] wrapped = Arrays.copyOfRange(encryptedKey, IV_BYTES, encryptedKey.length);
    return AesGcmEnvelopeCipher.crypt(Cipher.DECRYPT_MODE, masterKey, iv, masterKeyId, wrapped);
  }
}
