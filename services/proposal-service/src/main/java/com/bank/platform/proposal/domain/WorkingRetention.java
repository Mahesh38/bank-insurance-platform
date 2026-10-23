package com.bank.platform.proposal.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * {@code RET-WORKING} (schemas/20 §1.2): last update + {@code working}; no expiry while a payment
 * is in flight, so a TTL can never delete the proposal the money is for; materialised + {@code
 * afterMaterialize}.
 */
public record WorkingRetention(Duration working, Duration afterMaterialize) {

  public Optional<Instant> expiryOf(
      boolean paymentInFlight, Instant materializedAt, Instant updatedAt) {
    if (materializedAt != null) {
      return Optional.of(materializedAt.plus(afterMaterialize));
    }
    if (paymentInFlight) {
      return Optional.empty();
    }
    return Optional.of(updatedAt.plus(working));
  }
}
