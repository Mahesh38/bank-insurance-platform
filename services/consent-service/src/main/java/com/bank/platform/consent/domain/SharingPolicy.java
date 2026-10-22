package com.bank.platform.consent.domain;

import java.time.Duration;
import java.util.Optional;

/**
 * CNS-SHR — sharing with one named insurer. 30 days. A second insurer needs its own grant.
 * Withdrawal after submission stops further sharing and asks for an operations task.
 */
public final class SharingPolicy implements ConsentTypePolicy {

  @Override
  public ConsentCode code() {
    return ConsentCode.CNS_SHR;
  }

  @Override
  public Optional<Duration> validity() {
    return Optional.of(Duration.ofDays(30));
  }

  @Override
  public boolean requiresNamedCounterparty() {
    return true;
  }

  @Override
  public boolean absenceBlocksJourney() {
    return true;
  }

  @Override
  public JourneyEffect whenWithdrawn(boolean proposalAlreadySubmitted) {
    if (proposalAlreadySubmitted) {
      return JourneyEffect.HALT_FURTHER_SHARING;
    }
    return JourneyEffect.HALT_JOURNEY;
  }

  @Override
  public JourneyEffect whenAbsent() {
    return JourneyEffect.BLOCK_STEP;
  }
}
