package com.bank.platform.consent.domain;

import java.time.Duration;
import java.time.Instant;

/** OTP window, attempt cap and issue cap (CNS-R14, CNS-R15). */
public final class OtpRules {

  public static final Duration VALIDITY = Duration.ofMinutes(10);
  public static final int MAX_VERIFY_ATTEMPTS = 3;
  public static final int MAX_ISSUES_PER_DAY = 5;

  private OtpRules() {}

  public static void checkIssueAllowed(int issuesAlreadyInWindow) {
    if (issuesAlreadyInWindow >= MAX_ISSUES_PER_DAY) {
      throw new ConsentRefusal(ConsentCodes.OTP_ISSUE_RATE_EXCEEDED, "otp issue cap reached");
    }
  }

  public static void checkStillOpen(OtpChallenge challenge, Instant now) {
    if (challenge.status() == ChallengeStatus.LOCKED) {
      throw new ConsentRefusal(ConsentCodes.OTP_ATTEMPTS_EXCEEDED, "otp transaction is locked");
    }
    if (challenge.status() == ChallengeStatus.VERIFIED) {
      throw new ConsentRefusal(
          ConsentCodes.ILLEGAL_TRANSITION, "otp transaction is already verified");
    }
    if (challenge.status() == ChallengeStatus.EXPIRED || now.isAfter(challenge.expiresAt())) {
      throw new ConsentRefusal(ConsentCodes.OTP_EXPIRED, "otp window has elapsed");
    }
    if (challenge.attempts() >= MAX_VERIFY_ATTEMPTS) {
      throw new ConsentRefusal(ConsentCodes.OTP_ATTEMPTS_EXCEEDED, "otp verify cap reached");
    }
  }
}
