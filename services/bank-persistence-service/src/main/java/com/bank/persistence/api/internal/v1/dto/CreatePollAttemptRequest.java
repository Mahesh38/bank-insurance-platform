package com.bank.persistence.api.internal.v1.dto;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record CreatePollAttemptRequest(
        @NotNull Short attemptNumber,
        Instant attemptedAt,
        Short httpStatus,
        Boolean isComplete,
        Integer durationMs,
        String errorMessage
) {}
