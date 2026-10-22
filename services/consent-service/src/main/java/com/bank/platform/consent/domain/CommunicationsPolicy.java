package com.bank.platform.consent.domain;

import java.time.Duration;
import java.util.Optional;

/**
 * CNS-COM — optional servicing communications. No expiry. Withdrawal suppresses marketing and
 * leaves regulatory messages alone. The sale journey is not halted.
 */
public final class CommunicationsPolicy implements ConsentTypePolicy {

  @Override
  public ConsentCode code() {
    return ConsentCode.CNS_COM;
  }

  @Override
  public Optional<Duration> validity() {
    return Optional.empty();
  }

  @Override
  public boolean requiresNamedCounterparty() {
    return false;
  }

  @Override
  public boolean absenceBlocksJourney() {
    return false;
  }

  @Override
  public JourneyEffect whenWithdrawn(boolean proposalAlreadySubmitted) {
    return JourneyEffect.SUPPRESS_OPTIONAL_COMMUNICATIONS;
  }

  @Override
  public JourneyEffect whenAbsent() {
    return JourneyEffect.SUPPRESS_OPTIONAL_COMMUNICATIONS;
  }
}
