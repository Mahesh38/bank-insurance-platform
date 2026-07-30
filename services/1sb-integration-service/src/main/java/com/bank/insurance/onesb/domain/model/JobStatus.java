package com.bank.insurance.onesb.domain.model;

/**
 * Job status lifecycle states for all integration job types.
 */
public enum JobStatus {
    PENDING,
    POLLING,
    PARTIAL,
    COMPLETE,
    FAILED
}
