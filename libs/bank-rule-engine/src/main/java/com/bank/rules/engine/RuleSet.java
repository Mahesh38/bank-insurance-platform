package com.bank.rules.engine;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A parsed, validated rule-set/v1 document (ADR-024 section 2). Immutable. Obtain one through
 * {@link RuleSetParser#parse(String)}, which refuses anything the validator rejects, so an instance
 * of this type is always evaluable.
 *
 * @param document the original document tree; {@link #documentSha256()} hashes its canonical form
 */
public record RuleSet(
    String ruleSetId,
    String domain,
    Scope scope,
    String version,
    OffsetDateTime effectiveFrom,
    OffsetDateTime effectiveTo,
    Classification classification,
    Pinning pinning,
    int scale,
    List<InputDef> inputs,
    Map<String, Table> tables,
    List<Derive> derive,
    Decision decision,
    List<Rule> rules,
    Map<String, Object> parameters,
    Map<String, ReasonCode> reasonCodes,
    List<TestCase> testCases,
    JsonNode document,
    String documentSha256) {

  public static final String FORMAT = "rule-set/v1";

  /** Who must approve a version (ADR-024 section 5 point 2). */
  public enum Classification {
    COMPLIANCE("COMPLIANCE_APPROVER"),
    PRODUCT("PRODUCT_APPROVER"),
    OPERATIONAL("CONFIG_APPROVER");

    private final String approverRole;

    Classification(String approverRole) {
      this.approverRole = approverRole;
    }

    public String approverRole() {
      return approverRole;
    }
  }

  /** ADR-024 section 5 point 5: whether an in-flight journey keeps the version it started with. */
  public enum Pinning {
    JOURNEY,
    NONE
  }

  public enum InputType {
    INTEGER,
    DECIMAL,
    STRING,
    BOOLEAN,
    DATE,
    ENUM,
    LIST
  }

  public enum TableType {
    RANGE,
    EXACT
  }

  public enum HitPolicy {
    PRIORITY,
    FIRST,
    COLLECT
  }

  public enum CollectReasonCodes {
    ALL_FIRED,
    WINNING_ONLY
  }

  public enum Severity {
    INFO,
    CAUTION,
    APPROVAL,
    BLOCK
  }

  /** {@code lob} / {@code productClass} may be null, meaning "any". */
  public record Scope(String lob, String productClass) {}

  public record InputDef(String name, InputType type, boolean required, List<String> values) {}

  /** RANGE rows are inclusive-lower, exclusive-upper; a null {@code to} is open-ended. */
  public record Table(String name, TableType type, List<Row> rows) {
    public record Row(Object from, Object to, Object key, Object value) {}
  }

  /** {@code scale}, when set, rounds the derived number HALF_UP to that many decimals. */
  public record Derive(String name, Expr expr, Integer scale) {}

  public record Decision(
      HitPolicy hitPolicy,
      List<String> outcomeRank,
      String noMatchOutcome,
      String noMatchReasonCode,
      String missingInputOutcome,
      String missingConfigOutcome,
      String missingConfigReasonCode,
      CollectReasonCodes collectReasonCodes) {

    /** Rank index; lower is more restrictive. Unknown outcomes rank last. */
    public int rank(String outcome) {
      int i = outcomeRank.indexOf(outcome);
      return i < 0 ? Integer.MAX_VALUE : i;
    }
  }

  /**
   * @param outcome a literal outcome, or {@link Expr.Param} when the outcome itself is configured
   *     (lead exception "approval / block as configured")
   */
  public record Rule(
      String ruleId,
      int priority,
      int order,
      Expr when,
      Expr outcome,
      String reasonCode,
      Map<String, Expr> params,
      List<String> actions) {}

  public record ReasonCode(String code, Severity severity, String rmText, String customerText) {}

  public record TestCase(
      String name, JsonNode inputs, JsonNode expect, String evaluatedAt, boolean sample) {}

  public Optional<InputDef> input(String name) {
    return inputs.stream().filter(i -> i.name().equals(name)).findFirst();
  }
}
