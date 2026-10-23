package com.bank.platform.consent.adapter.policy;

/** Reaches the package-visible statement switch from tests that live in another package. */
public final class StatementTestControl {

  private StatementTestControl() {}

  public static void setStatus(FixtureStatementSource source, String purpose, String status) {
    source.setStatus(purpose, status);
  }
}
