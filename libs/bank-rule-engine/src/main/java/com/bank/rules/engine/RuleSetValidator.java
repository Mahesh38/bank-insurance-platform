package com.bank.rules.engine;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Structural and semantic validation of a rule-set/v1 document. Mirrors {@code
 * schema/rule-set-v1.schema.json} for structure and adds what a schema cannot express: operators
 * outside the closed set, unknown variables / parameters / tables, cycles in {@code derive}, reason
 * codes and outcomes a rule names but the document does not define (ADR-024 sections 2-4).
 *
 * <p>Hand-written rather than a JSON-Schema library at runtime so the engine adds nothing to a
 * deciding service's classpath beyond Jackson; a test proves the schema file and this class agree
 * on every seed.
 */
public final class RuleSetValidator {

  static final Pattern RULE_SET_ID = Pattern.compile("^[A-Z][A-Z0-9_]*(\\.[A-Z0-9_]+)*$");
  static final Pattern SEMVER = Pattern.compile("^\\d+\\.\\d+\\.\\d+$");

  private static final Set<String> TOP_LEVEL =
      Set.of(
          "format",
          "ruleSetId",
          "domain",
          "scope",
          "version",
          "effectiveFrom",
          "effectiveTo",
          "classification",
          "pinning",
          "scale",
          "description",
          "source",
          "governance",
          "inputs",
          "tables",
          "derive",
          "decision",
          "rules",
          "parameters",
          "reasonCodes",
          "testCases");
  private static final Set<String> INPUT_KEYS =
      Set.of("name", "type", "required", "values", "description", "sourceRef");
  private static final Set<String> TABLE_KEYS = Set.of("type", "key", "rows", "description");
  private static final Set<String> DERIVE_KEYS = Set.of("name", "expr", "scale", "description");
  private static final Set<String> DECISION_KEYS =
      Set.of(
          "hitPolicy",
          "outcomeRank",
          "noMatchOutcome",
          "noMatchReasonCode",
          "missingInputOutcome",
          "missingConfigOutcome",
          "missingConfigReasonCode",
          "collectReasonCodes");
  private static final Set<String> RULE_KEYS =
      Set.of("ruleId", "priority", "when", "then", "description", "sourceRef");
  private static final Set<String> THEN_KEYS = Set.of("outcome", "reasonCode", "params", "actions");
  private static final Set<String> REASON_KEYS = Set.of("severity", "rmText", "customerText");
  private static final Set<String> TEST_KEYS =
      Set.of("name", "description", "inputs", "expect", "evaluatedAt", "sample");
  private static final Set<String> EXPECT_KEYS =
      Set.of("outcome", "reasonCodes", "fired", "derived", "messages");

  private final List<String> errors = new ArrayList<>();
  private final Set<String> vars = new HashSet<>();
  private JsonNode parameters;
  private Set<String> tables = Set.of();

  private RuleSetValidator() {}

  /** Every finding; empty means the document is evaluable. */
  public static List<String> validate(JsonNode doc) {
    RuleSetValidator v = new RuleSetValidator();
    v.run(doc);
    return List.copyOf(v.errors);
  }

