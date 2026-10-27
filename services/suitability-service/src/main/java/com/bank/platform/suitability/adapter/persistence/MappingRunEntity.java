package com.bank.platform.suitability.adapter.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "mapping_run", schema = "suitability")
public class MappingRunEntity {

  @Id
  @Column(name = "run_id", length = 26)
  private String runId;

  @Column(name = "assessment_id", nullable = false, length = 26)
  private String assessmentId;

  @Column(name = "run_no", nullable = false)
  private int runNo;

  @Column(name = "rule_version", nullable = false, length = 40)
  private String ruleVersion;

  @Column(nullable = false, length = 40)
  private String outcome;

  @Column(name = "actor_id", nullable = false, length = 64)
  private String actorId;

  @Column(name = "recorded_at", nullable = false)
  private Instant recordedAt;

  public String getRunId() {
    return runId;
  }

  public void setRunId(String runId) {
    this.runId = runId;
  }

  public String getAssessmentId() {
    return assessmentId;
  }

  public void setAssessmentId(String assessmentId) {
    this.assessmentId = assessmentId;
  }

  public int getRunNo() {
    return runNo;
  }

  public void setRunNo(int runNo) {
    this.runNo = runNo;
  }

  public String getRuleVersion() {
    return ruleVersion;
  }

  public void setRuleVersion(String ruleVersion) {
    this.ruleVersion = ruleVersion;
  }

  public String getOutcome() {
    return outcome;
  }

  public void setOutcome(String outcome) {
    this.outcome = outcome;
  }

  public String getActorId() {
    return actorId;
  }

  public void setActorId(String actorId) {
    this.actorId = actorId;
  }

  public Instant getRecordedAt() {
    return recordedAt;
  }

  public void setRecordedAt(Instant recordedAt) {
    this.recordedAt = recordedAt;
  }
}
