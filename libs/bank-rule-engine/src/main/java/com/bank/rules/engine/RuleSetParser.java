package com.bank.rules.engine;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Turns a rule-set/v1 document into a {@link RuleSet}. Validation runs first and any finding
 * refuses the whole document: there is no partially loaded rule set (fail closed).
 */
public final class RuleSetParser {

  /** Arithmetic scale for {@code div} when the document declares none. */
  public static final int DEFAULT_SCALE = 6;

  private RuleSetParser() {}

  public static RuleSet parse(String json) {
    try {
      return parse(Values.mapper().readTree(json));
    } catch (JsonProcessingException e) {
      throw new RuleSetValidationException(List.of("/: not JSON (" + e.getOriginalMessage() + ")"));
    }
  }

  public static RuleSet parse(JsonNode doc) {
    List<String> errors = RuleSetValidator.validate(doc);
    if (!errors.isEmpty()) {
      throw new RuleSetValidationException(errors);
    }
    JsonNode scope = doc.path("scope");
    @SuppressWarnings("unchecked")
    Map<String, Object> parameters =
        doc.has("parameters")
            ? (Map<String, Object>) Values.fromJson(doc.get("parameters"))
            : Map.of();
    return new RuleSet(
        doc.get("ruleSetId").asText(),
        doc.get("domain").asText(),
        new RuleSet.Scope(scope.path("lob").asText(null), scope.path("productClass").asText(null)),
        doc.get("version").asText(),
        OffsetDateTime.parse(doc.get("effectiveFrom").asText()),
        doc.path("effectiveTo").isTextual()
            ? OffsetDateTime.parse(doc.get("effectiveTo").asText())
            : null,
        RuleSet.Classification.valueOf(doc.get("classification").asText()),
        RuleSet.Pinning.valueOf(doc.path("pinning").asText("NONE")),
        doc.path("scale").asInt(DEFAULT_SCALE),
        inputs(doc.get("inputs")),
        tables(doc.path("tables")),
        derive(doc.path("derive")),
        decision(doc.get("decision")),
        rules(doc.get("rules")),
        Collections.unmodifiableMap(parameters),
        reasonCodes(doc.get("reasonCodes")),
        testCases(doc.get("testCases")),
        doc.deepCopy(),
        Values.sha256(Values.canonicalString(doc)));
  }

  static Expr expr(JsonNode node) {
    if (node.has("var")) {
      return new Expr.Var(node.get("var").asText());
    }
    if (node.has("param")) {
      return new Expr.Param(node.get("param").asText());
    }
    if (node.has("const")) {
      return new Expr.Const(Values.fromJson(node.get("const")));
    }
    List<Expr> args = new ArrayList<>();
    node.get("args").forEach(a -> args.add(expr(a)));
    return new Expr.Op(Operator.fromJson(node.get("op").asText()).orElseThrow(), args);
  }

  private static List<RuleSet.InputDef> inputs(JsonNode node) {
    List<RuleSet.InputDef> out = new ArrayList<>();
    node.forEach(
        in -> {
          List<String> values = new ArrayList<>();
          in.path("values").forEach(v -> values.add(v.asText()));
          out.add(
              new RuleSet.InputDef(
                  in.get("name").asText(),
                  RuleSet.InputType.valueOf(in.get("type").asText()),
                  in.path("required").asBoolean(false),
                  List.copyOf(values)));
        });
    return List.copyOf(out);
  }

  private static Map<String, RuleSet.Table> tables(JsonNode node) {
    Map<String, RuleSet.Table> out = new LinkedHashMap<>();
    node.fields()
        .forEachRemaining(
            e -> {
              List<RuleSet.Table.Row> rows = new ArrayList<>();
              e.getValue()
                  .get("rows")
                  .forEach(
                      r ->
                          rows.add(
                              new RuleSet.Table.Row(
                                  Values.fromJson(r.get("from")),
                                  Values.fromJson(r.get("to")),
                                  Values.fromJson(r.get("key")),
                                  Values.fromJson(r.get("value")))));
              out.put(
                  e.getKey(),
                  new RuleSet.Table(
                      e.getKey(),
                      RuleSet.TableType.valueOf(e.getValue().get("type").asText()),
                      List.copyOf(rows)));
            });
    return Collections.unmodifiableMap(out);
  }

