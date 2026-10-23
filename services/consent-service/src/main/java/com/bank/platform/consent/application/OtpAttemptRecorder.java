package com.bank.platform.consent.application;

import com.bank.platform.consent.domain.ChallengeRow;
import com.bank.platform.consent.domain.ConsentPorts;
import com.bank.platform.consent.domain.ConsentRefusal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** A failed attempt commits on its own, so the refusal does not roll the counter back. */
@Service
public class OtpAttemptRecorder {

  private final ConsentPorts.ChallengeStore challenges;

  public OtpAttemptRecorder(ConsentPorts.ChallengeStore challenges) {
    this.challenges = challenges;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public ChallengeRow recordFailure(String challengeId, int maxAttempts) {
    ChallengeRow row =
        challenges
            .find(challengeId)
            .orElseThrow(() -> new ConsentRefusal("RESOURCE_NOT_FOUND", "challenge was not found"));
    int attempts = row.attempts() + 1;
    String state = attempts > maxAttempts ? "LOCKED" : row.state();
    ChallengeRow updated = row.withVerification(state, attempts);
    challenges.update(updated);
    return updated;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void lock(String challengeId) {
    ChallengeRow row =
        challenges
            .find(challengeId)
            .orElseThrow(() -> new ConsentRefusal("RESOURCE_NOT_FOUND", "challenge was not found"));
    if (!"LOCKED".equals(row.state())) {
      challenges.update(row.withVerification("LOCKED", row.attempts()));
    }
  }
}
