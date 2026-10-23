package com.bank.rules.engine;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Hit policies, missing input, missing configuration and message rendering (ADR-024 4 and 6). */
class EvaluationSemanticsTest {

  private static final String RULES =
      "{'a':[{'ruleId':'LOW','priority':1,'when':{'op':'gt','args':[{'var':'x'},{'const':1}]},"
          + "'then':{'outcome':'OK','reasonCode':'A','actions':['NOTE']}},"
          + "{'ruleId':'HIGH','priority':2,'when':{'op':'gt','args':[{'var':'x'},{'param':'limit'}]},"
          + "'then':{'outcome':'BAD','reasonCode':'TOO_BIG','params':{'lim':{'param':'limit'}}}}]}";

  private static ObjectNode twoRules(String hitPolicy, String collect) {
    ObjectNode doc = Docs.minimal();
    doc.set("rules", Docs.json(RULES).get("a"));
    ((ObjectNode) doc.get("reasonCodes"))
        .set("A", Docs.json("{'severity':'INFO','rmText':'a fired'}"));
    ObjectNode d = (ObjectNode) doc.get("decision");
    d.put("hitPolicy", hitPolicy);
    d.put("collectReasonCodes", collect);
    return doc;
  }

  @Test
  void priorityPicksMostRestrictiveAndCollectsAllReasonCodesInRuleOrder() {
    EvaluationResult r =
        RuleEngine.evaluate(Docs.parse(twoRules("PRIORITY", "ALL_FIRED")), Docs.json("{'x':11}"));
    assertThat(r.outcome()).isEqualTo("BAD");
    assertThat(r.reasonCodes()).containsExactly("A", "TOO_BIG");
    assertThat(r.messages())
        .extracting(EvaluationResult.Message::rmText)
        .containsExactly("a fired", "x above 10");
    assertThat(r.fired().get(0).actions()).containsExactly("NOTE");
  }

  @Test
  void winningOnlyKeepsOnlyTheWinnersReasonCode() {
    EvaluationResult r =
        RuleEngine.evaluate(
            Docs.parse(twoRules("PRIORITY", "WINNING_ONLY")), Docs.json("{'x':11}"));
    assertThat(r.reasonCodes()).containsExactly("TOO_BIG");
  }

  @Test
  void firstStopsAtTheFirstMatchByPriority() {
    EvaluationResult r =
        RuleEngine.evaluate(Docs.parse(twoRules("FIRST", "WINNING_ONLY")), Docs.json("{'x':11}"));
    assertThat(r.outcome()).isEqualTo("OK");
    assertThat(r.fired()).extracting(EvaluationResult.Fired::ruleId).containsExactly("LOW");
  }

  @Test
  void collectReturnsEveryMatchAndTheHighestRankedSummary() {
    EvaluationResult r =
        RuleEngine.evaluate(Docs.parse(twoRules("COLLECT", "ALL_FIRED")), Docs.json("{'x':11}"));
    assertThat(r.fired()).hasSize(2);
    assertThat(r.outcome()).isEqualTo("BAD");
    ObjectNode noRank = twoRules("COLLECT", "ALL_FIRED");
    ((ObjectNode) noRank.get("decision")).remove("outcomeRank");
    assertThat(RuleEngine.evaluate(Docs.parse(noRank), Docs.json("{'x':11}")).outcome()).isNull();
  }

  @Test
  void noMatchGivesTheConfiguredOutcome() {
    EvaluationResult r = RuleEngine.evaluate(Docs.parse(Docs.minimal()), Docs.json("{'x':1}"));
    assertThat(r.outcome()).isEqualTo("OK");
    assertThat(r.fired()).isEmpty();
    assertThat(r.reasonCodes()).isEmpty();
  }

  @Test
  void missingRequiredInputGivesMissingInputOutcome() {
    EvaluationResult r = RuleEngine.evaluate(Docs.parse(Docs.minimal()), Docs.json("{}"));
    assertThat(r.outcome()).isEqualTo("BAD");
    assertThat(r.reasonCodes()).containsExactly(RuleEngine.RULE_INPUT_MISSING);
    assertThat(r.missingInputs()).containsExactly("x");
    assertThat(r.messages().get(0).rmText()).isEqualTo("need x");
    assertThat(RuleEngine.evaluate(Docs.parse(Docs.minimal()), null).outcome()).isEqualTo("BAD");
  }

  @Test
  void wrongTypedInputIsRefusedAsInvalid() {
    RuleSet rs = Docs.parse(Docs.minimal());
    EvaluationResult r = RuleEngine.evaluate(rs, Docs.json("{'x':'ten'}"));
    assertThat(r.reasonCodes()).containsExactly(RuleEngine.RULE_INPUT_INVALID);
    assertThat(RuleEngine.evaluate(rs, Docs.json("{'x':1,'d':'not-a-date'}")).missingInputs())
        .containsExactly("d");
  }

