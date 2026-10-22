package com.bank.platform.consent.domain;

/**
 * A domain refusal. The message is safe to log: it never carries CIF, OTP, IP, or a phone number.
 */
public final class ConsentRefusal extends RuntimeException {

  private final String code;

  public ConsentRefusal(String code, String safeReason) {
    super(safeReason);
    this.code = code;
  }

  public String code() {
    return code;
  }
}
