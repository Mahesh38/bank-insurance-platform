package com.bank.platform.consent.domain;

/** Stored or derived state of a consent grant. Evidence rows only persist GRANTED and WITHDRAWN. */
public enum ConsentState {
  OTP_PENDING,
  GRANTED,
  WITHDRAWN,
  EXPIRED,
  SUPERSEDED,
  ABANDONED;

  public static ConsentState parse(String raw) {
    if (raw == null || raw.isBlank()) {
      throw new ConsentRefusal(ConsentCodes.INVALID_REQUEST, "consent state is required");
    }
    for (ConsentState state : values()) {
      if (state.name().equals(raw)) {
        return state;
      }
    }
    throw new ConsentRefusal(ConsentCodes.INVALID_REQUEST, "unknown consent state");
  }
}
