package com.bank.platform.quotation.domain;

/** Ciphertext plus the id of the master key that protects its data key. */
public record SealedPayload(byte[] ciphertext, String encryptionKeyId) {}
