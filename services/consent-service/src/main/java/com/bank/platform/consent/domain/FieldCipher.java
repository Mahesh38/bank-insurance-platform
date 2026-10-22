package com.bank.platform.consent.domain;

public interface FieldCipher {

  byte[] encrypt(String plaintext);

  String decrypt(byte[] ciphertext);

  String keyId();
}
