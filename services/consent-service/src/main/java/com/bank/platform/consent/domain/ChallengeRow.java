package com.bank.platform.consent.domain;

import java.time.Instant;
import java.util.List;

public record ChallengeRow(
    String challengeId,
    String kind,
    String state,
    int attempts,
    int otpIssueCount,
    Instant otpExpiresAt,
    String otpTxnId,
    String otpHash,
    List<String> purposes,
    String statementRefs,
    byte[] contactUsedEnc,
    String encryptionKeyId,
    String customerId,
    String journeyId,
    String leadId,
    String lob,
    String checkpoint,
    String insurerCode,
    String channel,
    String language,
    String targetConsentId,
    String actorId,
    String configVersionRef,
    Instant createdAt) {

  public ChallengeRow withVerification(String newState, int newAttempts) {
    return new ChallengeRow(
        challengeId,
        kind,
        newState,
        newAttempts,
        otpIssueCount,
        otpExpiresAt,
        otpTxnId,
        otpHash,
        purposes,
        statementRefs,
        contactUsedEnc,
        encryptionKeyId,
        customerId,
        journeyId,
        leadId,
        lob,
        checkpoint,
        insurerCode,
        channel,
        language,
        targetConsentId,
        actorId,
        configVersionRef,
        createdAt);
  }

  public ChallengeRow withFreshCode(
      String newState, Instant expiresAt, String txnId, String hash, int issueCount) {
    return new ChallengeRow(
        challengeId,
        kind,
        newState,
        0,
        issueCount,
        expiresAt,
        txnId,
        hash,
        purposes,
        statementRefs,
        contactUsedEnc,
        encryptionKeyId,
        customerId,
        journeyId,
        leadId,
        lob,
        checkpoint,
        insurerCode,
        channel,
        language,
        targetConsentId,
        actorId,
        configVersionRef,
        createdAt);
  }
}
