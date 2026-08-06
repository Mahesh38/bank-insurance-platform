package com.bank.identity.authz.persistence;

import com.bank.identity.authz.domain.AuthorizationModels.Entitlement;
import com.bank.identity.authz.domain.AuthorizationModels.Facts;
import com.bank.identity.authz.domain.AuthorizationModels.Subject;
import com.bank.identity.authz.domain.IdentityTypes.EntitlementEffect;
import com.bank.identity.authz.domain.IdentityTypes.ScopeType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Repository
public class AuthorizationFactsRepository {

    private final BusinessUserRepository users;
    private final JdbcClient jdbc;

    public AuthorizationFactsRepository(BusinessUserRepository users, JdbcClient jdbc) {
        this.users = users;
        this.jdbc = jdbc;
    }

    public Facts load(UUID userId, Instant now) {
        BusinessUserEntity user = users.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("Unknown business user"));
        Set<String> rolePermissions = Set.copyOf(jdbc.sql("""
                select distinct rp.permission_code
                  from user_role ur
                  join role_permission rp on rp.role_code = ur.role_code
                 where ur.user_id = :userId
                   and ur.valid_from <= :now
                   and (ur.valid_until is null or ur.valid_until > :now)
                """)
            .param("userId", userId)
            .param("now", now)
            .query(String.class)
            .list());
        List<Entitlement> entitlements = jdbc.sql("""
                select effect, permission_code, scope_type, scope_value
                  from entitlement
                 where user_id = :userId
                   and valid_from <= :now
                   and (valid_until is null or valid_until > :now)
                """)
            .param("userId", userId)
            .param("now", now)
            .query((rs, rowNum) -> new Entitlement(
                EntitlementEffect.valueOf(rs.getString("effect")),
                rs.getString("permission_code"),
                ScopeType.valueOf(rs.getString("scope_type")),
                rs.getString("scope_value")))
            .list();
        Set<String> branches = Set.copyOf(jdbc.sql("""
                select branch_code
                  from user_branch_assignment
                 where user_id = :userId
                   and valid_from <= :now
                   and (valid_until is null or valid_until > :now)
                """)
            .param("userId", userId)
            .param("now", now)
            .query(String.class)
            .list());
        boolean validCertification = jdbc.sql("""
                select count(*)
                  from certification
                 where user_id = :userId
                   and status = 'VALID'
                   and mandatory_for_selling = true
                   and valid_from <= :now
                   and (valid_until is null or valid_until > :now)
                """)
            .param("userId", userId)
            .param("now", now)
            .query(Long.class)
            .single() > 0;
        return new Facts(
            new Subject(user.getId(), user.getUserType(), user.getStatus(), user.getInsurerCode(),
                user.getPolicyVersion(), user.getValidFrom(), user.getValidUntil()),
            rolePermissions,
            entitlements,
            branches,
            validCertification
        );
    }
}
