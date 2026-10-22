package com.bank.platform.adminconfig.config;

import com.bank.common.error.ErrorCategory;
import com.bank.common.error.ErrorDefinition;
import com.bank.common.error.ErrorDefinitionProvider;
import com.bank.platform.adminconfig.application.AdminConfigErrorCodes;
import java.util.Collection;
import java.util.List;

/**
 * This service's own refusals, contributed to the platform catalogue by {@link
 * java.util.ServiceLoader} (META-INF/services). Additive only.
 */
public class AdminConfigErrorDefinitions implements ErrorDefinitionProvider {

  @Override
  public Collection<ErrorDefinition> definitions() {
    return List.of(
        new ErrorDefinition(
            AdminConfigErrorCodes.RULE_SET_INVALID,
            ErrorCategory.VALIDATION,
            422,
            null,
            "Rule set not valid",
            "The rule set document has errors; see the error list.",
            null,
            null,
            null,
            "ADR-024 section 2"),
        new ErrorDefinition(
            AdminConfigErrorCodes.RULE_SET_TEST_FAILED,
            ErrorCategory.VALIDATION,
            422,
            null,
            "Test cases failed",
            "The rule set cannot be published until every test case passes.",
            null,
            null,
            null,
            "ADR-024 section 5"),
        new ErrorDefinition(
            AdminConfigErrorCodes.MAKER_CHECKER_VIOLATION,
            ErrorCategory.AUTHORIZATION,
            403,
            null,
            "Not permitted",
            "The approver must be a different person from the author.",
            null,
            null,
            null,
            "ADR-024 section 5"));
  }
}
