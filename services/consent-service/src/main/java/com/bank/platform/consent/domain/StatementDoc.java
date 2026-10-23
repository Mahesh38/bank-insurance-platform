package com.bank.platform.consent.domain;

/** A consent statement resolved from configuration. Text is stored verbatim with its hash. */
public record StatementDoc(
    String purpose,
    String statementId,
    String version,
    String text,
    String textHash,
    String status,
    String language) {

  public boolean approved() {
    return "APPROVED".equals(status);
  }

  public int major() {
    int dot = version.indexOf('.');
    String head = dot < 0 ? version : version.substring(0, dot);
    return Integer.parseInt(head);
  }
}
