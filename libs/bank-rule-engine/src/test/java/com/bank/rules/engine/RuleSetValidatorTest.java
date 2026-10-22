package com.bank.rules.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

class RuleSetValidatorTest {

  private static List<String> errorsAfter(Consumer<ObjectNode> change) {
    ObjectNode doc = Docs.minimal();
    change.accept(doc);
    return RuleSetValidator.validate(doc);
  }

  private static ObjectNode when(ObjectNode doc) {
    return (ObjectNode) doc.at("/rules/0/when");
  }

  @Test
  void minimalDocumentIsValid() {
    assertThat(RuleSetValidator.validate(Docs.minimal())).isEmpty();
  }

  @Test
  void rejectsUnknownOperator() {
    assertThat(errorsAfter(d -> when(d).put("op", "eval")))
        .anyMatch(e -> e.contains("unknown operator 'eval'"));
  }

  @Test
  void rejectsWrongArity() {
    assertThat(errorsAfter(d -> ((ArrayNode) when(d).get("args")).add(Docs.json("{'const':1}"))))
        .anyMatch(e -> e.contains("'gt' takes 2 arguments"));
  }

  @Test
  void rejectsUnknownVarParamAndTable() {
    assertThat(
            errorsAfter(
                d ->
                    when(d)
                        .set(
                            "args",
                            Docs.json("{'a':[{'var':'y'},{'param':'nope.deep'}]}").get("a"))))
        .anyMatch(e -> e.contains("unknown var 'y'"))
        .anyMatch(e -> e.contains("unknown param 'nope.deep'"));
    assertThat(
            errorsAfter(
                d ->
                    d.set(
                        "derive",
                        Docs.json(
                                "{'a':[{'name':'z','expr':{'op':'lookup','args':[{'const':'t'},{'var':'x'}]}}]}")
                            .get("a"))))
        .anyMatch(e -> e.contains("unknown table 't'"));
    assertThat(
            errorsAfter(
                d ->
                    d.set(
                        "derive",
                        Docs.json(
                                "{'a':[{'name':'z','expr':{'op':'lookup','args':[{'var':'x'},{'var':'x'}]}}]}")
                            .get("a"))))
        .anyMatch(e -> e.contains("lookup table must be"));
  }

  @Test
  void rejectsDeriveCycle() {
    assertThat(
            errorsAfter(
                d ->
                    d.set(
                        "derive",
                        Docs.json(
                                "{'a':[{'name':'p','expr':{'op':'add','args':[{'var':'q'},{'const':1}]}},"
                                    + "{'name':'q','expr':{'op':'add','args':[{'var':'p'},{'const':1}]}}]}")
                            .get("a"))))
        .anyMatch(e -> e.contains("cycle"));
  }

  @Test
  void rejectsUndefinedReasonCodeAndOutcome() {
    assertThat(errorsAfter(d -> ((ObjectNode) d.at("/rules/0/then")).put("reasonCode", "NOPE")))
        .anyMatch(e -> e.contains("'NOPE' is not defined"));
    assertThat(errorsAfter(d -> ((ObjectNode) d.at("/rules/0/then")).put("outcome", "MAYBE")))
        .anyMatch(e -> e.contains("'MAYBE' is not in outcomeRank"));
    assertThat(
            errorsAfter(d -> ((ObjectNode) d.get("decision")).put("missingConfigReasonCode", "X")))
        .anyMatch(e -> e.contains("reason code 'X' is not defined"));
    assertThat(errorsAfter(d -> ((ObjectNode) d.get("decision")).put("noMatchOutcome", "ZZ")))
        .anyMatch(e -> e.contains("'ZZ' is not in outcomeRank"));
  }

