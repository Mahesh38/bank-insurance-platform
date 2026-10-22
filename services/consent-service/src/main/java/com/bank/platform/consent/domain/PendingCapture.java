package com.bank.platform.consent.domain;

/**
 * Facts collected when a challenge is opened. Ciphertext only — the clear CIF never sits on this
 * object, so a log of it cannot leak the customer.
 */
public record PendingCapture(
    byte[] cifEnc,
    String encryptionKeyId,
    byte[] contactRefEnc,
    String leadId,
    String journeyId,
    String statementId,
    String statementVersion,
    String statementText,
    String statementTextHash,
    String language,
    ConsentChannel channel,
    String capturedByActorId,
    String capturedByAgentId,
    String distributorId,
    String counterpartyId,
    String lob,
    String configVersionRef,
    String withdrawalReasonCode,
    boolean proposalSubmitted,
    String sourceIp) {}
