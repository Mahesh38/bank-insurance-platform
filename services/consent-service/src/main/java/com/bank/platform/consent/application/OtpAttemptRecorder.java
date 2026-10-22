package com.bank.platform.consent.application;

import com.bank.platform.consent.domain.OtpChallenge;
import com.bank.platform.consent.domain.OtpChallengeStore;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Commits an OTP failure or expiry on its own. The verify use case then refuses, and that refusal
 * must not roll the attempt counter back.
 */
@Component
public class OtpAttemptRecorder {

  private final OtpChallengeStore challenges;

  public OtpAttemptRecorder(OtpChallengeStore challenges) {
    this.challenges = challenges;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void commit(OtpChallenge challenge) {
    challenges.save(challenge);
  }
}
