package com.bank.rules.engine;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Evaluates a {@link RuleSet} against inputs (ADR-024 sections 3, 4 and 6). Stateless and pure: no
 * clock, no I/O, no randomness, so the same rule-set version and inputs always produce the same
 * canonical result (SUIT-R05). Dates come in as inputs (by convention {@code asOf}).
 *
 * <p>Null semantics, chosen so a missing fact never satisfies a condition by accident: a comparison
 * with a null operand is false; arithmetic with a null operand is null; a division by zero is null.
 * Use {@code isNull} / {@code isPresent} to test absence explicitly.
 */
public final class RuleEngine {

  public static final String ENGINE_VERSION = "1.0.0";

  /** Engine-defined reason when a required input is absent. */
  public static final String RULE_INPUT_MISSING = "RULE_INPUT_MISSING";

  /** Engine-defined reason when an input is present but not of its declared type. */
  public static final String RULE_INPUT_INVALID = "RULE_INPUT_INVALID";

  /** Engine-defined reason for an unset parameter when the rule set names no reason of its own. */
  public static final String RULE_CONFIG_MISSING = "RULE_CONFIG_MISSING";

  private static final Pattern PLACEHOLDER = Pattern.compile("@([A-Za-z_][A-Za-z0-9_]*)");

  private RuleEngine() {}

  public static EvaluationResult evaluate(RuleSet rs, JsonNode inputs) {
    return evaluate(rs, inputs, null);
  }

  public static EvaluationResult evaluate(RuleSet rs, JsonNode inputs, String evaluatedAt) {
    ObjectNode given =
        inputs != null && inputs.isObject()
            ? (ObjectNode) inputs
            : Values.mapper().createObjectNode();
    Map<String, Object> vars = new TreeMap<>();
    List<String> missing = new ArrayList<>();
    boolean invalid = false;
    for (RuleSet.InputDef def : rs.inputs()) {
      JsonNode raw = given.get(def.name());
      if (raw == null || raw.isNull()) {
        vars.put(def.name(), null);
        if (def.required()) {
          missing.add(def.name());
        }
        continue;
      }
      Object typed = coerce(def, raw);
      if (typed == null) {
        missing.add(def.name());
        invalid = true;
      }
      vars.put(def.name(), typed);
    }
    String inputsHash = "sha256:" + Values.sha256(Values.canonicalString(Values.toJson(vars)));
    RuleSet.Decision d = rs.decision();

    if (!missing.isEmpty()) {
      String code = invalid ? RULE_INPUT_INVALID : RULE_INPUT_MISSING;
      Map<String, Object> p = Map.of("missing", String.join(", ", missing));
      return new EvaluationResult(
          rs.ruleSetId(),
          rs.version(),
          ENGINE_VERSION,
          inputsHash,
          Map.of(),
          d.missingInputOutcome(),
          List.of(),
          List.of(code),
          messages(rs, List.of(code), Map.of(code, p)),
          List.copyOf(missing),
          evaluatedAt);
    }

    Context ctx = new Context(rs, vars);
    Map<String, Object> derived = new TreeMap<>();
    Set<String> deriveGaps = new TreeSet<>();
    for (RuleSet.Derive dv : rs.derive()) {
      deriveGaps.addAll(unsetParams(rs, List.of(dv.expr())));
    }
    if (!deriveGaps.isEmpty()) {
      return configGapForWholeSet(rs, inputsHash, deriveGaps, evaluatedAt);
    }
    for (RuleSet.Derive dv : rs.derive()) {
      Object v = ctx.eval(dv.expr());
      if (v instanceof BigDecimal n && dv.scale() != null) {
        v = n.setScale(dv.scale(), RoundingMode.HALF_UP);
      }
      vars.put(dv.name(), v);
      derived.put(dv.name(), v);
    }

    List<RuleSet.Rule> ordered = new ArrayList<>(rs.rules());
    ordered.sort(
        Comparator.comparingInt(RuleSet.Rule::priority).thenComparingInt(RuleSet.Rule::order));
    List<EvaluationResult.Fired> fired = new ArrayList<>();
    for (RuleSet.Rule rule : ordered) {
      List<Expr> reads = new ArrayList<>(rule.params().values());
      reads.add(rule.when());
      reads.add(rule.outcome());
      Set<String> gaps = unsetParams(rs, reads);
      if (!gaps.isEmpty()) {
        fired.add(configGap(rs, rule.ruleId(), gaps));
      } else if (Boolean.TRUE.equals(ctx.eval(rule.when()))) {
        Map<String, Object> params = new LinkedHashMap<>();
        rule.params().forEach((k, e) -> params.put(k, ctx.eval(e)));
        Object outcome = ctx.eval(rule.outcome());
        fired.add(
            new EvaluationResult.Fired(
                rule.ruleId(),
                Objects.toString(outcome, null),
                rule.reasonCode(),
                params,
                rule.actions(),
                List.of()));
      } else {
        continue;
      }
      if (d.hitPolicy() == RuleSet.HitPolicy.FIRST) {
        break;
      }
    }
    return decide(rs, inputsHash, derived, fired, evaluatedAt);
  }

