package com.bank.rules.engine;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.InputStream;
import org.junit.jupiter.api.Test;

/**
 * DECISIONS D2, configured behaviour: the shipped LEAD.EXCEPTION seed overlaid with SAMPLE values
 * from {@code src/test/resources}. The values are invented for the test only and are not the BRD
 * owner's thresholds.
 */
class LeadExceptionSampleTest {

  @Test
  void sampleConfiguredCopyPassesItsCases() throws Exception {
    ObjectNode seed =
        (ObjectNode)
            Values.mapper()
                .readTree(
                    RuleSetSeeds.all().stream()
                        .filter(s -> s.resource().endsWith("LEAD.EXCEPTION.json"))
                        .findFirst()
                        .orElseThrow()
                        .json());
    JsonNode overlay;
    try (InputStream in =
        getClass()
            .getClassLoader()
            .getResourceAsStream("rulesets/LEAD.EXCEPTION.SAMPLE-overlay.json")) {
      overlay = Values.mapper().readTree(in);
    }
    assertThat(overlay.path("_SAMPLE").asText()).contains("SAMPLE");
    seed.put("version", overlay.get("version").asText());
    seed.set("parameters", overlay.get("parameters"));
    seed.set("testCases", overlay.get("testCases"));

    RuleSet sample = RuleSetParser.parse(seed);
    TestCaseRunner.Report report = TestCaseRunner.run(sample);

    assertThat(report.failed()).isEmpty();
    assertThat(sample.testCases()).allMatch(RuleSet.TestCase::sample);

    // E6: the evaluation evidence carries who may approve each fired rule, so the approval
    // endpoint checks the roles of the version that decided, not of whatever is in force later.
    RuleSet.TestCase age =
        sample.testCases().stream()
            .filter(t -> t.name().equals("SAMPLE-age-routes-to-approval"))
            .findFirst()
            .orElseThrow();
    EvaluationResult r = RuleEngine.evaluate(sample, age.inputs());
    assertThat(r.fired())
        .singleElement()
        .satisfies(
            f ->
                assertThat(f.params().get("approverRoles"))
                    .isEqualTo(java.util.List.of("BANK_ADMIN", "BRANCH_MANAGER")));
  }
}
