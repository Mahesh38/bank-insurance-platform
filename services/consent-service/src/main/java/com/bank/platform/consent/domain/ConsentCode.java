package com.bank.platform.consent.domain;

/** R0 consent events. Each one is a separate purpose under DPDP, never a bundle. */
public enum ConsentCode {
  CNS_DP("CNS-DP"),
  CNS_SOL("CNS-SOL"),
  CNS_SHR("CNS-SHR"),
  CNS_COM("CNS-COM");

  private final String wire;

  ConsentCode(String wire) {
    this.wire = wire;
  }

  public String wire() {
    return wire;
  }

  public static ConsentCode parse(String raw) {
    if (raw == null || raw.isBlank()) {
      throw new ConsentRefusal(ConsentCodes.INVALID_REQUEST, "consent code is required");
    }
    String trimmed = raw.trim();
    if ("CNS-RDR".equals(trimmed)) {
      throw new ConsentRefusal(
          ConsentCodes.INVALID_REQUEST, "CNS-RDR is outside the current release");
    }
    for (ConsentCode code : values()) {
      if (code.wire.equals(trimmed)) {
        return code;
      }
    }
    throw new ConsentRefusal(ConsentCodes.INVALID_REQUEST, "unknown consent code");
  }
}
