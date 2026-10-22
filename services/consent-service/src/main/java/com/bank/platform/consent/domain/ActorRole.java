package com.bank.platform.consent.domain;

/** Who is performing the call. OTP entry is refused for the RM role (CNS-R13). */
public enum ActorRole {
  RM,
  CUSTOMER_DEVICE;

  public static ActorRole parse(String raw) {
    if (raw == null || raw.isBlank()) {
      throw new ConsentRefusal(ConsentCodes.INVALID_REQUEST, "actor role is required");
    }
    for (ActorRole role : values()) {
      if (role.name().equals(raw.trim())) {
        return role;
      }
    }
    throw new ConsentRefusal(ConsentCodes.INVALID_REQUEST, "unknown actor role");
  }
}
