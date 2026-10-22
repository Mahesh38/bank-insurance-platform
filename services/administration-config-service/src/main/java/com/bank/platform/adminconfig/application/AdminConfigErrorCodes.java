package com.bank.platform.adminconfig.application;

import com.bank.common.error.ErrorCodes;

/**
 * The refusals this service raises. They are registered in the shared {@code ErrorCatalogue}
 * (moved there from this service's own provider, SUG-20261022-rdy), so these are aliases kept for
 * readability at the throw sites.
 */
public final class AdminConfigErrorCodes {

  /** The submitted document is not a valid rule-set/v1 (422). */
  public static final String RULE_SET_INVALID = ErrorCodes.RULE_SET_INVALID;

  /** Publish refused: at least one of the document's test cases fails (422). */
  public static final String RULE_SET_TEST_FAILED = ErrorCodes.RULE_SET_TEST_FAILED;

  /** The checker is the maker (403). */
  public static final String MAKER_CHECKER_VIOLATION = ErrorCodes.MAKER_CHECKER_VIOLATION;

  private AdminConfigErrorCodes() {}
}
