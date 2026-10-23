package com.bank.platform.consent.adapter.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "otp_issue", schema = "consent")
public class OtpIssueEntity {

  @Id
  @Column(name = "issue_id", length = 26)
  private String issueId;

  @Column(name = "customer_id", nullable = false, length = 26)
  private String customerId;

  @Column(nullable = false, length = 16)
  private String purpose;

  @Column(name = "issued_at", nullable = false)
  private Instant issuedAt;

  public void setIssueId(String issueId) {
    this.issueId = issueId;
  }

  public void setCustomerId(String customerId) {
    this.customerId = customerId;
  }

  public void setPurpose(String purpose) {
    this.purpose = purpose;
  }

  public void setIssuedAt(Instant issuedAt) {
    this.issuedAt = issuedAt;
  }
}
