package com.bank.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "job_poll_attempt")
@Getter
@Setter
@NoArgsConstructor
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
}
