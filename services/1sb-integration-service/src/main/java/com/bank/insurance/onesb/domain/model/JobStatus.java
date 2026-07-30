package com.bank.insurance.onesb.domain.model;

/**
 * Job status lifecycle states for all integration job types.
 * <p>
 * Phase 2 (TECH-006 / PRODUCT-BACKLOG): {@code PENDING|RUNNING|COMPLETED|PARTIAL|FAILED|TIMEOUT}.
 * Renamed from arch draft {@code POLLING}/{@code COMPLETE} for backlog consistency.
 */
public enum JobStatus {
    PENDING,
    RUNNING,
    PARTIAL,
    COMPLETED,
    FAILED,
    TIMEOUT
}