  private void run(JsonNode doc) {
    if (doc == null || !doc.isObject()) {
      errors.add("/: document must be a JSON object");
      return;
    }
    unknownKeys(doc, TOP_LEVEL, "");
    if (!RuleSet.FORMAT.equals(doc.path("format").asText(null))) {
      errors.add("/format: must be '" + RuleSet.FORMAT + "'");
    }
    text(doc, "ruleSetId", "/ruleSetId", RULE_SET_ID);
    text(doc, "domain", "/domain", null);
    text(doc, "version", "/version", SEMVER);
    oneOf(doc.path("classification"), "/classification", RuleSet.Classification.class, true);
    oneOf(doc.path("pinning"), "/pinning", RuleSet.Pinning.class, false);
    if (doc.has("scale")
        && !(doc.get("scale").canConvertToInt() && doc.get("scale").asInt() >= 0)) {
      errors.add("/scale: must be a non-negative integer");
    }
    JsonNode scope = doc.path("scope");
    if (!scope.isObject()) {
      errors.add("/scope: required object {lob, productClass}");
    } else {
      unknownKeys(scope, Set.of("lob", "productClass"), "/scope");
    }
    OffsetDateTime from = dateTime(doc.path("effectiveFrom"), "/effectiveFrom", true);
    OffsetDateTime to = dateTime(doc.path("effectiveTo"), "/effectiveTo", false);
    if (from != null && to != null && !to.isAfter(from)) {
      errors.add("/effectiveTo: must be after effectiveFrom");
    }
    parameters = doc.path("parameters");
    if (!parameters.isMissingNode() && !parameters.isObject()) {
      errors.add("/parameters: must be an object");
    }
    Set<String> reasonCodes = reasonCodes(doc.path("reasonCodes"));
    inputs(doc.path("inputs"));
    tables(doc.path("tables"));
    derive(doc.path("derive"));
    Set<String> outcomes = decision(doc.path("decision"), reasonCodes);
    rules(doc.path("rules"), reasonCodes, outcomes, doc.path("decision"));
    testCases(doc.path("testCases"));
  }

  private Set<String> reasonCodes(JsonNode node) {
    Set<String> codes = new HashSet<>();
    if (!node.isObject()) {
      errors.add("/reasonCodes: required object");
      return codes;
    }
    node.fields()
        .forEachRemaining(
            e -> {
              String path = "/reasonCodes/" + e.getKey();
              codes.add(e.getKey());
              unknownKeys(e.getValue(), REASON_KEYS, path);
              oneOf(
                  e.getValue().path("severity"), path + "/severity", RuleSet.Severity.class, true);
              if (!e.getValue().path("rmText").isTextual()) {
                errors.add(path + "/rmText: required text");
              }
            });
    return codes;
  }

  private void inputs(JsonNode node) {
    if (!node.isArray() || node.isEmpty()) {
      errors.add("/inputs: required non-empty array");
      return;
    }
    for (int i = 0; i < node.size(); i++) {
      JsonNode in = node.get(i);
      String path = "/inputs/" + i;
      unknownKeys(in, INPUT_KEYS, path);
      String name = in.path("name").asText("");
      if (name.isBlank()) {
        errors.add(path + "/name: required");
      } else if (!vars.add(name)) {
        errors.add(path + "/name: duplicate '" + name + "'");
      }
      boolean known = oneOf(in.path("type"), path + "/type", RuleSet.InputType.class, true);
      if (known
          && "ENUM".equals(in.path("type").asText())
          && (!in.path("values").isArray() || in.path("values").isEmpty())) {
        errors.add(path + "/values: ENUM input needs a non-empty values list");
      }
    }
  }

  private void tables(JsonNode node) {
    if (node.isMissingNode()) {
      return;
    }
    if (!node.isObject()) {
      errors.add("/tables: must be an object");
      return;
    }
    Set<String> names = new HashSet<>();
    node.fields()
        .forEachRemaining(
            e -> {
              String path = "/tables/" + e.getKey();
              names.add(e.getKey());
              JsonNode t = e.getValue();
              unknownKeys(t, TABLE_KEYS, path);
              oneOf(t.path("type"), path + "/type", RuleSet.TableType.class, true);
              JsonNode rows = t.path("rows");
              if (!rows.isArray() || rows.isEmpty()) {
                errors.add(path + "/rows: required non-empty array");
                return;
              }
              boolean range = "RANGE".equals(t.path("type").asText());
              for (int i = 0; i < rows.size(); i++) {
                JsonNode r = rows.get(i);
                String rp = path + "/rows/" + i;
                if (!r.has("value")) {
                  errors.add(rp + "/value: required");
                }
                if (range) {
                  if (!r.path("from").isNumber()) {
                    errors.add(rp + "/from: required number");
                  }
                  JsonNode to = r.path("to");
                  if (!to.isMissingNode() && !to.isNull() && !to.isNumber()) {
                    errors.add(rp + "/to: must be a number or null");
                  } else if (to.isNumber()
                      && r.path("from").isNumber()
                      && to.decimalValue().compareTo(r.path("from").decimalValue()) <= 0) {
                    errors.add(rp + ": 'to' must be greater than 'from'");
                  }
                } else if (!r.has("key")) {
                  errors.add(rp + "/key: required for EXACT tables");
                }
              }
            });
    tables = names;
  }

