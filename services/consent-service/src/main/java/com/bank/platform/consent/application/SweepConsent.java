package com.bank.platform.consent.application;

import com.bank.platform.consent.domain.ChallengeRow;
import com.bank.platform.consent.domain.ConsentPorts;
import com.bank.platform.consent.domain.ConsentRow;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Appends EXPIRED rows for grants past validity, and abandons challenges whose code is stale. */
@Service
public class SweepConsent {

  private final ConsentPorts.ConsentStore consents;
  private final ConsentPorts.ChallengeStore challenges;
  private final ConsentPorts.PolicySource policies;
  private final ConsentEvidence evidence;
  private final Clock clock;

  public SweepConsent(
      ConsentPorts.ConsentStore consents,
      ConsentPorts.ChallengeStore challenges,
      ConsentPorts.PolicySource policies,
      ConsentEvidence evidence,
      Clock clock) {
    this.consents = consents;
    this.challenges = challenges;
    this.policies = policies;
    this.evidence = evidence;
    this.clock = clock;
  }

  @Transactional
  public int sweep() {
    Instant now = clock.instant();
    int written = 0;
    for (ConsentRow head : consents.grantedPastValidity(now)) {
      ConsentRow expired =
          evidence.copyState(head, head.recordVersion() + 1, "EXPIRED", head.consentId(), now);
      consents.insert(expired);
      evidence.emitExpired(expired, policies.current());
      written++;
    }
    Instant abandonBefore = now.minus(Duration.ofHours(24));
    for (ChallengeRow stale : challenges.stale(abandonBefore)) {
      challenges.update(stale.withVerification("ABANDONED", stale.attempts()));
      written++;
    }
    return written;
  }
}
