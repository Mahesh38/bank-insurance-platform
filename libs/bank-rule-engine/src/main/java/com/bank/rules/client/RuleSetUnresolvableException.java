package com.bank.rules.client;

/**
 * No usable rule set could be obtained — the configuration service did not answer within budget,
 * answered with an error, had no version in force, or returned a document that failed validation or
 * its integrity hash. Callers refuse the action with {@code CONFIGURATION_UNRESOLVABLE}.
 */
public class RuleSetUnresolvableException extends RuntimeException {

  public RuleSetUnresolvableException(String message) {
    super(message);
  }

  public RuleSetUnresolvableException(String message, Throwable cause) {
    super(message, cause);
  }
}
