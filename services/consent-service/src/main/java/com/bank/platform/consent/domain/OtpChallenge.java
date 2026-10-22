package com.bank.platform.consent.domain;

import java.time.Instant;
import java.util.Arrays;

/**
 * Operational OTP transaction. Not consent evidence. Attempts may advance; the code is stored as a
 * hash.
 */
public final class OtpChallenge {

  private final String challengeId;
  private final ChallengePurpose purpose;
  private final String rootConsentId;
  private final String customerId;
  private final ConsentCode code;
  private final String otpHash;
  private final Instant expiresAt;
  private final int attempts;
  private final ChallengeStatus status;
  private final PendingCapture pending;
  private final Instant createdAt;

  public OtpChallenge(
      String challengeId,
      ChallengePurpose purpose,
      String rootConsentId,
      String customerId,
      ConsentCode code,
      String otpHash,
      Instant expiresAt,
      int attempts,
      ChallengeStatus status,
      PendingCapture pending,
      Instant createdAt) {
    this.challengeId = challengeId;
    this.purpose = purpose;
    this.rootConsentId = rootConsentId;
    this.customerId = customerId;
    this.code = code;
    this.otpHash = otpHash;
    this.expiresAt = expiresAt;
    this.attempts = attempts;
    this.status = status;
    this.pending = pending;
    this.createdAt = createdAt;
  }

  public OtpChallenge failedAttempt() {
    int next = attempts + 1;
    ChallengeStatus nextStatus =
        next >= OtpRules.MAX_VERIFY_ATTEMPTS ? ChallengeStatus.LOCKED : ChallengeStatus.OPEN;
    return copy(next, nextStatus);
  }

  public OtpChallenge verified() {
    return copy(attempts, ChallengeStatus.VERIFIED);
  }

  public OtpChallenge expired() {
    return copy(attempts, ChallengeStatus.EXPIRED);
  }

  private OtpChallenge copy(int nextAttempts, ChallengeStatus nextStatus) {
    return new OtpChallenge(
        challengeId,
        purpose,
        rootConsentId,
        customerId,
        code,
        otpHash,
        expiresAt,
        nextAttempts,
        nextStatus,
        pending,
        createdAt);
  }

  public String challengeId() {
    return challengeId;
  }

  public ChallengePurpose purpose() {
    return purpose;
  }

  public String rootConsentId() {
    return rootConsentId;
  }

  public String customerId() {
    return customerId;
  }

  public ConsentCode code() {
    return code;
  }

  public String otpHash() {
    return otpHash;
  }

  public Instant expiresAt() {
    return expiresAt;
  }

  public int attempts() {
    return attempts;
  }

  public ChallengeStatus status() {
    return status;
  }

  public PendingCapture pending() {
    return pending;
  }

  public Instant createdAt() {
    return createdAt;
  }

  @Override
  public String toString() {
    return "OtpChallenge{id=" + challengeId + ", purpose=" + purpose + ", status=" + status + "}";
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof OtpChallenge challenge)) {
      return false;
    }
    return challengeId.equals(challenge.challengeId)
        && attempts == challenge.attempts
        && status == challenge.status
        && Arrays.equals(pending.cifEnc(), challenge.pending.cifEnc());
  }

  @Override
  public int hashCode() {
    return challengeId.hashCode();
  }
}