  private static EvaluationResult decide(
      RuleSet rs,
      String inputsHash,
      Map<String, Object> derived,
      List<EvaluationResult.Fired> fired,
      String evaluatedAt) {
    RuleSet.Decision d = rs.decision();
    if (fired.isEmpty()) {
      List<String> codes =
          d.noMatchReasonCode() == null ? List.of() : List.of(d.noMatchReasonCode());
      return new EvaluationResult(
          rs.ruleSetId(),
          rs.version(),
          ENGINE_VERSION,
          inputsHash,
          derived,
          d.noMatchOutcome(),
          List.of(),
          codes,
          messages(rs, codes, Map.of()),
          List.of(),
          evaluatedAt);
    }
    EvaluationResult.Fired winner =
        fired.stream()
            .min(Comparator.comparingInt((EvaluationResult.Fired f) -> d.rank(f.outcome())))
            .orElseThrow();
    String outcome =
        switch (d.hitPolicy()) {
          case FIRST -> fired.get(0).outcome();
          case PRIORITY -> winner.outcome();
          case COLLECT -> d.outcomeRank().isEmpty() ? null : winner.outcome();
        };
    List<EvaluationResult.Fired> contributing =
        d.collectReasonCodes() == RuleSet.CollectReasonCodes.WINNING_ONLY
                && d.hitPolicy() != RuleSet.HitPolicy.COLLECT
            ? List.of(d.hitPolicy() == RuleSet.HitPolicy.FIRST ? fired.get(0) : winner)
            : fired;
    Set<String> codes = new LinkedHashSet<>();
    Map<String, Map<String, Object>> paramsByCode = new LinkedHashMap<>();
    for (EvaluationResult.Fired f : contributing) {
      if (f.reasonCode() != null && codes.add(f.reasonCode())) {
        paramsByCode.put(f.reasonCode(), f.params());
      }
    }
    List<String> codeList = List.copyOf(codes);
    return new EvaluationResult(
        rs.ruleSetId(),
        rs.version(),
        ENGINE_VERSION,
        inputsHash,
        derived,
        outcome,
        List.copyOf(fired),
        codeList,
        messages(rs, codeList, paramsByCode),
        List.of(),
        evaluatedAt);
  }

  private static EvaluationResult.Fired configGap(RuleSet rs, String ruleId, Set<String> gaps) {
    RuleSet.Decision d = rs.decision();
    String code =
        d.missingConfigReasonCode() != null ? d.missingConfigReasonCode() : RULE_CONFIG_MISSING;
    String outcome =
        d.missingConfigOutcome() != null ? d.missingConfigOutcome() : d.missingInputOutcome();
    Map<String, Object> params = new LinkedHashMap<>();
    params.put("ruleId", ruleId);
    params.put("missingParameters", String.join(", ", gaps));
    return new EvaluationResult.Fired(ruleId, outcome, code, params, List.of(), List.copyOf(gaps));
  }

  private static EvaluationResult configGapForWholeSet(
      RuleSet rs, String inputsHash, Set<String> gaps, String evaluatedAt) {
    EvaluationResult.Fired gap = configGap(rs, "derive", gaps);
    return new EvaluationResult(
        rs.ruleSetId(),
        rs.version(),
        ENGINE_VERSION,
        inputsHash,
        Map.of(),
        gap.outcome(),
        List.of(gap),
        List.of(gap.reasonCode()),
        messages(rs, List.of(gap.reasonCode()), Map.of(gap.reasonCode(), gap.params())),
        List.of(),
        evaluatedAt);
  }

