package com.bank.identity.authz.support;

import java.time.Instant;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;

/** Seeds active business users with a role, for maker / checker PDP tests. */
public final class AuthzFixtures {

  private AuthzFixtures() {}

  public static UUID activeUser(
      JdbcClient jdbc, String username, String userType, String insurerCode, String role) {
    UUID id = UUID.randomUUID();
    Instant now = Instant.now();
    jdbc.sql(
            """
            insert into business_user
                (id, provider, provider_subject, user_type, status, username, insurer_code,
                 policy_version, valid_from, created_at, updated_at, version)
            values (:id, 'KEYCLOAK', :subject, :userType, 'ACTIVE', :username, :insurer,
                    1, :validFrom, :now, :now, 0)
            """)
        .param("id", id)
        .param("subject", "subject-" + id)
        .param("userType", userType)
        .param("username", username)
        .param("insurer", insurerCode)
        .param("validFrom", now.minusSeconds(60))
        .param("now", now)
        .update();
    if (role != null) {
      jdbc.sql(
              """
              insert into user_role (id, user_id, role_code, valid_from, assigned_by, created_at)
              values (:id, :userId, :role, :validFrom, 'test', :now)
              """)
          .param("id", UUID.randomUUID())
          .param("userId", id)
          .param("role", role)
          .param("validFrom", now.minusSeconds(60))
          .param("now", now)
          .update();
    }
    return id;
  }

  public static void insurer(JdbcClient jdbc, String code) {
    // Idempotent: non-transactional tests share the in-memory database with the others.
    jdbc.sql(
            """
            insert into insurer(code, name, active)
            select :code, :code, true
             where not exists (select 1 from insurer where code = :code)
            """)
        .param("code", code)
        .update();
  }
}
