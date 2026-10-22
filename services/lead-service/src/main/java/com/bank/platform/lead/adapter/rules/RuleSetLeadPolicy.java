package com.bank.platform.lead.adapter.rules;

import com.bank.platform.lead.domain.LeadPolicyPort;
import com.bank.platform.lead.domain.RuleConfigurationException;
import com.bank.rules.client.RuleSetResolver;
import com.bank.rules.engine.EvaluationResult;
import com.bank.rules.engine.RuleEngine;
import com.bank.rules.engine.RuleSet;
import com.bank.rules.engine.Values;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Component;

/**
 * {@link LeadPolicyPort} on rule set {@code LEAD.POLICY} (E7): resolved for the lead's lob and
 * evaluated so an unset or non-positive {@code expiryDays} is refused by the rule set itself
 * ({@code CONFIG_MISSING}), not by a constant here.
 */
@Component
public class RuleSetLeadPolicy implements LeadPolicyPort {

  static final String DOMAIN = "LEAD_POLICY";

  private final RuleSetResolver resolver;

  public RuleSetLeadPolicy(RuleSetResolver resolver) {
    this.resolver = resolver;
  }

  @Override
  public Duration expiryFor(String lob, Instant asOf) {
    RuleSet ruleSet = RuleSetLookup.resolve(resolver, DOMAIN, lob, null, asOf);
    EvaluationResult result =
        RuleEngine.evaluate(
            ruleSet, Values.mapper().createObjectNode().put("lob", lob), asOf.toString());
    Object days = result.derived().get("expiryDays");
    if (!"RESOLVED".equals(result.outcome()) || !(days instanceof BigDecimal value)) {
      throw new RuleConfigurationException(
          ruleSet.ruleSetId()
              + "@"
              + ruleSet.version()
              + " gives no usable expiryDays for "
              + lob
              + ": "
              + result.outcome()
              + " "
              + result.reasonCodes(),
          false,
          null);
    }
    return Duration.ofDays(value.longValueExact());
  }
}
