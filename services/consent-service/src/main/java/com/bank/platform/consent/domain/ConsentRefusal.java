package com.bank.platform.consent.domain;

/**
 * A domain refusal. The message is safe to log: it never carries CIF, OTP, IP, or a phone number.
 */
public final class ConsentRefusal extends RuntimeException {

  private final String code;
  private final Integer attemptsRemaining;

  public ConsentRefusal(String code, String safeReason) {
    this(code, safeReason, null);
  }

  public ConsentRefusal(String code, String safeReason, Integer attemptsRemaining) {
    super(safeReason);
    this.code = code;
    this.attemptsRemaining = attemptsRemaining;
  }

  public String code() {
    return code;
  }

  public Integer attemptsRemaining() {
    return attemptsRemaining;
  }
}
