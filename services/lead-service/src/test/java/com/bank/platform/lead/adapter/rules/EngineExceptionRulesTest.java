package com.bank.platform.lead.adapter.rules;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bank.platform.lead.domain.ExceptionCheckpoint;
import com.bank.platform.lead.domain.ExceptionOutcome;
import com.bank.platform.lead.domain.ExceptionRulesPort.Evaluation;
import com.bank.platform.lead.domain.ExceptionRulesPort.FiredRule;
import com.bank.platform.lead.domain.ExceptionRulesPort.Subject;
import com.bank.platform.lead.domain.LeadExceptionFactsPort.Fact;
import com.bank.platform.lead.domain.LeadExceptionFactsPort.Facts;
import com.bank.platform.lead.domain.ProductClass;
import com.bank.platform.lead.domain.RuleConfigurationException;
import com.bank.rules.client.RuleSetResolver;
import com.bank.rules.client.RuleSetUnresolvableException;
import com.bank.rules.engine.RuleSet;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.ResourceAccessException;

/** D2 / E6 / E9 on the real engine: seed behaviour, SAMPLE-configured behaviour, unknown facts. */
class EngineExceptionRulesTest {

  private static final Instant NOW = Instant.parse("2026-11-15T06:00:00Z");

  private static Subject subject(ExceptionCheckpoint checkpoint) {
    return new Subject(
        "01JQX4K7R8M2N3P4Q5S6T7V8W9",
        "cust-1",
        "LIFE",
        ProductClass.TERM,
        "BR-1",
        "rm-1",
        checkpoint,
        NOW);
  }

  private static Evaluation evaluate(RuleSet rs, Facts facts) {
    return new EngineExceptionRules(SampleRuleSets.resolverOf(rs), s -> facts)
        .evaluate(subject(ExceptionCheckpoint.START_ONBOARDING));
  }

  @Test
  void unknownFactsBlockEveryLead() {
    Evaluation e =
        new EngineExceptionRules(
                new ClasspathSeedRuleSetResolver(), new UnknownLeadExceptionFacts())
            .evaluate(subject(ExceptionCheckpoint.START_ONBOARDING));

    assertThat(e.outcome()).isEqualTo(ExceptionOutcome.BLOCK);
    assertThat(e.reasonCodes()).containsExactly("RULE_INPUT_MISSING");
    assertThat(e.ruleSetRef()).isEqualTo("LEAD.EXCEPTION@1.1.0");
    assertThat(e.inputsHash()).startsWith("sha256:");
    assertThat(e.evidenceJson()).contains("\"decidedOutcome\":\"BLOCK\"");
  }

  @Test
  void seedWithUnsetThresholdsBlocksAsAConfigurationGap() {
    Evaluation e = evaluate(SampleRuleSets.seed("LEAD.EXCEPTION"), SampleRuleSets.cleanFacts(40));

    assertThat(e.outcome()).isEqualTo(ExceptionOutcome.BLOCK);
    assertThat(e.reasonCodes()).containsExactly("EH-CONFIG-MISSING");
    assertThat(e.fired()).hasSize(15);
    // approverRoles are set in the seed, so they are never the missing configuration (E6).
    assertThat(e.fired())
        .allSatisfy(
            f -> assertThat(f.configMissing()).noneMatch(p -> p.startsWith("approverRoles")));
  }

  @Test
  void configuredBreachRoutesToApprovalWithItsApproverRoles() {
    Evaluation e =
        evaluate(SampleRuleSets.configuredLeadException(), SampleRuleSets.cleanFacts(61));

    assertThat(e.outcome()).isEqualTo(ExceptionOutcome.APPROVAL_REQUIRED);
    assertThat(e.reasonCodes()).containsExactly("EH-ERR-RULE-001");
    assertThat(e.messages())
        .containsExactly("The Proposer's age exceeds the maximum limit of 60 years.");
    assertThat(e.fired())
        .singleElement()
        .extracting(FiredRule::approverRoles)
        .isEqualTo(java.util.List.of("BANK_ADMIN"));
  }

