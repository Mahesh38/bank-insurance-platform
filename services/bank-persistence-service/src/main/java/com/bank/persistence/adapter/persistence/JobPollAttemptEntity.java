package com.bank.persistence.adapter.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "job_poll_attempt")
public class JobPollAttemptEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "attempt_id")
  private Long attemptId;

  @Column(name = "job_id", length = 36, nullable = false)
  private String jobId;

  @Column(name = "attempt_number", nullable = false)
  private Short attemptNumber;

  @Column(name = "attempted_at", nullable = false)
  private Instant attemptedAt;

  @Column(name = "http_status")
  private Short httpStatus;

  @Column(name = "is_complete")
  private Boolean isComplete;

  @Column(name = "duration_ms")
  private Integer durationMs;

  @Column(name = "error_message", length = 500)
  private String errorMessage;

  public JobPollAttemptEntity() {}

  public Long getAttemptId() {
    return attemptId;
  }

  public void setAttemptId(Long attemptId) {
    this.attemptId = attemptId;
  }

  public String getJobId() {
    return jobId;
  }

  public void setJobId(String jobId) {
    this.jobId = jobId;
  }

  public Short getAttemptNumber() {
    return attemptNumber;
  }

  public void setAttemptNumber(Short attemptNumber) {
    this.attemptNumber = attemptNumber;
  }

  public Instant getAttemptedAt() {
    return attemptedAt;
  }

  public void setAttemptedAt(Instant attemptedAt) {
    this.attemptedAt = attemptedAt;
  }

  public Short getHttpStatus() {
    return httpStatus;
  }

  public void setHttpStatus(Short httpStatus) {
    this.httpStatus = httpStatus;
  }

  public Boolean getIsComplete() {
    return isComplete;
  }

  public void setIsComplete(Boolean isComplete) {
    this.isComplete = isComplete;
  }

  public Integer getDurationMs() {
    return durationMs;
  }

  public void setDurationMs(Integer durationMs) {
    this.durationMs = durationMs;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }
}