  @Test
  void rejectsStructuralProblems() {
    assertThat(errorsAfter(d -> d.put("format", "rule-set/v2")))
        .anyMatch(e -> e.startsWith("/format"));
    assertThat(errorsAfter(d -> d.put("ruleSetId", "lower.case")))
        .anyMatch(e -> e.startsWith("/ruleSetId"));
    assertThat(errorsAfter(d -> d.put("version", "1.0"))).anyMatch(e -> e.startsWith("/version"));
    assertThat(errorsAfter(d -> d.put("classification", "WHATEVER")))
        .anyMatch(e -> e.startsWith("/classification"));
    assertThat(errorsAfter(d -> d.put("surprise", 1)))
        .anyMatch(e -> e.contains("/surprise: unknown field"));
    assertThat(errorsAfter(d -> d.put("effectiveFrom", "tomorrow")))
        .anyMatch(e -> e.startsWith("/effectiveFrom"));
    assertThat(errorsAfter(d -> d.put("effectiveTo", "2026-01-01T00:00:00Z")))
        .anyMatch(e -> e.startsWith("/effectiveTo"));
    assertThat(errorsAfter(d -> d.put("scale", -1))).anyMatch(e -> e.startsWith("/scale"));
    assertThat(errorsAfter(d -> d.remove("scope"))).anyMatch(e -> e.startsWith("/scope"));
    assertThat(errorsAfter(d -> d.remove("testCases"))).anyMatch(e -> e.startsWith("/testCases"));
    assertThat(errorsAfter(d -> d.set("testCases", Docs.json("{'a':[]}").get("a"))))
        .anyMatch(e -> e.startsWith("/testCases"));
    assertThat(errorsAfter(d -> d.set("rules", Docs.json("{'a':[]}").get("a"))))
        .anyMatch(e -> e.startsWith("/rules"));
    assertThat(
            errorsAfter(
                d ->
                    d.set(
                        "inputs",
                        Docs.json("{'a':[{'name':'e','type':'ENUM'},{'name':'e','type':'NOPE'}]}")
                            .get("a"))))
        .anyMatch(e -> e.contains("ENUM input needs"))
        .anyMatch(e -> e.contains("duplicate 'e'"))
        .anyMatch(e -> e.contains("is not one of"));
    assertThat(errorsAfter(d -> ((ObjectNode) d.get("decision")).remove("outcomeRank")))
        .anyMatch(e -> e.contains("PRIORITY needs"));
    assertThat(errorsAfter(d -> ((ObjectNode) d.get("decision")).remove("missingConfigOutcome")))
        .anyMatch(e -> e.contains("missingConfigOutcome"));
    assertThat(errorsAfter(d -> ((ObjectNode) d.at("/rules/0")).put("priority", "high")))
        .anyMatch(e -> e.contains("/priority"));
    assertThat(errorsAfter(d -> ((ObjectNode) d.at("/reasonCodes/TOO_BIG")).put("severity", "MEH")))
        .anyMatch(e -> e.contains("/severity"));
    assertThat(RuleSetValidator.validate(Docs.json("{'a':1}").get("a"))).hasSize(1);
  }

  @Test
  void rejectsBadTables() {
    assertThat(
            errorsAfter(
                d ->
                    d.set(
                        "tables",
                        Docs.json(
                            "{'r':{'type':'RANGE','rows':[{'from':5,'to':1,'value':1},{'to':'x'}]},"
                                + "'e':{'type':'EXACT','rows':[{'value':1}]},'n':{'type':'RANGE','rows':[]}}"))))
        .anyMatch(e -> e.contains("'to' must be greater"))
        .anyMatch(e -> e.contains("/from: required number"))
        .anyMatch(e -> e.contains("/key: required for EXACT"))
        .anyMatch(e -> e.contains("/tables/n/rows"));
  }

  @Test
  void parserRefusesInvalidDocumentsAndNonJson() {
    ObjectNode bad = Docs.minimal();
    bad.put("format", "x");
    assertThatThrownBy(() -> RuleSetParser.parse(bad))
        .isInstanceOf(RuleSetValidationException.class)
        .satisfies(e -> assertThat(((RuleSetValidationException) e).errors()).isNotEmpty());
    assertThatThrownBy(() -> RuleSetParser.parse("{not json"))
        .isInstanceOf(RuleSetValidationException.class);
  }

  @Test
  void parsesModelFields() {
    RuleSet rs = Docs.parse(Docs.minimal());
    assertThat(rs.scope().lob()).isEqualTo("LIFE");
    assertThat(rs.scope().productClass()).isNull();
    assertThat(rs.effectiveTo()).isNull();
    assertThat(rs.pinning()).isEqualTo(RuleSet.Pinning.NONE);
    assertThat(rs.classification().approverRole()).isEqualTo("CONFIG_APPROVER");
    assertThat(rs.input("x")).isPresent();
    assertThat(rs.input("nope")).isEmpty();
    assertThat(rs.decision().rank("OK")).isEqualTo(1);
    assertThat(rs.decision().rank("??")).isEqualTo(Integer.MAX_VALUE);
    assertThat(Operator.fromJson("roundUpTo")).contains(Operator.ROUND_UP_TO);
  }
}
