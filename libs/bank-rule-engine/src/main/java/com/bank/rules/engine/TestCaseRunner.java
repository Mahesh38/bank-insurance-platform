package com.bank.rules.engine;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Runs a rule set's own {@code testCases} (ADR-024 section 5 point 3). The configuration service
 * refuses to publish a version unless {@link Report#allPassed()} holds; the engine's own build runs
 * every seed through this as well, so a seed cannot ship with a failing case.
 *
 * <p>Expectations are partial: only the keys present in {@code expect} are checked. {@code
 * reasonCodes} and {@code fired} (rule ids) compare as exact ordered lists; {@code derived}
 * compares numbers by value ({@code 25600000} equals {@code 25600000.00}).
 */
public final class TestCaseRunner {

  private TestCaseRunner() {}

  public record CaseResult(String name, boolean passed, List<String> failures, JsonNode actual) {}

  public record Report(String ruleSetId, String version, List<CaseResult> cases) {
    public boolean allPassed() {
      return cases.stream().allMatch(CaseResult::passed);
    }

    public List<CaseResult> failed() {
      return cases.stream().filter(c -> !c.passed()).toList();
    }
  }

  public static Report run(RuleSet rs) {
    List<CaseResult> out = new ArrayList<>();
    for (RuleSet.TestCase tc : rs.testCases()) {
      EvaluationResult result = RuleEngine.evaluate(rs, tc.inputs(), tc.evaluatedAt());
      out.add(check(tc, result));
    }
    return new Report(rs.ruleSetId(), rs.version(), List.copyOf(out));
  }

  static CaseResult check(RuleSet.TestCase tc, EvaluationResult result) {
    JsonNode actual = result.toJson();
    JsonNode expect = tc.expect();
    List<String> failures = new ArrayList<>();
    if (expect.has("outcome")) {
      String want = expect.get("outcome").isNull() ? null : expect.get("outcome").asText();
      if (!java.util.Objects.equals(want, result.outcome())) {
        failures.add("outcome: expected " + want + " but was " + result.outcome());
      }
    }
    if (expect.has("reasonCodes")) {
      List<String> want = texts(expect.get("reasonCodes"));
      if (!want.equals(result.reasonCodes())) {
        failures.add("reasonCodes: expected " + want + " but was " + result.reasonCodes());
      }
    }
    if (expect.has("fired")) {
      List<String> want = texts(expect.get("fired"));
      List<String> got = result.fired().stream().map(EvaluationResult.Fired::ruleId).toList();
      if (!want.equals(got)) {
        failures.add("fired: expected " + want + " but was " + got);
      }
    }
    if (expect.has("derived")) {
      Iterator<Map.Entry<String, JsonNode>> it = expect.get("derived").fields();
      while (it.hasNext()) {
        Map.Entry<String, JsonNode> e = it.next();
        Object got = result.derived().get(e.getKey());
        if (!sameValue(e.getValue(), got)) {
          failures.add("derived." + e.getKey() + ": expected " + e.getValue() + " but was " + got);
        }
      }
    }
    if (expect.has("messages")) {
      List<String> want = texts(expect.get("messages"));
      List<String> got = result.messages().stream().map(EvaluationResult.Message::rmText).toList();
      if (!want.equals(got)) {
        failures.add("messages: expected " + want + " but was " + got);
      }
    }
    return new CaseResult(tc.name(), failures.isEmpty(), List.copyOf(failures), actual);
  }

  private static boolean sameValue(JsonNode want, Object got) {
    if (want.isNull()) {
      return got == null;
    }
    if (want.isNumber()) {
      return got instanceof BigDecimal d && d.compareTo(want.decimalValue()) == 0;
    }
    return got != null && Values.canonical(want).equals(Values.toJson(got));
  }

  private static List<String> texts(JsonNode array) {
    List<String> out = new ArrayList<>();
    array.forEach(n -> out.add(n.asText()));
    return out;
  }
}
