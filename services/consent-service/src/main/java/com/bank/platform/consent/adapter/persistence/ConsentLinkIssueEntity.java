package com.bank.platform.consent.adapter.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "consent_link_issue", schema = "consent")
public class ConsentLinkIssueEntity {

  @Id
  @Column(name = "issue_id", length = 26)
  private String issueId;

  @Column(name = "customer_id", nullable = false, length = 26)
  private String customerId;

  @Column(name = "assessment_id", nullable = false, length = 26)
  private String assessmentId;

  @Column(name = "issued_at", nullable = false)
  private Instant issuedAt;

  public String getIssueId() {
    return issueId;
  }

  public void setIssueId(String issueId) {
    this.issueId = issueId;
  }

  public String getCustomerId() {
    return customerId;
  }

  public void setCustomerId(String customerId) {
    this.customerId = customerId;
  }

  public String getAssessmentId() {
    return assessmentId;
  }

  public void setAssessmentId(String assessmentId) {
    this.assessmentId = assessmentId;
  }

  public Instant getIssuedAt() {
    return issuedAt;
  }

  public void setIssuedAt(Instant issuedAt) {
    this.issuedAt = issuedAt;
  }
}
