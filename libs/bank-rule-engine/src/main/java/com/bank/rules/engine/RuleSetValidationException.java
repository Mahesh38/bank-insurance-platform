package com.bank.rules.engine;

import java.util.List;

/** The document is not a valid rule-set/v1; {@link #errors()} lists every finding with its path. */
public class RuleSetValidationException extends RuntimeException {

  private final List<String> errors;

  public RuleSetValidationException(List<String> errors) {
    super("rule set is invalid: " + String.join("; ", errors));
    this.errors = List.copyOf(errors);
  }

  public List<String> errors() {
    return errors;
  }
}
