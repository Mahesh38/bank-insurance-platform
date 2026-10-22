package com.bank.platform.consent.adapter.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "retention_anchor")
public class RetentionAnchorEntity {

  @Id
  @Column(name = "root_consent_id", nullable = false, length = 26)
  private String rootConsentId;

  @Column(name = "terminal_at", nullable = false)
  private Instant terminalAt;

  @Column(name = "retain_until", nullable = false)
  private Instant retainUntil;

  @Column(name = "anchored_at", nullable = false)
  private Instant anchoredAt;

  protected RetentionAnchorEntity() {}

  public RetentionAnchorEntity(
      String rootConsentId, Instant terminalAt, Instant retainUntil, Instant anchoredAt) {
    this.rootConsentId = rootConsentId;
    this.terminalAt = terminalAt;
    this.retainUntil = retainUntil;
    this.anchoredAt = anchoredAt;
  }

  public Instant terminalAt() {
    return terminalAt;
  }

  public Instant retainUntil() {
    return retainUntil;
  }
}
