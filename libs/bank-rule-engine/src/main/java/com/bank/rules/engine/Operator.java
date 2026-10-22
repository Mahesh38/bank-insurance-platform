package com.bank.rules.engine;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The closed operator set of rule-set/v1 (ADR-024 section 3). Adding an operator is an engine
 * release; nothing in a rule-set document can extend this list.
 */
public enum Operator {
  ALL("all", 1, Integer.MAX_VALUE),
  ANY("any", 1, Integer.MAX_VALUE),
  NOT("not", 1, 1),
  EQ("eq", 2, 2),
  NE("ne", 2, 2),
  GT("gt", 2, 2),
  GTE("gte", 2, 2),
  LT("lt", 2, 2),
  LTE("lte", 2, 2),
  IN("in", 2, 2),
  NOT_IN("notIn", 2, 2),
  BETWEEN("between", 3, 3),
  IS_NULL("isNull", 1, 1),
  IS_PRESENT("isPresent", 1, 1),
  ADD("add", 2, Integer.MAX_VALUE),
  SUB("sub", 2, 2),
  MUL("mul", 2, Integer.MAX_VALUE),
  DIV("div", 2, 2),
  MIN("min", 1, Integer.MAX_VALUE),
  MAX("max", 1, Integer.MAX_VALUE),
  ABS("abs", 1, 1),
  ROUND_UP_TO("roundUpTo", 2, 2),
  LOOKUP("lookup", 2, 2),
  DAYS_BETWEEN("daysBetween", 2, 2),
  AGE_AT("ageAt", 2, 2),
  COALESCE("coalesce", 1, Integer.MAX_VALUE);

  private static final Map<String, Operator> BY_NAME =
      Arrays.stream(values()).collect(Collectors.toMap(Operator::jsonName, Function.identity()));

  private final String jsonName;
  private final int minArgs;
  private final int maxArgs;

  Operator(String jsonName, int minArgs, int maxArgs) {
    this.jsonName = jsonName;
    this.minArgs = minArgs;
    this.maxArgs = maxArgs;
  }

  public String jsonName() {
    return jsonName;
  }

  public int minArgs() {
    return minArgs;
  }

  public int maxArgs() {
    return maxArgs;
  }

  /**
   * Operators whose purpose is to inspect absence. A parameter read beneath one of them is allowed
   * to be unset; anywhere else an unset parameter is a configuration gap (DECISIONS D2).
   */
  public boolean nullTolerant() {
    return this == IS_NULL || this == IS_PRESENT || this == COALESCE;
  }

  public static Optional<Operator> fromJson(String name) {
    return Optional.ofNullable(BY_NAME.get(name));
  }
}
