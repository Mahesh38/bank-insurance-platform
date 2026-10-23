package com.bank.platform.proposal.adapter.crypto;

import com.bank.platform.proposal.domain.EnvelopeCipher;
import com.bank.platform.proposal.domain.SealedPayload;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * AES-256-GCM envelope encryption. The encrypted data key travels inside the payload, so a sealed
 * payload plus its {@code encryptionKeyId} is self-contained:
 *
 * <pre>
 * [1 format=1][2 data-key length][encrypted data key][12 IV][ciphertext + 16 tag]
 * </pre>
 */
public class AesGcmEnvelopeCipher implements EnvelopeCipher {

  private static final byte FORMAT = 1;
  private static final int IV_BYTES = 12;
  private static final int TAG_BITS = 128;

  private final DataKeyProvider keys;
  private final SecureRandom random = new SecureRandom();

  public AesGcmEnvelopeCipher(DataKeyProvider keys) {
    this.keys = keys;
  }

  @Override
  public SealedPayload seal(byte[] plaintext, String context) {
    DataKey key = keys.current();
    byte[] iv = new byte[IV_BYTES];
    random.nextBytes(iv);
    byte[] ciphertext = crypt(Cipher.ENCRYPT_MODE, key.plaintext(), iv, context, plaintext);
    byte[] encryptedKey = key.encrypted();
    if (encryptedKey.length > Short.MAX_VALUE) {
      throw new IllegalStateException("encrypted data key exceeds envelope length field");
    }
    // Math.addExact: lengths come from cipher / key material — CodeQL flags plain int sum.
    int size =
        Math.addExact(
            Math.addExact(Math.addExact(1 + 2, encryptedKey.length), IV_BYTES), ciphertext.length);
    ByteBuffer out = ByteBuffer.allocate(size);
    out.put(FORMAT).putShort((short) encryptedKey.length).put(encryptedKey);
    out.put(iv).put(ciphertext);
    return new SealedPayload(out.array(), key.masterKeyId());
  }

  @Override
  public byte[] open(SealedPayload sealed, String context) {
    ByteBuffer in = ByteBuffer.wrap(sealed.ciphertext());
    byte format = in.get();
    if (format != FORMAT) {
      throw new IllegalStateException("unknown envelope format " + format);
    }
    byte[] encryptedKey = new byte[Short.toUnsignedInt(in.getShort())];
    in.get(encryptedKey);
    byte[] iv = new byte[IV_BYTES];
    in.get(iv);
    byte[] ciphertext = new byte[in.remaining()];
    in.get(ciphertext);
    byte[] dataKey = keys.decrypt(encryptedKey, sealed.encryptionKeyId());
    return crypt(Cipher.DECRYPT_MODE, dataKey, iv, context, ciphertext);
  }

  static byte[] crypt(int mode, byte[] key, byte[] iv, String aad, byte[] input) {
    try {
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(mode, new SecretKeySpec(key, "AES"), new GCMParameterSpec(TAG_BITS, iv));
      if (aad != null) {
        cipher.updateAAD(aad.getBytes(StandardCharsets.UTF_8));
      }
      return cipher.doFinal(input);
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException("envelope " + modeName(mode) + " failed", e);
    }
  }

  private static String modeName(int mode) {
    return mode == Cipher.ENCRYPT_MODE ? "encryption" : "decryption";
  }
}
