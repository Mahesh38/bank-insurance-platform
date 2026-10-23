package com.bank.platform.proposal.domain;

/**
 * Client-side envelope encryption of working payloads (DB-DEC-0005: table read access must not
 * reveal members, nominee or form PII). {@code context} is bound as additional authenticated data,
 * so a ciphertext copied onto another item does not decrypt.
 */
public interface EnvelopeCipher {

  SealedPayload seal(byte[] plaintext, String context);

  byte[] open(SealedPayload sealed, String context);
}
