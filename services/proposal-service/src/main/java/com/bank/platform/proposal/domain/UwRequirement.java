package com.bank.platform.proposal.domain;

import java.time.LocalDate;

/** An insurer underwriting requirement; becomes a {@code proposal.uw_requirement} row. */
public record UwRequirement(
    String requirementId,
    String requirementType,
    String subType,
    String description,
    String status,
    LocalDate dueDate) {}
