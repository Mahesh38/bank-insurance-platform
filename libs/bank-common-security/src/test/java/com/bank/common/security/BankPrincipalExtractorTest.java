package com.bank.common.security;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

class BankPrincipalExtractorTest {

    @Test
    void extractsPrincipal_fromValidClaims() {
        var claims = Map.<String, Object>of(
            JwtClaims.ACTOR_ID,    "EMP001",
            JwtClaims.EMPLOYEE_ID, "EMP001",
            JwtClaims.BRANCH_CODE, "BLR01",
            JwtClaims.ROLES,       List.of("RM", "OPS")
        );

        BankPrincipal principal = BankPrincipalExtractor.fromClaims(claims);

        assertThat(principal.getActorId()).isEqualTo("EMP001");
        assertThat(principal.getEmployeeId()).isEqualTo("EMP001");
        assertThat(principal.getBranchCode()).isEqualTo("BLR01");
        assertThat(principal.getRoles()).containsExactlyInAnyOrder(Role.RM, Role.OPS);
        assertThat(principal.getCustomerId()).isNull();
    }

    @Test
    void extractsPrincipal_withCustomerId() {
        var claims = Map.<String, Object>of(
            JwtClaims.ACTOR_ID,    "EMP002",
            JwtClaims.EMPLOYEE_ID, "EMP002",
            JwtClaims.ROLES,       List.of("RM"),
            JwtClaims.CUSTOMER_ID, "CUST9999"
        );

        BankPrincipal principal = BankPrincipalExtractor.fromClaims(claims);

        assertThat(principal.getCustomerId()).isEqualTo("CUST9999");
    }

    @Test
    void throwsSecurityException_whenActorIdMissing() {
        var claims = Map.<String, Object>of(
            JwtClaims.EMPLOYEE_ID, "EMP003",
            JwtClaims.ROLES,       List.of()
        );

        assertThatThrownBy(() -> BankPrincipalExtractor.fromClaims(claims))
            .isInstanceOf(SecurityException.class)
            .hasMessageContaining(JwtClaims.ACTOR_ID);
    }

    @Test
    void unknownRolesAreDropped_silently() {
        var claims = Map.<String, Object>of(
            JwtClaims.ACTOR_ID,    "EMP004",
            JwtClaims.EMPLOYEE_ID, "EMP004",
            JwtClaims.ROLES,       List.of("RM", "SUPER_SECRET_ROLE")
        );

        BankPrincipal principal = BankPrincipalExtractor.fromClaims(claims);

        assertThat(principal.getRoles()).containsExactly(Role.RM);
    }

    @Test
    void hasRole_returnsTrue_whenRolePresent() {
        var principal = BankPrincipal.builder()
            .actorId("E1").employeeId("E1")
            .roles(EnumSet.of(Role.RM, Role.OPS))
            .build();

        assertThat(principal.hasRole(Role.RM)).isTrue();
        assertThat(principal.hasRole(Role.AUDITOR)).isFalse();
        assertThat(principal.hasAnyRole(Role.AUDITOR, Role.OPS)).isTrue();
        assertThat(principal.hasAnyRole(Role.AUDITOR, Role.BRANCH_MANAGER)).isFalse();
    }

    @Test
    void emptyRolesList_yieldsEmptyRoleSet() {
        var claims = Map.<String, Object>of(
            JwtClaims.ACTOR_ID,    "EMP006",
            JwtClaims.EMPLOYEE_ID, "EMP006",
            JwtClaims.ROLES,       List.of()
        );

        BankPrincipal principal = BankPrincipalExtractor.fromClaims(claims);

        assertThat(principal.getRoles()).isEmpty();
        assertThat(principal.hasAnyRole(Role.RM)).isFalse();
    }

    @Test
    void nonIterableRolesClaim_isIgnored() {
        var claims = Map.<String, Object>of(
            JwtClaims.ACTOR_ID,    "EMP007",
            JwtClaims.EMPLOYEE_ID, "EMP007",
            JwtClaims.ROLES,       "RM"
        );

        BankPrincipal principal = BankPrincipalExtractor.fromClaims(claims);

        assertThat(principal.getRoles()).isEmpty();
    }

    @Test
    void builder_acceptsEmptyRoles() {
        var principal = BankPrincipal.builder()
            .actorId("E2").employeeId("E2")
            .roles(Set.of())
            .build();

        assertThat(principal.getRoles()).isEmpty();
    }

    @Test
    void toString_doesNotLeakSensitiveFields() {
        var principal = BankPrincipal.builder()
            .actorId("EMP005").employeeId("EMP005")
            .branchCode("MUM99")
            .roles(EnumSet.of(Role.RM))
            .customerId("CUST_SENSITIVE")
            .build();

        String str = principal.toString();
        assertThat(str).contains("EMP005");
        assertThat(str).doesNotContain("CUST_SENSITIVE");
        assertThat(str).doesNotContain("MUM99");
    }
}
