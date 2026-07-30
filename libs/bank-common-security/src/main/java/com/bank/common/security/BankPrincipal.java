package com.bank.common.security;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;
import lombok.Value;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Immutable representation of an authenticated bank actor (employee/RM).
 *
 * <p>Created by the JWT authentication filter from validated JWT claims.
 * Service code should obtain this from {@code SecurityContextHolder}.
 *
 * <p>PII note: never log the full principal object — {@link #toString()} emits
 * only actorId to prevent leaking customerId or roles into logs.
 */
@Value
@Builder
@EqualsAndHashCode(of = {"actorId", "employeeId"})
@ToString(of = "actorId")
public class BankPrincipal {

    @NonNull String    actorId;
    @NonNull String    employeeId;
    String             branchCode;
    Set<Role>          roles;
    String             customerId;

    public boolean hasRole(Role role) {
        return roles.contains(role);
    }

    public boolean hasAnyRole(Role... requiredRoles) {
        for (Role r : requiredRoles) {
            if (roles.contains(r)) return true;
        }
        return false;
    }

    public static class BankPrincipalBuilder {
        private Set<Role> roles = EnumSet.noneOf(Role.class);

        public BankPrincipalBuilder roles(Set<Role> roles) {
            this.roles = EnumSet.copyOf(
                roles.isEmpty() ? EnumSet.noneOf(Role.class) : roles);
            return this;
        }

        public BankPrincipal build() {
            Set<Role> resolvedRoles = Collections.unmodifiableSet(
                roles == null || roles.isEmpty()
                    ? EnumSet.noneOf(Role.class)
                    : EnumSet.copyOf(roles));
            return new BankPrincipal(actorId, employeeId, branchCode, resolvedRoles, customerId);
        }
    }
}
