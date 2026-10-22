package com.bank.rules.engine;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** ADR-024 fitness function / SUIT-R05: same inputs and version twice give identical bytes. */
class RuleEngineDeterminismTest {

  @Test
  void sameInputsTwiceGiveIdenticalBytesForEverySeedCase() {
    for (RuleSetSeeds.Seed seed : RuleSetSeeds.all()) {
      RuleSet first = RuleSetParser.parse(seed.json());
      RuleSet second = RuleSetParser.parse(seed.json());
      assertThat(first.documentSha256()).isEqualTo(second.documentSha256());
      for (RuleSet.TestCase tc : first.testCases()) {
        String a =
            RuleEngine.evaluate(first, tc.inputs(), "2026-11-01T00:00:00Z").toCanonicalJson();
        String b =
            RuleEngine.evaluate(second, tc.inputs().deepCopy(), "2026-11-01T00:00:00Z")
                .toCanonicalJson();
        assertThat(a).as(seed.resource() + " " + tc.name()).isEqualTo(b);
      }
    }
  }

  @Test
  void inputsHashIgnoresKeyOrderAndNumberFormatting() {
    RuleSet rs = Docs.parse(Docs.minimal());
    String h1 = RuleEngine.evaluate(rs, Docs.json("{'x':5.0,'d':'2026-01-01'}")).inputsHash();
    String h2 = RuleEngine.evaluate(rs, Docs.json("{'d':'2026-01-01','x':5}")).inputsHash();
    assertThat(h1).isEqualTo(h2).startsWith("sha256:");
  }

  @Test
  void documentHashIgnoresFormatting() {
    String compact = Docs.minimal().toString();
    String pretty = Docs.minimal().toPrettyString();
    assertThat(RuleSetParser.parse(compact).documentSha256())
        .isEqualTo(RuleSetParser.parse(pretty).documentSha256());
  }
}
