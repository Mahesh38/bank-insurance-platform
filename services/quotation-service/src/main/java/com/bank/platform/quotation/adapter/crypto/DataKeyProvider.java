package com.bank.platform.quotation.adapter.crypto;

/** Source of data keys for envelope encryption: KMS in every real environment, local in tests. */
public interface DataKeyProvider {

  /** A data key to encrypt with. May be reused for a short time. */
  DataKey current();

  /** The plaintext of a stored encrypted data key. */
  byte[] decrypt(byte[] encryptedKey, String masterKeyId);
}
