package com.bank.common.security;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Stateless helper that creates a {@link BankPrincipal} from a validated JWT claims map.
 *
 * <p>The consuming service provides the actual JWT validation (e.g. via
 * Spring Security OAuth2 Resource Server); this extractor only interprets
 * the already-validated claims.
 *
 * <p>Design rationale: keeping this logic in the shared lib ensures all services
 * parse claims identically. Services wire this into their
 * {@code JwtAuthenticationConverter}.
 */
public final class BankPrincipalExtractor {

    private BankPrincipalExtractor() {}

    /**
     * Builds a {@link BankPrincipal} from a validated JWT claims map.
     *
     * @param claims non-null map of JWT claims
     * @return populated principal
     * @throws SecurityException if required claims ({@code actor_id}, {@code emp_id}) are absent
     */
    @SuppressWarnings("unchecked")
    public static BankPrincipal fromClaims(Map<String, Object> claims) {
        var actorId    = requiredString(claims, JwtClaims.ACTOR_ID);
        var employeeId = requiredString(claims, JwtClaims.EMPLOYEE_ID);
        var branchCode = (String) claims.get(JwtClaims.BRANCH_CODE);
        var customerId = (String) claims.get(JwtClaims.CUSTOMER_ID);

        Set<Role> roles = Set.of();
        Object rawRoles = claims.get(JwtClaims.ROLES);
        if (rawRoles instanceof Iterable<?> iterable) {
            roles = ((Iterable<String>) iterable).iterator().hasNext()
                ? parseRoles((Iterable<String>) iterable)
                : Set.of();
        }

        return BankPrincipal.builder()
            .actorId(actorId)
            .employeeId(employeeId)
            .branchCode(branchCode)
            .roles(roles)
            .customerId(customerId)
            .build();
    }

    private static String requiredString(Map<String, Object> claims, String key) {
        Object value = claims.get(key);
        if (value == null) {
            throw new SecurityException("Missing required JWT claim: " + key);
        }
        return value.toString();
    }

    private static Set<Role> parseRoles(Iterable<String> rawRoles) {
        Set<Role> roles = new java.util.HashSet<>();
        for (String raw : rawRoles) {
            try {
                roles.add(Role.valueOf(raw.toUpperCase()));
            } catch (IllegalArgumentException ignored) {
                // unknown role names are silently dropped
            }
        }
        return roles;
    }
}
