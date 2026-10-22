package com.bank.platform.adminconfig.domain;

/** A lifecycle rule of ADR-024 section 5 refused the change. The API maps {@link Kind} to a code. */
public class RuleSetLifecycleException extends RuntimeException {

  /** Why the change was refused. */
  public enum Kind {
    /** The version is not in a status that allows this step (includes any change once PUBLISHED). */
    ILLEGAL_TRANSITION,
    /** The checker is the maker (section 5 point 2). */
    MAKER_CHECKER,
    /** The checker does not hold the role the classification requires. */
    ROLE_REQUIRED,
    /** Rejection needs remarks. */
    REMARKS_REQUIRED
  }

  private final Kind kind;

  public RuleSetLifecycleException(Kind kind, String message) {
    super(message);
    this.kind = kind;
  }

  public Kind kind() {
    return kind;
  }
}
