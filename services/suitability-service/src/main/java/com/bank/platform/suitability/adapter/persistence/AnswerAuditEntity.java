package com.bank.platform.suitability.adapter.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "answer_audit", schema = "suitability")
public class AnswerAuditEntity {

  @Id
  @Column(name = "audit_id", length = 26)
  private String auditId;

  @Column(name = "assessment_id", nullable = false, length = 26)
  private String assessmentId;

  @Column(nullable = false, length = 40)
  private String screen;

  @Column(nullable = false, length = 8000)
  private String answers;

  @Column(name = "actor_id", nullable = false, length = 64)
  private String actorId;

  @Column(name = "recorded_at", nullable = false)
  private Instant recordedAt;

  public String getAuditId() {
    return auditId;
  }

  public void setAuditId(String auditId) {
    this.auditId = auditId;
  }

  public String getAssessmentId() {
    return assessmentId;
  }

  public void setAssessmentId(String assessmentId) {
    this.assessmentId = assessmentId;
  }

  public String getScreen() {
    return screen;
  }

  public void setScreen(String screen) {
    this.screen = screen;
  }

  public String getAnswers() {
    return answers;
  }

  public void setAnswers(String answers) {
    this.answers = answers;
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
