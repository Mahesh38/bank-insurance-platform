package com.bank.platform.consent.domain;

import java.time.Duration;
import java.util.Optional;

/** CNS-SOL — solicitation, need analysis and suitability. 90 days. Absence blocks suitability. */
public final class SolicitationPolicy implements ConsentTypePolicy {

  @Override
  public ConsentCode code() {
    return ConsentCode.CNS_SOL;
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
