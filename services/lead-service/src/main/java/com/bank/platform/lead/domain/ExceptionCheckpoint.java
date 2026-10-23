package com.bank.platform.lead.domain;

/**
 * Where {@code LEAD.EXCEPTION} is evaluated (Exception Handling BRD §6.2, §24.3 column E): Start
 * Onboarding evaluates every rule whose data is available; rules 012–013 again after the annual
 * premium is captured. The names are the rule set's {@code triggerPoint} values.
 */
public enum ExceptionCheckpoint {
  START_ONBOARDING,
  PREMIUM_CAPTURE
}
