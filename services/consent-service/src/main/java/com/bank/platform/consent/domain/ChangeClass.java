package com.bank.platform.consent.domain;

public enum ChangeClass {
  MAJOR,
  MINOR;

  public static ChangeClass parse(String raw) {
    if (raw == null || raw.isBlank()) {
      throw new ConsentRefusal(ConsentCodes.INVALID_REQUEST, "change class is required");
    }
    for (ChangeClass value : values()) {
      if (value.name().equals(raw.trim())) {
        return value;
      }
    }
    throw new ConsentRefusal(ConsentCodes.INVALID_REQUEST, "change class must be MAJOR or MINOR");
  }
}
