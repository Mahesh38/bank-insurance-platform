package com.bank.platform.lead.domain;

/**
 * A command the lead's current state does not permit. The application layer maps it to {@code 409
 * ILLEGAL_TRANSITION}; the domain stays free of the error library.
 */
public class IllegalLeadTransitionException extends RuntimeException {

  public IllegalLeadTransitionException(String reason) {
    super(reason);
  }
}
