package com.bank.platform.consent.adapter.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class StatementKey implements Serializable {

  @Column(name = "statement_id", nullable = false, length = 64)
  private String statementId;

  @Column(name = "version", nullable = false, length = 16)
  private String version;

  @Column(name = "language", nullable = false, length = 8)
  private String language;

  protected StatementKey() {}

  public StatementKey(String statementId, String version, String language) {
    this.statementId = statementId;
    this.version = version;
    this.language = language;
  }

  public String statementId() {
    return statementId;
  }

  public String version() {
    return version;
  }

  public String language() {
    return language;
  }

  @Override
  public boolean equals(Object other) {
    if (!(other instanceof StatementKey key)) {
      return false;
    }
    return Objects.equals(statementId, key.statementId)
        && Objects.equals(version, key.version)
        && Objects.equals(language, key.language);
  }

  @Override
  public int hashCode() {
    return Objects.hash(statementId, version, language);
  }
}
