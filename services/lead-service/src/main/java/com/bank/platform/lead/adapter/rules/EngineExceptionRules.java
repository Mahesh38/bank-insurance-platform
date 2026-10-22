package com.bank.platform.lead.adapter.rules;

import com.bank.platform.lead.domain.ExceptionCheckpoint;
import com.bank.platform.lead.domain.ExceptionOutcome;
import com.bank.platform.lead.domain.ExceptionRulesPort;
import com.bank.platform.lead.domain.LeadExceptionFactsPort;
import com.bank.platform.lead.domain.LeadExceptionFactsPort.Fact;
import com.bank.platform.lead.domain.LeadExceptionFactsPort.Facts;
import com.bank.rules.client.RuleSetResolver;
import com.bank.rules.engine.EvaluationResult;
import com.bank.rules.engine.RuleEngine;
import com.bank.rules.engine.RuleSet;
import com.bank.rules.engine.Values;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * {@link ExceptionRulesPort} on the configuration-driven engine (D2, ADR-024): resolves the
 * published {@code LEAD.EXCEPTION} in force for the lead's lob and evaluates it with the facts from
 * {@link LeadExceptionFactsPort}. The only production evaluator; {@code ProductionSafetyGuard}
 * refuses prod/uat without it.
 *
 * <p>Unknown facts (BRD §24.3 column I, E9): a required one is simply not passed, so the engine
 * returns {@code BLOCK / RULE_INPUT_MISSING}. An unknown <em>optional</em> fact would read as "no
 * breach" (null comparisons are false), so this adapter turns the outcome into {@code BLOCK} with
 * {@code RULE_INPUT_MISSING} and records the gap in the evidence. Premium-capture facts are not
 * gaps at Start Onboarding (BRD §6.2).
 */
@Component
@ConditionalOnProperty(name = "lead.exception-rules", havingValue = "engine", matchIfMissing = true)
public class EngineExceptionRules implements ExceptionRulesPort {

  static final String DOMAIN = "LEAD_EXCEPTION";
  static final String INPUT_MISSING = RuleEngine.RULE_INPUT_MISSING;

  private final RuleSetResolver resolver;
  private final LeadExceptionFactsPort facts;

  public EngineExceptionRules(RuleSetResolver resolver, LeadExceptionFactsPort facts) {
    this.resolver = resolver;
    this.facts = facts;
  }

  @Override
  public Evaluation evaluate(Subject subject) {
    RuleSet ruleSet =
        RuleSetLookup.resolve(
            resolver,
            DOMAIN,
            subject.lob(),
            subject.productClass() == null ? null : subject.productClass().name(),
            subject.asOf());
    Facts known = facts.factsFor(subject);

    ObjectNode inputs = Values.mapper().createObjectNode();
    inputs.put("triggerPoint", subject.checkpoint().name());
    inputs.put("asOf", LocalDate.ofInstant(subject.asOf(), RuleSetLookup.BANK_ZONE).toString());
    for (Fact fact : Fact.values()) {
      if (!known.unknown().contains(fact)) {
        put(inputs, fact.inputName(), known.value(fact));
      }
    }
    EvaluationResult result = RuleEngine.evaluate(ruleSet, inputs, subject.asOf().toString());

    List<String> gaps = new ArrayList<>();
    for (Fact fact : known.unknown()) {
      boolean expectedAbsent =
          fact.premiumCaptureOnly() && subject.checkpoint() == ExceptionCheckpoint.START_ONBOARDING;
      if (!expectedAbsent && !result.missingInputs().contains(fact.inputName())) {
        gaps.add(fact.inputName());
      }
    }
    gaps.sort(String::compareTo);

    ExceptionOutcome outcome = outcomeOf(result.outcome());
    List<String> reasonCodes = new ArrayList<>(result.reasonCodes());
    List<String> messages = new ArrayList<>();
    result.messages().forEach(m -> messages.add(m.rmText()));
    if (!gaps.isEmpty()) {
      outcome = ExceptionOutcome.BLOCK;
      if (!reasonCodes.contains(INPUT_MISSING)) {
        reasonCodes.add(INPUT_MISSING);
      }
      messages.add(missingText(ruleSet, gaps));
    }

    List<FiredRule> fired = new ArrayList<>();
    for (EvaluationResult.Fired f : result.fired()) {
      fired.add(
          new FiredRule(
              f.ruleId(),
              f.outcome(),
              f.reasonCode(),
              strings(f.params() == null ? null : f.params().get("approverRoles")),
              f.configMissing()));
    }

    ObjectNode evidence = (ObjectNode) result.toJson();
    ArrayNode unknownFacts = evidence.putArray("unknownFacts");
    gaps.forEach(unknownFacts::add);
    evidence.put("decidedOutcome", outcome.name());
    return new Evaluation(
        outcome,
        reasonCodes,
        result.ruleSetId(),
        result.version(),
        result.engineVersion(),
        result.inputsHash(),
        fired,
        messages,
        evidence.toString());
  }

  // A rule's outcome is itself configuration (parameters.actions.<ruleId>); anything that is not a
  // known outcome is treated as the most restrictive one, never as PASS.
  private static ExceptionOutcome outcomeOf(String outcome) {
    if ("PASS".equals(outcome)) {
      return ExceptionOutcome.PASS;
    }
    if ("APPROVAL_REQUIRED".equals(outcome)) {
      return ExceptionOutcome.APPROVAL_REQUIRED;
    }
    return ExceptionOutcome.BLOCK;
  }

  private static String missingText(RuleSet ruleSet, List<String> gaps) {
    RuleSet.ReasonCode code = ruleSet.reasonCodes().get(INPUT_MISSING);
    String template = code == null ? "Exception rule data unavailable: @missing." : code.rmText();
    return template.replace("@missing", String.join(", ", gaps));
  }

  private static List<String> strings(Object value) {
    if (!(value instanceof List<?> list)) {
      return List.of();
    }
    return list.stream().filter(String.class::isInstance).map(String.class::cast).toList();
  }

  private static void put(ObjectNode inputs, String name, Object value) {
    if (value == null) {
      return; // known absence (e.g. no lapsed policy): the optional input stays null
    }
    switch (value) {
      case Integer i -> inputs.put(name, i);
      case Boolean b -> inputs.put(name, b);
      case BigDecimal d -> inputs.put(name, d);
      case LocalDate d -> inputs.put(name, d.toString());
      default -> inputs.put(name, value.toString());
    }
  }
}
