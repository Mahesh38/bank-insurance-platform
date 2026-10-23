package com.bank.rules.client;

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * What a deciding service asks for: the rule set in force for (domain, lob, productClass) at {@code
 * asOf} (ADR-024 section 5 point 4). {@code lob} and {@code productClass} may be null.
 */
public record RuleSetQuery(String domain, String lob, String productClass, OffsetDateTime asOf) {

  public RuleSetQuery {
    Objects.requireNonNull(domain, "domain");
    Objects.requireNonNull(asOf, "asOf");
  }

  /** Cache key: everything except the instant. */
  String scopeKey() {
    return domain + "|" + lob + "|" + productClass;
  }
}