  /**
   * Derives in dependency order, so evaluation is a single pass (the validator excluded cycles).
   */
  private static List<RuleSet.Derive> derive(JsonNode node) {
    Map<String, RuleSet.Derive> byName = new LinkedHashMap<>();
    node.forEach(
        d ->
            byName.put(
                d.get("name").asText(),
                new RuleSet.Derive(
                    d.get("name").asText(),
                    expr(d.get("expr")),
                    d.has("scale") ? d.get("scale").asInt() : null)));
    List<RuleSet.Derive> ordered = new ArrayList<>();
    Set<String> done = new HashSet<>();
    byName.keySet().forEach(n -> visit(n, byName, done, ordered));
    return List.copyOf(ordered);
  }

  private static void visit(
      String name, Map<String, RuleSet.Derive> byName, Set<String> done, List<RuleSet.Derive> out) {
    if (!byName.containsKey(name) || !done.add(name)) {
      return;
    }
    Set<String> deps = new HashSet<>();
    Expr.vars(byName.get(name).expr(), deps);
    deps.stream().sorted().forEach(dep -> visit(dep, byName, done, out));
    out.add(byName.get(name));
  }

  private static RuleSet.Decision decision(JsonNode d) {
    List<String> rank = new ArrayList<>();
    d.path("outcomeRank").forEach(r -> rank.add(r.asText()));
    return new RuleSet.Decision(
        RuleSet.HitPolicy.valueOf(d.get("hitPolicy").asText()),
        List.copyOf(rank),
        d.get("noMatchOutcome").asText(),
        d.path("noMatchReasonCode").asText(null),
        d.get("missingInputOutcome").asText(),
        d.path("missingConfigOutcome").asText(null),
        d.path("missingConfigReasonCode").asText(null),
        RuleSet.CollectReasonCodes.valueOf(d.path("collectReasonCodes").asText("ALL_FIRED")));
  }

  private static List<RuleSet.Rule> rules(JsonNode node) {
    List<RuleSet.Rule> out = new ArrayList<>();
    for (int i = 0; i < node.size(); i++) {
      JsonNode r = node.get(i);
      JsonNode then = r.get("then");
      Map<String, Expr> params = new LinkedHashMap<>();
      then.path("params")
          .fields()
          .forEachRemaining(e -> params.put(e.getKey(), expr(e.getValue())));
      List<String> actions = new ArrayList<>();
      then.path("actions").forEach(a -> actions.add(a.asText()));
      JsonNode outcome = then.get("outcome");
      out.add(
          new RuleSet.Rule(
              r.get("ruleId").asText(),
              r.get("priority").asInt(),
              i,
              expr(r.get("when")),
              outcome.isTextual() ? new Expr.Const(outcome.asText()) : expr(outcome),
              then.path("reasonCode").asText(null),
              Collections.unmodifiableMap(params),
              List.copyOf(actions)));
    }
    return List.copyOf(out);
  }

  private static Map<String, RuleSet.ReasonCode> reasonCodes(JsonNode node) {
    Map<String, RuleSet.ReasonCode> out = new HashMap<>();
    node.fields()
        .forEachRemaining(
            e ->
                out.put(
                    e.getKey(),
                    new RuleSet.ReasonCode(
                        e.getKey(),
                        RuleSet.Severity.valueOf(e.getValue().get("severity").asText()),
                        e.getValue().get("rmText").asText(),
                        e.getValue().path("customerText").asText(null))));
    return Collections.unmodifiableMap(out);
  }

  private static List<RuleSet.TestCase> testCases(JsonNode node) {
    List<RuleSet.TestCase> out = new ArrayList<>();
    node.forEach(
        t ->
            out.add(
                new RuleSet.TestCase(
                    t.get("name").asText(),
                    t.get("inputs").deepCopy(),
                    t.get("expect").deepCopy(),
                    t.path("evaluatedAt").asText(null),
                    t.path("sample").asBoolean(false))));
    return List.copyOf(out);
  }
}
