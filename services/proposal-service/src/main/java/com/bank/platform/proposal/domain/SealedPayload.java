package com.bank.platform.proposal.domain;

/** Ciphertext plus the id of the master key that protects its data key. */
public record SealedPayload(byte[] ciphertext, String encryptionKeyId) {}