  private void derive(JsonNode node) {
    if (node.isMissingNode()) {
      return;
    }
    if (!node.isArray()) {
      errors.add("/derive: must be an array");
      return;
    }
    Map<String, Set<String>> deps = new HashMap<>();
    for (int i = 0; i < node.size(); i++) {
      String name = node.get(i).path("name").asText("");
      if (!name.isBlank() && !vars.add(name)) {
        errors.add("/derive/" + i + "/name: '" + name + "' clashes with an input or derive");
      }
    }
    for (int i = 0; i < node.size(); i++) {
      JsonNode d = node.get(i);
      String path = "/derive/" + i;
      unknownKeys(d, DERIVE_KEYS, path);
      String name = d.path("name").asText("");
      if (name.isBlank()) {
        errors.add(path + "/name: required");
        continue;
      }
      if (d.has("scale") && !(d.get("scale").canConvertToInt() && d.get("scale").asInt() >= 0)) {
        errors.add(path + "/scale: must be a non-negative integer");
      }
      Expr expr = expression(d.path("expr"), path + "/expr");
      Set<String> used = new HashSet<>();
      if (expr != null) {
        Expr.vars(expr, used);
      }
      deps.put(name, used);
    }
    for (String start : deps.keySet()) {
      List<String> cycle = cycleFrom(start, deps, new LinkedHashSet<>());
      if (cycle != null) {
        errors.add("/derive: cycle " + String.join(" -> ", cycle));
        return;
      }
    }
  }

  private static List<String> cycleFrom(
      String node, Map<String, Set<String>> deps, LinkedHashSet<String> path) {
    if (path.contains(node)) {
      List<String> cycle = new ArrayList<>(path);
      cycle.add(node);
      return cycle.subList(cycle.indexOf(node), cycle.size());
    }
    if (!deps.containsKey(node)) {
      return null;
    }
    path.add(node);
    for (String next : deps.get(node)) {
      List<String> found = cycleFrom(next, deps, path);
      if (found != null) {
        return found;
      }
    }
    path.remove(node);
    return null;
  }

  private Set<String> decision(JsonNode d, Set<String> reasonCodes) {
    Set<String> outcomes = new HashSet<>();
    if (!d.isObject()) {
      errors.add("/decision: required object");
      return outcomes;
    }
    unknownKeys(d, DECISION_KEYS, "/decision");
    oneOf(d.path("hitPolicy"), "/decision/hitPolicy", RuleSet.HitPolicy.class, true);
    oneOf(
        d.path("collectReasonCodes"),
        "/decision/collectReasonCodes",
        RuleSet.CollectReasonCodes.class,
        false);
    JsonNode rank = d.path("outcomeRank");
    if (rank.isArray()) {
      rank.forEach(r -> outcomes.add(r.asText()));
    } else if (!rank.isMissingNode()) {
      errors.add("/decision/outcomeRank: must be an array of outcome names");
    }
    if ("PRIORITY".equals(d.path("hitPolicy").asText()) && outcomes.isEmpty()) {
      errors.add("/decision/outcomeRank: PRIORITY needs a non-empty outcomeRank");
    }
    for (String field : List.of("noMatchOutcome", "missingInputOutcome")) {
      if (!d.path(field).isTextual()) {
        errors.add("/decision/" + field + ": required");
      }
    }
    for (String field : List.of("noMatchOutcome", "missingInputOutcome", "missingConfigOutcome")) {
      String o = d.path(field).asText(null);
      if (o != null && !outcomes.isEmpty() && !outcomes.contains(o)) {
        errors.add("/decision/" + field + ": '" + o + "' is not in outcomeRank");
      }
    }
    for (String field : List.of("noMatchReasonCode", "missingConfigReasonCode")) {
      String c = d.path(field).asText(null);
      if (c != null && !reasonCodes.contains(c)) {
        errors.add("/decision/" + field + ": reason code '" + c + "' is not defined");
      }
    }
    if (d.has("missingConfigReasonCode") && !d.has("missingConfigOutcome")) {
      errors.add("/decision/missingConfigOutcome: required with missingConfigReasonCode");
    }
    return outcomes;
  }

