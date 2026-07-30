package com.bank.common.security;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable representation of an authenticated bank actor (employee/RM).
 *
 * <p>Created by the JWT authentication filter from validated JWT claims.
 * Service code should obtain this from {@code SecurityContextHolder} via
 * {@link BankPrincipal#fromContext()}.
 *
 * <p>PII note: never log the full principal object — {@link #toString()} emits
 * only actorId to prevent leaking customerId or roles into logs.
 */
public final class BankPrincipal {

    private final String     actorId;
    private final String     employeeId;
    private final String     branchCode;
    private final Set<Role>  roles;
    private final String     customerId;

    private BankPrincipal(Builder b) {
        this.actorId    = Objects.requireNonNull(b.actorId,    "actorId must not be null");
        this.employeeId = Objects.requireNonNull(b.employeeId, "employeeId must not be null");
        this.branchCode = b.branchCode;
        this.roles      = Collections.unmodifiableSet(
            b.roles.isEmpty() ? EnumSet.noneOf(Role.class) : EnumSet.copyOf(b.roles));
        this.customerId = b.customerId;
    }

    public String    getActorId()    { return actorId; }
    public String    getEmployeeId() { return employeeId; }
    public String    getBranchCode() { return branchCode; }
    public Set<Role> getRoles()      { return roles; }
    public String    getCustomerId() { return customerId; }

    public boolean hasRole(Role role) {
        return roles.contains(role);
    }

    public boolean hasAnyRole(Role... requiredRoles) {
        for (Role r : requiredRoles) {
            if (roles.contains(r)) return true;
        }
        return false;
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String    actorId;
        private String    employeeId;
        private String    branchCode;
        private Set<Role> roles      = EnumSet.noneOf(Role.class);
        private String    customerId;

        private Builder() {}

        public Builder actorId(String actorId)       { this.actorId = actorId;         return this; }
        public Builder employeeId(String employeeId) { this.employeeId = employeeId;   return this; }
        public Builder branchCode(String branchCode) { this.branchCode = branchCode;   return this; }
        public Builder roles(Set<Role> roles)         { this.roles = EnumSet.copyOf(roles.isEmpty() ? EnumSet.noneOf(Role.class) : roles); return this; }
        public Builder customerId(String customerId) { this.customerId = customerId;   return this; }

        public BankPrincipal build() { return new BankPrincipal(this); }
    }

    /**
     * Returns only the actorId — never PII fields.
     */
    @Override
    public String toString() {
        return "BankPrincipal{actorId='" + actorId + "'}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BankPrincipal that)) return false;
        return Objects.equals(actorId, that.actorId)
            && Objects.equals(employeeId, that.employeeId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(actorId, employeeId);
    }
}