  private static Set<String> unsetParams(RuleSet rs, List<Expr> exprs) {
    Set<String> paths = new TreeSet<>();
    exprs.forEach(e -> Expr.strictParams(e, paths));
    paths.removeIf(p -> param(rs, p) != null);
    return paths;
  }

  static Object param(RuleSet rs, String path) {
    Object cur = rs.parameters();
    for (String seg : path.split("\\.")) {
      if (!(cur instanceof Map<?, ?> m)) {
        return null;
      }
      cur = m.get(seg);
    }
    return cur;
  }

  private static List<EvaluationResult.Message> messages(
      RuleSet rs, List<String> codes, Map<String, Map<String, Object>> paramsByCode) {
    List<EvaluationResult.Message> out = new ArrayList<>();
    for (String code : codes) {
      RuleSet.ReasonCode def = rs.reasonCodes().get(code);
      if (def == null) {
        continue;
      }
      Map<String, Object> p = paramsByCode.getOrDefault(code, Map.of());
      out.add(
          new EvaluationResult.Message(
              code,
              def.severity().name(),
              render(def.rmText(), p),
              def.customerText() == null ? null : render(def.customerText(), p)));
    }
    return List.copyOf(out);
  }

  static String render(String template, Map<String, Object> params) {
    Matcher m = PLACEHOLDER.matcher(template);
    StringBuilder sb = new StringBuilder();
    while (m.find()) {
      String key = m.group(1);
      String replacement = params.containsKey(key) ? Values.render(params.get(key)) : m.group();
      m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
    }
    m.appendTail(sb);
    return sb.toString();
  }

  /** Coerces a JSON input to its declared type; null means "wrong type". */
  static Object coerce(RuleSet.InputDef def, JsonNode raw) {
    return switch (def.type()) {
      case INTEGER ->
          raw.isNumber() && Values.normalize(raw.decimalValue()).scale() <= 0
              ? raw.decimalValue()
              : null;
      case DECIMAL -> raw.isNumber() ? raw.decimalValue() : null;
      case STRING -> raw.isTextual() ? raw.textValue() : null;
      case BOOLEAN -> raw.isBoolean() ? raw.booleanValue() : null;
      case DATE -> raw.isTextual() ? Values.toDate(raw.textValue()) : null;
      case ENUM ->
          raw.isTextual() && def.values().contains(raw.textValue()) ? raw.textValue() : null;
      case LIST -> raw.isArray() ? Values.fromJson(raw) : null;
    };
  }

  /** Evaluation context: variables (inputs, then derived values) and the rule set. */
  private record Context(RuleSet rs, Map<String, Object> vars) {

    Object eval(Expr e) {
      return switch (e) {
        case Expr.Var v -> vars.get(v.name());
        case Expr.Param p -> param(rs, p.path());
        case Expr.Const c -> c.value();
        case Expr.Op o -> op(o);
      };
    }

