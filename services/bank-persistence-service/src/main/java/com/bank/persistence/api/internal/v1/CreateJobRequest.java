package com.bank.persistence.api.internal.v1;

import jakarta.validation.constraints.NotBlank;

public record CreateJobRequest(
    @NotBlank String lob,
    @NotBlank String jobType,
    String journeyId,
    String idempotencyKey,
    @NotBlank String createdByActor) {}
