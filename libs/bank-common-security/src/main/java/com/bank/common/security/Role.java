package com.bank.common.security;

/**
 * Roles recognised by the bank JWT token.
 * Service-specific permission checks should be layered on top of these coarse-grained roles.
 */
public enum Role {
    /** Bank relationship manager — can submit proposals and create quotes. */
    RM,
    /** Branch manager — supervisor access, can view all branch activity. */
    BRANCH_MANAGER,
    /** Operations team — can trigger manual retries and view job details. */
    OPS,
    /** Read-only auditor role. */
    AUDITOR,
    /** Internal service-to-service caller authenticated via mTLS. */
    SERVICE_ACCOUNT
}
