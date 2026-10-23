package com.bank.platform.lead.adapter.rules;

import com.bank.rules.client.RuleSetQuery;
import com.bank.rules.client.RuleSetResolver;
import com.bank.rules.client.RuleSetUnresolvableException;
import com.bank.rules.engine.RuleSet;
import com.bank.rules.engine.RuleSetParser;
import com.bank.rules.engine.RuleSetSeeds;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * LOCAL AND TEST ONLY ({@code lead.rule-sets.source=classpath}): serves the seed rule sets shipped
 * in {@code libs/bank-rule-engine} as if they were published, so the service runs without
 * administration-config-service. Seeds are unapproved DRAFTs (ADR-024 §9) and their effective dates
 * are ignored here — which is exactly why {@code ProductionSafetyGuard} refuses this resolver under
 * prod / uat. Scope matching follows the service: specific beats wildcard ({@code null}).
 */
public final class ClasspathSeedRuleSetResolver implements RuleSetResolver {

  private final List<RuleSet> seeds;

  public ClasspathSeedRuleSetResolver() {
    this(RuleSetSeeds.all().stream().map(s -> RuleSetParser.parse(s.json())).toList());
  }

  ClasspathSeedRuleSetResolver(List<RuleSet> seeds) {
    this.seeds = List.copyOf(seeds);
  }

  @Override
  public RuleSet resolve(RuleSetQuery query) {
    return seeds.stream()
        .filter(rs -> rs.domain().equals(query.domain()))
        .filter(rs -> matches(rs.scope().lob(), query.lob()))
        .filter(rs -> matches(rs.scope().productClass(), query.productClass()))
        .max(Comparator.comparingInt(ClasspathSeedRuleSetResolver::specificity))
        .orElseThrow(
            () ->
                new RuleSetUnresolvableException(
                    "no seed rule set for " + query.domain() + "/" + query.lob()));
  }

  @Override
  public RuleSet version(String ruleSetId, String version) {
    return seeds.stream()
        .filter(rs -> rs.ruleSetId().equals(ruleSetId) && rs.version().equals(version))
        .findFirst()
        .orElseThrow(
            () -> new RuleSetUnresolvableException("no seed " + ruleSetId + "@" + version));
  }

  private static boolean matches(String scoped, String asked) {
    return scoped == null || Objects.equals(scoped, asked);
  }

  private static int specificity(RuleSet rs) {
    return (rs.scope().lob() == null ? 0 : 1) + (rs.scope().productClass() == null ? 0 : 1);
  }
}
