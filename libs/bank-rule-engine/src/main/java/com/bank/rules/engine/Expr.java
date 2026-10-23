package com.bank.rules.engine;

import java.util.List;
import java.util.Set;

/**
 * A parsed expression tree. Never a string that is evaluated (ADR-024 section 3): a configuration
 * author can compose the closed {@link Operator} set over inputs, derived values, parameters and
 * constants, and nothing else.
 */
public sealed interface Expr permits Expr.Var, Expr.Param, Expr.Const, Expr.Op {

  /** {@code {"var": name}} — an input or a derived value. */
  record Var(String name) implements Expr {}

  /** {@code {"param": "a.b.c"}} — a value from the rule set's {@code parameters} (dotted path). */
  record Param(String path) implements Expr {}

  /** {@code {"const": value}} — a literal; numbers are {@link java.math.BigDecimal}. */
  record Const(Object value) implements Expr {}

  /** {@code {"op": name, "args": [...]}}. */
  record Op(Operator op, List<Expr> args) implements Expr {
    public Op {
      args = List.copyOf(args);
    }
  }

  /**
   * Parameter paths this expression reads where an unset value is a configuration gap — that is,
   * outside {@code isNull}, {@code isPresent} and {@code coalesce}.
   */
  static void strictParams(Expr expr, Set<String> into) {
    switch (expr) {
      case Param p -> into.add(p.path());
      case Op o when !o.op().nullTolerant() -> o.args().forEach(a -> strictParams(a, into));
      default -> {
        // vars, constants and null-tolerant operators contribute nothing
      }
    }
  }

  /** Every variable this expression reads, used for derive-cycle detection. */
  static void vars(Expr expr, Set<String> into) {
    switch (expr) {
      case Var v -> into.add(v.name());
      case Op o -> o.args().forEach(a -> vars(a, into));
      default -> {
        // params and constants read no variables
      }
    }
  }
}
