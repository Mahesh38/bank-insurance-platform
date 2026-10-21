package com.bank.common.apigee;

import java.time.Instant;
import java.util.Objects;

/** Access token minted from Apigee {@code /token} plus its absolute expiry. */
public record IssuedToken(String value, Instant expiresAt) {

  public IssuedToken {
    Objects.requireNonNull(value, "value");
    Objects.requireNonNull(expiresAt, "expiresAt");
    if (value.isBlank()) {
      throw new IllegalArgumentException("token value is blank");
    }
  }
}
