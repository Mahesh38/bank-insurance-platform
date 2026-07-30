package com.bank.persistence.api.internal.v1.dto;

import java.time.Instant;

public record PollAttemptResponse(
        Long attemptId,
        String jobId,
        short attemptNumber,
        Instant attemptedAt,
        Short httpStatus,
        Boolean isComplete,
        Integer durationMs,
        String errorMessage
) {}
