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

  @Test
  void allEightSeedsAreListed() {
    assertThat(RuleSetSeeds.all())
        .extracting(s -> RuleSetParser.parse(s.json()).ruleSetId())
        .containsExactlyInAnyOrder(
            "CONSENT.POLICY",
            "HUB.ROUTING",
            "LEAD.EXCEPTION",
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
              if (v instanceof java.util.Map<?, ?> m) {
                assertThat(m.values()).as(k).containsOnlyNulls();
              } else {
                assertThat(v).as(k).isNull();
              }
            });
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
