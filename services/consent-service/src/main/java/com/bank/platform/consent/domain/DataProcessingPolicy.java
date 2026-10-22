package com.bank.platform.consent.domain;

import java.time.Duration;
import java.util.Optional;

/** CNS-DP — data processing and CBS prefill. 90 days. Absence blocks the journey. */
public final class DataProcessingPolicy implements ConsentTypePolicy {

  @Override
  public ConsentCode code() {
    return ConsentCode.CNS_DP;
  }

  @Override
  public Optional<Duration> validity() {
    return Optional.of(Duration.ofDays(90));
  }

  @Override
  public boolean requiresNamedCounterparty() {
    return false;
  }

  @Override
  public boolean absenceBlocksJourney() {
    return true;
  }

  @Override
  public JourneyEffect whenWithdrawn(boolean proposalAlreadySubmitted) {
    return JourneyEffect.HALT_JOURNEY;
  }

  @Override
  public JourneyEffect whenAbsent() {
    return JourneyEffect.BLOCK_STEP;
  }
}
