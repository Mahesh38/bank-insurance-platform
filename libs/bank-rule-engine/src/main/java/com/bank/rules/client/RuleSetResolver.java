package com.bank.rules.client;

import com.bank.rules.engine.RuleSet;

/**
 * Port through which a deciding service obtains rule sets. Implementations fail closed: they return
 * a parsed, validated rule set or throw {@link RuleSetUnresolvableException}, which callers map to
 * {@code CONFIGURATION_UNRESOLVABLE}. There is never a compiled-in default (ADR-024 section 5 point
 * 4).
 */
public interface RuleSetResolver {

  /** The published version in force for the query. */
  RuleSet resolve(RuleSetQuery query);

  /**
   * An exact published version, for journeys pinned to the version they started with ({@code
   * pinning: JOURNEY}, ADR-024 section 5 point 5).
   */
  RuleSet version(String ruleSetId, String version);
}