  private void rules(JsonNode node, Set<String> reasonCodes, Set<String> outcomes, JsonNode d) {
    if (!node.isArray() || node.isEmpty()) {
      errors.add("/rules: required non-empty array");
      return;
    }
    Set<String> ids = new HashSet<>();
    for (int i = 0; i < node.size(); i++) {
      JsonNode r = node.get(i);
      String path = "/rules/" + i;
      unknownKeys(r, RULE_KEYS, path);
      String id = r.path("ruleId").asText("");
      if (id.isBlank()) {
        errors.add(path + "/ruleId: required");
      } else if (!ids.add(id)) {
        errors.add(path + "/ruleId: duplicate '" + id + "'");
      }
      if (!r.path("priority").canConvertToInt()) {
        errors.add(path + "/priority: required integer");
      }
      expression(r.path("when"), path + "/when");
      JsonNode then = r.path("then");
      if (!then.isObject()) {
        errors.add(path + "/then: required object");
        continue;
      }
      unknownKeys(then, THEN_KEYS, path + "/then");
      JsonNode outcome = then.path("outcome");
      if (outcome.isTextual()) {
        if (!outcomes.isEmpty() && !outcomes.contains(outcome.asText())) {
          errors.add(path + "/then/outcome: '" + outcome.asText() + "' is not in outcomeRank");
        }
      } else if (outcome.isObject() && outcome.has("param")) {
        expression(outcome, path + "/then/outcome");
      } else {
        errors.add(path + "/then/outcome: required outcome name or {\"param\": path}");
      }
      JsonNode code = then.path("reasonCode");
      if (code.isTextual() && !reasonCodes.contains(code.asText())) {
        errors.add(path + "/then/reasonCode: '" + code.asText() + "' is not defined");
      }
      JsonNode params = then.path("params");
      if (params.isObject()) {
        params
            .fields()
            .forEachRemaining(e -> expression(e.getValue(), path + "/then/params/" + e.getKey()));
      } else if (!params.isMissingNode()) {
        errors.add(path + "/then/params: must be an object of expressions");
      }
      JsonNode actions = then.path("actions");
      if (!actions.isMissingNode() && !actions.isArray()) {
        errors.add(path + "/then/actions: must be an array of strings");
      }
    }
    if (d.path("missingConfigOutcome").isMissingNode() && usesParams(node)) {
      // An unset parameter with nowhere to route is itself a configuration defect.
      errors.add("/decision/missingConfigOutcome: required when rules read parameters");
    }
  }

  private static boolean usesParams(JsonNode rules) {
    return rules.toString().contains("\"param\"");
  }

  private void testCases(JsonNode node) {
    if (node.isMissingNode()) {
      errors.add("/testCases: required (publish is refused without passing test cases)");
      return;
    }
    if (!node.isArray() || node.isEmpty()) {
      errors.add("/testCases: required non-empty array");
      return;
    }
    Set<String> names = new HashSet<>();
    for (int i = 0; i < node.size(); i++) {
      JsonNode t = node.get(i);
      String path = "/testCases/" + i;
      unknownKeys(t, TEST_KEYS, path);
      if (!names.add(t.path("name").asText(""))) {
        errors.add(path + "/name: required and unique");
      }
      if (!t.path("inputs").isObject()) {
        errors.add(path + "/inputs: required object");
      }
      if (!t.path("expect").isObject()) {
        errors.add(path + "/expect: required object");
      } else {
        unknownKeys(t.path("expect"), EXPECT_KEYS, path + "/expect");
      }
    }
  }