  @Test
  void cleanCustomerPassesAndBlockedCustomerBlocks() {
    RuleSet rs = SampleRuleSets.configuredLeadException();
    assertThat(evaluate(rs, SampleRuleSets.cleanFacts(40)).outcome())
        .isEqualTo(ExceptionOutcome.PASS);
    assertThat(evaluate(rs, SampleRuleSets.blockedCustomer()).outcome())
        .isEqualTo(ExceptionOutcome.BLOCK);
  }

  @Test
  void anUnknownOptionalFactIsNeverReadAsNoBreach() {
    Facts clean = SampleRuleSets.cleanFacts(40);
    Facts lapseUnknown =
        new Facts(
            40,
            false,
            760,
            null,
            null,
            null,
            1,
            clean.casaOpenedOn(),
            null,
            true,
            false,
            null,
            null,
            null,
            false,
            false,
            Set.of(Fact.LAST_LAPSED_POLICY_CATEGORY));

    Evaluation e = evaluate(SampleRuleSets.configuredLeadException(), lapseUnknown);

    assertThat(e.outcome()).isEqualTo(ExceptionOutcome.BLOCK);
    assertThat(e.reasonCodes()).contains("RULE_INPUT_MISSING");
    assertThat(e.messages()).anyMatch(m -> m.contains("lastLapsedPolicyCategory"));
  }

  @Test
  void premiumIsNotAGapAtStartOnboarding() {
    Facts premiumUnknown =
        new Facts(
            40,
            false,
            760,
            null,
            null,
            null,
            1,
            SampleRuleSets.cleanFacts(40).casaOpenedOn(),
            null,
            true,
            false,
            null,
            null,
            null,
            false,
            false,
            Set.of(Fact.ANNUAL_PREMIUM, Fact.OLDEST_UNPAID_RENEWAL_DUE_ON));

    assertThat(evaluate(SampleRuleSets.configuredLeadException(), premiumUnknown).outcome())
        .isEqualTo(ExceptionOutcome.PASS);
    assertThat(
            new EngineExceptionRules(
                    SampleRuleSets.resolverOf(SampleRuleSets.configuredLeadException()),
                    s -> premiumUnknown)
                .evaluate(subject(ExceptionCheckpoint.PREMIUM_CAPTURE))
                .outcome())
        .isEqualTo(ExceptionOutcome.BLOCK);
  }

  @Test
  void unresolvableRuleSetFailsClosedAndTellsUnavailableFromAbsent() {
    RuleSetResolver down =
        failing(
            new RuleSetUnresolvableException(
                "timeout", new ResourceAccessException("connect timed out")));
    RuleSetResolver absent = failing(new RuleSetUnresolvableException("no version in force"));

    assertThatThrownBy(
            () ->
                new EngineExceptionRules(down, new UnknownLeadExceptionFacts())
                    .evaluate(subject(ExceptionCheckpoint.START_ONBOARDING)))
        .isInstanceOfSatisfying(
            RuleConfigurationException.class, ex -> assertThat(ex.upstreamUnavailable()).isTrue());
    assertThatThrownBy(
            () ->
                new EngineExceptionRules(absent, new UnknownLeadExceptionFacts())
                    .evaluate(subject(ExceptionCheckpoint.START_ONBOARDING)))
        .isInstanceOfSatisfying(
            RuleConfigurationException.class, ex -> assertThat(ex.upstreamUnavailable()).isFalse());
  }

  @Test
  void leadPolicyGivesTheLifeExpiryAndRefusesAnUnknownLob() {
    RuleSetLeadPolicy policy = new RuleSetLeadPolicy(new ClasspathSeedRuleSetResolver());

    assertThat(policy.expiryFor("LIFE", NOW)).isEqualTo(java.time.Duration.ofDays(90));
    assertThatThrownBy(() -> policy.expiryFor("MOTOR", NOW))
        .isInstanceOf(RuleConfigurationException.class);
  }

  private static RuleSetResolver failing(RuleSetUnresolvableException ex) {
    return new RuleSetResolver() {
      @Override
      public RuleSet resolve(com.bank.rules.client.RuleSetQuery query) {
        throw ex;
      }

      @Override
      public RuleSet version(String ruleSetId, String version) {
        throw ex;
      }
    };
  }
}