    private Object op(Expr.Op o) {
      List<Expr> a = o.args();
      return switch (o.op()) {
        case ALL -> a.stream().allMatch(x -> Boolean.TRUE.equals(eval(x)));
        case ANY -> a.stream().anyMatch(x -> Boolean.TRUE.equals(eval(x)));
        case NOT -> !Boolean.TRUE.equals(eval(a.get(0)));
        case EQ -> equal(eval(a.get(0)), eval(a.get(1)));
        case NE -> {
          Object l = eval(a.get(0));
          Object r = eval(a.get(1));
          yield l != null && r != null && !equal(l, r);
        }
        case GT -> cmp(a, c -> c > 0);
        case GTE -> cmp(a, c -> c >= 0);
        case LT -> cmp(a, c -> c < 0);
        case LTE -> cmp(a, c -> c <= 0);
        case IN -> member(eval(a.get(0)), eval(a.get(1)), true);
        case NOT_IN -> member(eval(a.get(0)), eval(a.get(1)), false);
        case BETWEEN -> {
          Object x = eval(a.get(0));
          Integer lo = compare(x, eval(a.get(1)));
          Integer hi = compare(x, eval(a.get(2)));
          yield lo != null && hi != null && lo >= 0 && hi <= 0;
        }
        case IS_NULL -> eval(a.get(0)) == null;
        case IS_PRESENT -> eval(a.get(0)) != null;
        case ADD -> fold(a, BigDecimal::add);
        case SUB -> fold(a, BigDecimal::subtract);
        case MUL -> fold(a, BigDecimal::multiply);
        case DIV -> {
          BigDecimal l = number(eval(a.get(0)));
          BigDecimal r = number(eval(a.get(1)));
          yield l == null || r == null || r.signum() == 0
              ? null
              : l.divide(r, rs.scale(), RoundingMode.HALF_UP);
        }
        case MIN -> fold(a, BigDecimal::min);
        case MAX -> fold(a, BigDecimal::max);
        case ABS -> {
          BigDecimal n = number(eval(a.get(0)));
          yield n == null ? null : n.abs();
        }
        case ROUND_UP_TO -> {
          BigDecimal n = number(eval(a.get(0)));
          BigDecimal unit = number(eval(a.get(1)));
          yield n == null || unit == null || unit.signum() <= 0
              ? null
              : n.divide(unit, 0, RoundingMode.CEILING).multiply(unit);
        }
        case LOOKUP -> lookup((String) ((Expr.Const) a.get(0)).value(), eval(a.get(1)));
        case DAYS_BETWEEN -> {
          LocalDate from = Values.toDate(eval(a.get(0)));
          LocalDate to = Values.toDate(eval(a.get(1)));
          yield from == null || to == null
              ? null
              : BigDecimal.valueOf(ChronoUnit.DAYS.between(from, to));
        }
        case AGE_AT -> {
          LocalDate dob = Values.toDate(eval(a.get(0)));
          LocalDate at = Values.toDate(eval(a.get(1)));
          yield dob == null || at == null
              ? null
              : BigDecimal.valueOf(Period.between(dob, at).getYears());
        }
        case COALESCE ->
            a.stream().map(this::eval).filter(Objects::nonNull).findFirst().orElse(null);
      };
    }

    private Object lookup(String tableName, Object key) {
      RuleSet.Table table = rs.tables().get(tableName);
      if (key == null) {
        return null;
      }
      for (RuleSet.Table.Row row : table.rows()) {
        if (table.type() == RuleSet.TableType.EXACT) {
          if (equal(row.key(), key)) {
            return row.value();
          }
          continue;
        }
        Integer lo = compare(key, row.from());
        Integer hi = row.to() == null ? Integer.valueOf(-1) : compare(key, row.to());
        if (lo != null && hi != null && lo >= 0 && hi < 0) {
          return row.value();
        }
      }
      return null;
    }

    private boolean cmp(List<Expr> a, java.util.function.IntPredicate test) {
      Integer c = compare(eval(a.get(0)), eval(a.get(1)));
      return c != null && test.test(c);
    }

    private BigDecimal fold(List<Expr> a, java.util.function.BinaryOperator<BigDecimal> f) {
      BigDecimal acc = null;
      for (Expr x : a) {
        BigDecimal n = number(eval(x));
        if (n == null) {
          return null;
        }
        acc = acc == null ? n : f.apply(acc, n);
      }
      return acc;
    }
  }

  private static BigDecimal number(Object v) {
    return v instanceof BigDecimal d ? d : null;
  }

  /** Ordering for numbers, dates and strings; null when incomparable (including null operands). */
  static Integer compare(Object l, Object r) {
    if (l == null || r == null) {
      return null;
    }
    if (l instanceof BigDecimal a && r instanceof BigDecimal b) {
      return a.compareTo(b);
    }
    LocalDate dl = l instanceof String || l instanceof LocalDate ? Values.toDate(l) : null;
    LocalDate dr = r instanceof String || r instanceof LocalDate ? Values.toDate(r) : null;
    if (dl != null && dr != null) {
      return dl.compareTo(dr);
    }
    if (l instanceof String a && r instanceof String b) {
      return a.compareTo(b);
    }
    return null;
  }

  static boolean equal(Object l, Object r) {
    if (l == null || r == null) {
      return false;
    }
    if (l instanceof BigDecimal a && r instanceof BigDecimal b) {
      return a.compareTo(b) == 0;
    }
    if (l instanceof LocalDate || r instanceof LocalDate) {
      Integer c = compare(l, r);
      return c != null && c == 0;
    }
    return l.equals(r);
  }

  private static boolean member(Object x, Object list, boolean in) {
    if (x == null || !(list instanceof List<?> items)) {
      return false;
    }
    boolean found = items.stream().anyMatch(i -> equal(x, i));
    return in == found;
  }
}