  @Test
  void coercesEveryInputType() {
    RuleSet.InputDef e =
        new RuleSet.InputDef("e", RuleSet.InputType.ENUM, true, java.util.List.of("A"));
    assertThat(RuleEngine.coerce(e, Docs.json("{'v':'A'}").get("v"))).isEqualTo("A");
    assertThat(RuleEngine.coerce(e, Docs.json("{'v':'B'}").get("v"))).isNull();
    var i = new RuleSet.InputDef("i", RuleSet.InputType.INTEGER, true, java.util.List.of());
    assertThat(RuleEngine.coerce(i, Docs.json("{'v':3.0}").get("v"))).isNotNull();
    assertThat(RuleEngine.coerce(i, Docs.json("{'v':3.5}").get("v"))).isNull();
    var b = new RuleSet.InputDef("b", RuleSet.InputType.BOOLEAN, true, java.util.List.of());
    assertThat(RuleEngine.coerce(b, Docs.json("{'v':'true'}").get("v"))).isNull();
    var s = new RuleSet.InputDef("s", RuleSet.InputType.STRING, true, java.util.List.of());
    assertThat(RuleEngine.coerce(s, Docs.json("{'v':1}").get("v"))).isNull();
    var l = new RuleSet.InputDef("l", RuleSet.InputType.LIST, true, java.util.List.of());
    assertThat(RuleEngine.coerce(l, Docs.json("{'v':[1]}").get("v"))).isNotNull();
    assertThat(RuleEngine.coerce(l, Docs.json("{'v':1}").get("v"))).isNull();
  }

  @Test
  void aRuleReadingAnUnsetParameterProducesTheMissingConfigOutcome() {
    ObjectNode doc = Docs.minimal();
    ((ObjectNode) doc.get("parameters")).putNull("limit");
    EvaluationResult r = RuleEngine.evaluate(Docs.parse(doc), Docs.json("{'x':1}"));
    assertThat(r.outcome()).isEqualTo("BAD");
    assertThat(r.reasonCodes()).containsExactly("CFG");
    assertThat(r.fired().get(0).configMissing()).containsExactly("limit");
    assertThat(r.messages().get(0).rmText()).isEqualTo("missing limit");
  }

  @Test
  void configuredOutcomeComesFromAParameter() {
    ObjectNode doc = Docs.minimal();
    ((ObjectNode) doc.at("/rules/0/then")).set("outcome", Docs.json("{'param':'act'}"));
    ((ObjectNode) doc.get("parameters")).put("act", "OK");
    EvaluationResult r = RuleEngine.evaluate(Docs.parse(doc), Docs.json("{'x':11}"));
    assertThat(r.outcome()).isEqualTo("OK");
  }

  @Test
  void rendersPlaceholders() {
    assertThat(RuleEngine.render("@a and @b and @a", Map.of("a", new java.math.BigDecimal("1.50"))))
        .isEqualTo("1.5 and @b and 1.5");
    assertThat(RuleEngine.render("@l", Map.of("l", java.util.List.of("A", "B")))).isEqualTo("A, B");
    assertThat(Values.render(null)).isEmpty();
  }

  @Test
  void resultJsonCarriesTheEvidenceFields() {
    EvaluationResult r =
        RuleEngine.evaluate(
            Docs.parse(Docs.minimal()), Docs.json("{'x':11}"), "2026-11-01T00:00:00Z");
    var json = r.toJson();
    assertThat(json.get("engineVersion").asText()).isEqualTo(RuleEngine.ENGINE_VERSION);
    assertThat(json.get("evaluatedAt").asText()).isEqualTo("2026-11-01T00:00:00Z");
    assertThat(json.at("/derived/twice").decimalValue()).isEqualByComparingTo("22");
    assertThat(json.at("/fired/0/ruleId").asText()).isEqualTo("R1");
  }

  @Test
  void testCaseRunnerReportsEachKindOfMismatch() {
    ObjectNode doc = Docs.minimal();
    doc.set(
        "testCases",
        Docs.json(
                "{'a':[{'name':'bad','inputs':{'x':11},'expect':{'outcome':'OK','reasonCodes':[],"
                    + "'fired':[],'derived':{'twice':1,'nope':null},'messages':['zz']}},"
                    + "{'name':'nulls','inputs':{'x':1},'expect':{'outcome':'OK','derived':{'twice':2,'x':'1'}}}]}")
            .get("a"));
    TestCaseRunner.Report report = TestCaseRunner.run(Docs.parse(doc));
    assertThat(report.allPassed()).isFalse();
    assertThat(report.failed()).hasSize(2);
    assertThat(report.failed().get(0).failures()).hasSize(5);
  }
}
