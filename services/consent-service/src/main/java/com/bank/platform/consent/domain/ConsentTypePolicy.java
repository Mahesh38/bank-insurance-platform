package com.bank.platform.consent.domain;

import java.time.Duration;
import java.time.Instant;

/**
 * Type-specific consent behaviour. A new purpose is a new policy, not a branch inside the engine.
 *
 * <p>The registry selects the policy from the consent code, so validity, scope, withdrawal and
 * whether the journey stops all follow the type.
 */
public interface ConsentTypePolicy {

  ConsentCode code();

  /** Empty when the grant lasts until withdrawal (CNS-COM). */
  java.util.Optional<Duration> validity();

  boolean requiresNamedCounterparty();

  /** True when absence or expiry must stop the journey step. False for optional communications. */
  boolean absenceBlocksJourney();

  JourneyEffect whenWithdrawn(boolean proposalAlreadySubmitted);

  JourneyEffect whenAbsent();

  default Instant validUntil(Instant validFrom) {
    return validity().map(validFrom::plus).orElse(null);
  }

  /** A MAJOR statement change forces re-consent only before this type's blocking point. */
  default boolean majorVersionForcesReconsent(boolean pastBlockingPoint) {
    return !pastBlockingPoint;
  }
}
