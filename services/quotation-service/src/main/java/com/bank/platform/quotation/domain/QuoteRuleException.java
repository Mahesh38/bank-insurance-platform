package com.bank.platform.quotation.domain;

/**
 * A quote state rule refused a change. {@code code} is an {@code ErrorCodes} value; the application
 * layer turns it into the catalogue error so the domain stays free of the factory.
 */
public class QuoteRuleException extends RuntimeException {

  private final String code;

  public QuoteRuleException(String code, String reason) {
    super(reason);
    this.code = code;
  }

  public String code() {
    return code;
  }
}
