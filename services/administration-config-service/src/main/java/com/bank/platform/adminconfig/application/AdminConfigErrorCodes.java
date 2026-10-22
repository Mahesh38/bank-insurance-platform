package com.bank.platform.adminconfig.application;

/**
 * Codes this service adds to the platform catalogue through {@code AdminConfigErrorDefinitions} (an
 * {@code ErrorDefinitionProvider}), so the shared {@code ErrorCodes} stays untouched.
 */
public final class AdminConfigErrorCodes {

  /** The submitted document is not a valid rule-set/v1 (422). */
  public static final String RULE_SET_INVALID = "RULE_SET_INVALID";

  /** Publish refused: at least one of the document's test cases fails (422). */
  public static final String RULE_SET_TEST_FAILED = "RULE_SET_TEST_FAILED";

  /** The checker is the maker (403). */
  public static final String MAKER_CHECKER_VIOLATION = "MAKER_CHECKER_VIOLATION";

  private AdminConfigErrorCodes() {}
}
