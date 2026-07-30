package com.bank.common.security;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class RoleAndRequiresRoleTest {

    @Test
    void roleEnum_containsExpectedValues() {
        assertThat(Role.values()).containsExactlyInAnyOrder(
                Role.RM, Role.BRANCH_MANAGER, Role.OPS, Role.AUDITOR, Role.SERVICE_ACCOUNT);
        assertThat(Role.valueOf("RM")).isEqualTo(Role.RM);
    }

    @RequiresRole({Role.RM, Role.OPS})
    void annotatedMethod() {
        // marker for annotation reflection
    }

    @Test
    void requiresRole_annotation_readableAtRuntime() throws Exception {
        Method method = RoleAndRequiresRoleTest.class.getDeclaredMethod("annotatedMethod");
        RequiresRole annotation = method.getAnnotation(RequiresRole.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.value()).containsExactly(Role.RM, Role.OPS);
    }

    @Test
    void jwtClaims_areStable() {
        assertThat(JwtClaims.ACTOR_ID).isEqualTo("actor_id");
        assertThat(JwtClaims.EMPLOYEE_ID).isEqualTo("emp_id");
        assertThat(JwtClaims.BRANCH_CODE).isEqualTo("branch_code");
        assertThat(JwtClaims.ROLES).isEqualTo("roles");
        assertThat(JwtClaims.CUSTOMER_ID).isEqualTo("cust_id");
    }
}