  /** Validates an expression and returns its tree, or null after recording errors. */
  private Expr expression(JsonNode node, String path) {
    if (node == null || !node.isObject()) {
      errors.add(path + ": expression must be an object");
      return null;
    }
    if (node.size() != 1 && !(node.size() == 2 && node.has("op") && node.has("args"))) {
      errors.add(path + ": expression must be exactly one of var, param, const, or op+args");
      return null;
    }
    if (node.has("var")) {
      String name = node.get("var").asText("");
      if (!vars.contains(name)) {
        errors.add(path + ": unknown var '" + name + "'");
      }
      return new Expr.Var(name);
    }
    if (node.has("param")) {
      String p = node.get("param").asText("");
      if (!paramExists(p)) {
        errors.add(path + ": unknown param '" + p + "'");
      }
      return new Expr.Param(p);
    }
    if (node.has("const")) {
      return new Expr.Const(Values.fromJson(node.get("const")));
    }
    if (!node.has("op")) {
      errors.add(path + ": unknown expression form " + node.fieldNames().next());
      return null;
    }
    String opName = node.get("op").asText("");
    var op = Operator.fromJson(opName);
    if (op.isEmpty()) {
      errors.add(path + "/op: unknown operator '" + opName + "'");
      return null;
    }
    JsonNode args = node.path("args");
    if (!args.isArray()) {
      errors.add(path + "/args: required array");
      return null;
    }
    if (args.size() < op.get().minArgs() || args.size() > op.get().maxArgs()) {
      errors.add(path + "/args: '" + opName + "' takes " + arity(op.get()) + " arguments");
    }
    if (op.get() == Operator.LOOKUP) {
      JsonNode table = args.path(0).path("const");
      if (!table.isTextual()) {
        errors.add(path + "/args/0: lookup table must be {\"const\": \"<table name>\"}");
      } else if (!tables.contains(table.asText())) {
        errors.add(path + "/args/0: unknown table '" + table.asText() + "'");
      }
    }
    List<Expr> parsed = new ArrayList<>();
    for (int i = 0; i < args.size(); i++) {
      parsed.add(expression(args.get(i), path + "/args/" + i));
    }
    return parsed.contains(null) ? null : new Expr.Op(op.get(), parsed);
  }

  private static String arity(Operator op) {
    if (op.maxArgs() == Integer.MAX_VALUE) {
      return "at least " + op.minArgs();
    }
    return op.minArgs() == op.maxArgs()
        ? String.valueOf(op.minArgs())
        : op.minArgs() + ".." + op.maxArgs();
  }

  /** True when every segment of the dotted path exists (the leaf may hold null = "unset"). */
  private boolean paramExists(String path) {
    JsonNode cur = parameters;
    for (String seg : path.split("\\.", -1)) {
      if (cur == null || !cur.isObject() || !cur.has(seg)) {
        return false;
      }
      cur = cur.get(seg);
    }
    return !path.isBlank();
  }

  private void text(JsonNode doc, String field, String path, Pattern pattern) {
    JsonNode n = doc.path(field);
    if (!n.isTextual() || n.asText().isBlank()) {
      errors.add(path + ": required");
    } else if (pattern != null && !pattern.matcher(n.asText()).matches()) {
      errors.add(path + ": '" + n.asText() + "' does not match " + pattern.pattern());
    }
  }

  private <E extends Enum<E>> boolean oneOf(
      JsonNode n, String path, Class<E> type, boolean required) {
    if (n.isMissingNode() || n.isNull()) {
      if (required) {
        errors.add(path + ": required");
      }
      return false;
    }
    try {
      Enum.valueOf(type, n.asText());
      return true;
    } catch (IllegalArgumentException e) {
      errors.add(path + ": '" + n.asText() + "' is not one of " + List.of(type.getEnumConstants()));
      return false;
    }
  }

  private OffsetDateTime dateTime(JsonNode n, String path, boolean required) {
    if (n.isMissingNode() || n.isNull()) {
      if (required) {
        errors.add(path + ": required RFC 3339 date-time");
      }
      return null;
    }
    try {
      return OffsetDateTime.parse(n.asText());
    } catch (DateTimeParseException e) {
      errors.add(path + ": not an RFC 3339 date-time");
      return null;
    }
  }

  private void unknownKeys(JsonNode node, Set<String> allowed, String path) {
    if (!node.isObject()) {
      return;
    }
    Iterator<String> it = node.fieldNames();
    while (it.hasNext()) {
      String k = it.next();
      if (!allowed.contains(k)) {
        errors.add(path + "/" + k + ": unknown field");
      }
    }
  }
}
