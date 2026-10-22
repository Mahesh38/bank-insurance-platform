package com.bank.rules.engine;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import java.io.InputStream;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Every seed rule set (ADR-024 section 7) parses, passes its own test cases, conforms to the
 * shipped JSON-Schema, cites its source and carries an unapproved governance block.
 */
class SeedRuleSetsTest {

  // Seeds are added by several work items (ADR-024 section 7, E7): the R0 set must be present and
  // no ruleSetId may be listed twice; further seeds do not break this test.
  @Test
  void theR0SeedsAreListedOnce() {
    assertThat(RuleSetSeeds.all())
        .extracting(s -> RuleSetParser.parse(s.json()).ruleSetId())
        .doesNotHaveDuplicates()
        .contains(
            "CONSENT.POLICY",
            "HUB.ROUTING",
            "LEAD.EXCEPTION",
            "LEAD.POLICY",
            "PROPOSAL.TAT",
            "SUITABILITY.GATE",
            "SUITABILITY.LIFE.SAVINGS",
            "SUITABILITY.LIFE.TERM",
            "SUITABILITY.LIFE.ULIP");
  }

  @Test
  void everySeedPassesEveryTestCase() {
    for (RuleSetSeeds.Seed seed : RuleSetSeeds.all()) {
      TestCaseRunner.Report report = TestCaseRunner.run(RuleSetParser.parse(seed.json()));
      assertThat(report.failed()).as(seed.resource()).isEmpty();
      assertThat(report.cases()).as(seed.resource()).isNotEmpty();
    }
  }

  @Test
  void everySeedConformsToTheJsonSchema() throws Exception {
    JsonSchema schema;
    try (InputStream in =
        getClass().getClassLoader().getResourceAsStream("schema/rule-set-v1.schema.json")) {
      schema = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012).getSchema(in);
    }
    for (RuleSetSeeds.Seed seed : RuleSetSeeds.all()) {
      JsonNode doc = Values.mapper().readTree(seed.json());
      assertThat(schema.validate(doc)).as(seed.resource()).isEmpty();
    }
  }

  @Test
  void schemaRejectsAnUnknownOperatorToo() throws Exception {
    JsonSchema schema;
    try (InputStream in =
        getClass().getClassLoader().getResourceAsStream("schema/rule-set-v1.schema.json")) {
      schema = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012).getSchema(in);
    }
    var doc = Docs.minimal();
    ((com.fasterxml.jackson.databind.node.ObjectNode) doc.at("/rules/0/when")).put("op", "eval");
    assertThat(schema.validate(doc)).isNotEmpty();
    assertThat(RuleSetValidator.validate(doc)).anyMatch(e -> e.contains("unknown operator 'eval'"));
  }

  @Test
  void everySeedCitesItsSourceAndAwaitsContentApproval() throws Exception {
    for (RuleSetSeeds.Seed seed : RuleSetSeeds.all()) {
      JsonNode doc = Values.mapper().readTree(seed.json());
      assertThat(doc.path("source").isMissingNode()).as(seed.resource()).isFalse();
      assertThat(doc.at("/governance/status").asText()).isEqualTo("DRAFT-awaiting-approval");
      assertThat(doc.at("/governance/contentApprovers").isArray()).isTrue();
    }
  }

  @Test
  void leadExceptionShipsWithEveryThresholdAndActionUnset() {
    RuleSet rs =
        RuleSetSeeds.all().stream()
            .filter(s -> s.resource().endsWith("LEAD.EXCEPTION.json"))
            .map(s -> RuleSetParser.parse(s.json()))
            .findFirst()
            .orElseThrow();
    assertThat(rs.rules()).hasSize(15);
    rs.parameters()
        .forEach(
            (k, v) -> {
              if ("approverRoles".equals(k)) {
                return; // E6: the one value the seed sets — asserted below
              }
              if (v instanceof java.util.Map<?, ?> m) {
                assertThat(m.values()).as(k).containsOnlyNulls();
              } else {
                assertThat(v).as(k).isNull();
              }
            });
  }

  /**
   * E6 / E9: every rule names who may approve it, and the strict seed is the bank admin alone — no
   * insurer partner role, no unset entry (an unset entry would block every lead as a configuration
   * gap, never let a partner approve).
   */
  @Test
  void leadExceptionApproverRolesAreBankAdminOnlyAndReachTheEvaluation() {
    RuleSet rs = seed("LEAD.EXCEPTION");
    @SuppressWarnings("unchecked")
    java.util.Map<String, Object> roles =
        (java.util.Map<String, Object>) rs.parameters().get("approverRoles");
    assertThat(roles.keySet())
        .containsExactlyInAnyOrderElementsOf(
            rs.rules().stream().map(RuleSet.Rule::ruleId).toList());
    assertThat(roles.values()).allSatisfy(v -> assertThat(v).isEqualTo(List.of("BANK_ADMIN")));

    EvaluationResult r = RuleEngine.evaluate(rs, rs.testCases().get(0).inputs());
    assertThat(r.fired())
        .allSatisfy(
            f -> assertThat(f.configMissing()).doesNotContain("approverRoles." + f.ruleId()));
  }

  @Test
  void leadPolicyLifeExpiryIsTheOwnersNinetyDays() {
    RuleSet rs = seed("LEAD.POLICY");
    assertThat(rs.domain()).isEqualTo("LEAD_POLICY");
    assertThat(rs.scope().lob()).isEqualTo("LIFE");
    EvaluationResult r =
        RuleEngine.evaluate(rs, Values.mapper().createObjectNode().put("lob", "LIFE"));
    assertThat(r.outcome()).isEqualTo("RESOLVED");
    assertThat(r.derived().get("expiryDays").toString()).isEqualTo("90");

    // Unset or non-positive expiry fails closed: lead-service refuses the create.
    for (var bad :
        List.of(Values.mapper().nullNode(), Values.mapper().getNodeFactory().numberNode(0))) {
      var doc = (com.fasterxml.jackson.databind.node.ObjectNode) rs.document().deepCopy();
      ((com.fasterxml.jackson.databind.node.ObjectNode) doc.get("parameters"))
          .set("expiryDays", bad);
      EvaluationResult broken =
          RuleEngine.evaluate(
              RuleSetParser.parse(doc), Values.mapper().createObjectNode().put("lob", "LIFE"));
      assertThat(broken.outcome()).as(bad.toString()).isEqualTo("CONFIG_MISSING");
    }
  }

  private static RuleSet seed(String ruleSetId) {
    return RuleSetSeeds.all().stream()
        .map(s -> RuleSetParser.parse(s.json()))
        .filter(r -> r.ruleSetId().equals(ruleSetId))
        .findFirst()
        .orElseThrow();
  }

  @Test
  void suitabilityReferenceCaseMatchesTheWorkedExample() {
    RuleSet term =
        RuleSetSeeds.all().stream()
            .map(s -> RuleSetParser.parse(s.json()))
            .filter(r -> r.ruleSetId().equals("SUITABILITY.LIFE.TERM"))
            .findFirst()
            .orElseThrow();
    RuleSet.TestCase ref =
        term.testCases().stream()
            .filter(t -> t.name().equals("SUIT-TC-REF-01"))
            .findFirst()
            .orElseThrow();
    EvaluationResult r = RuleEngine.evaluate(term, ref.inputs());
    assertThat(r.derived().get("recommendedCover").toString()).isEqualTo("25600000");
    assertThat(r.outcome()).isEqualTo("SUITABLE");
    assertThat(List.of(term.pinning())).containsExactly(RuleSet.Pinning.JOURNEY);
  }
}
