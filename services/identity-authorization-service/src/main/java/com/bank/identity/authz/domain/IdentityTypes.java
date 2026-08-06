package com.bank.identity.authz.domain;

public final class IdentityTypes {
    private IdentityTypes() {}

    public enum UserType {
        BANK_EMPLOYEE,
        INSURER_REPRESENTATIVE
    }

    public enum UserStatus {
        PENDING,
        ACTIVE,
        SUSPENDED,
        DISABLED,
        EXPIRED
    }

    public enum EntitlementEffect {
        GRANT,
        DENY
    }

    public enum ScopeType {
        GLOBAL,
        BRANCH,
        INSURER,
        RESOURCE
    }
}
