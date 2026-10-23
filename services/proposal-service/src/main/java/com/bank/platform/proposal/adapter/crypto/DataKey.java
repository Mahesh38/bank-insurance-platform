package com.bank.platform.proposal.adapter.crypto;

/**
 * A data key: the plaintext used for AES-GCM in process, and the same key encrypted under the
 * master key, which is what is stored beside the ciphertext.
 */
public record DataKey(byte[] plaintext, byte[] encrypted, String masterKeyId) {}
