package com.bank.identity.authz.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.identity.authz.domain.AuthorizationModels.DecisionRequest;
import com.bank.identity.authz.domain.AuthorizationModels.Resource;
import com.bank.identity.authz.support.AuthzFixtures;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

/**
 * DECISIONS-2026-10-22 E5 / E6 against the Flyway-seeded catalogue: approvals and partner-user
 * creation belong to bank roles only, and the PDP refuses them to a partner representative even
 * when someone grants them explicitly.
 */
@SpringBootTest
@Transactional
class RolePermissionSeedTest {

  /** Insurer partner representative roles in the V2 catalogue. */
  private static final Set<String> PARTNER_ROLES =
      Set.of("PARTNER_SR", "PARTNER_MANAGER", "PARTNER_ADMIN");

  @Autowired JdbcClient jdbc;
  @Autowired AuthorizationDecisionService decisions;

  @Test
  void leadExceptionApproveIsSeededForBankAdminOnly() {
    assertThat(rolesHolding("lead.exception.approve")).containsExactly("BANK_ADMIN");
  }

  @Test
  void partnerUserCreationIsSeededForBankAdminOnly() {
    assertThat(rolesHolding("partner_user.create")).containsExactly("BANK_ADMIN");
    assertThat(rolesHolding("partner_user.bulk_import")).containsExactly("BANK_ADMIN");
    assertThat(rolesHolding("partner_user.approve")).containsExactly("BANK_ADMIN");
  }

  @Test
  void noPartnerRoleHoldsAnyApprovalOrPartnerUserCreation() {
    assertThat(
            jdbc.sql("select code from role where code like 'PARTNER%'").query(String.class).list())
        .containsExactlyInAnyOrderElementsOf(PARTNER_ROLES);
    List<String> held =
        jdbc.sql(
                """
                select role_code || ':' || permission_code from role_permission
                 where role_code like 'PARTNER%'
                   and (permission_code like '%.approve'
                        or permission_code in ('partner_user.create', 'partner_user.bulk_import'))
                """)
            .query(String.class)
            .list();
    assertThat(held).isEmpty();
  }

  @Test
  void partnerRepresentativeIsDeniedLeadExceptionApprovalEvenWithAnExplicitGrant() {
    AuthzFixtures.insurer(jdbc, "ICICI_PRU");
    UUID partner =
        AuthzFixtures.activeUser(
            jdbc, "icici.rep", "INSURER_REPRESENTATIVE", "ICICI_PRU", "PARTNER_ADMIN");
    grant(partner, "lead.exception.approve", "GLOBAL", null);
    grant(partner, "*", "GLOBAL", null);

    var decision =
        decisions.decide(
            new DecisionRequest(
                partner,
                "lead.exception.approve",
                new Resource("lead", "L1", null, "ICICI_PRU", null, Set.of(), true, false)));

    assertThat(decision.allowed()).isFalse();
    assertThat(decision.reasonCode()).isEqualTo("PARTNER_ACTION_PROHIBITED");
  }

  @Test
  void bankAdminIsAllowedLeadExceptionApproval() {
    UUID admin =
        AuthzFixtures.activeUser(jdbc, "bank.approver", "BANK_EMPLOYEE", null, "BANK_ADMIN");

    var decision =
        decisions.decide(
            new DecisionRequest(
                admin,
                "lead.exception.approve",
                new Resource("lead", "L1", null, null, null, Set.of(), false, false)));

    assertThat(decision.allowed()).isTrue();
    assertThat(decision.reasonCode()).isEqualTo("ROLE_GRANT");
  }

  private List<String> rolesHolding(String permission) {
    return jdbc.sql(
            "select role_code from role_permission where permission_code = :p order by role_code")
        .param("p", permission)
        .query(String.class)
        .list();
  }

  private void grant(UUID userId, String permission, String scopeType, String scopeValue) {
    Instant now = Instant.now();
    jdbc.sql(
            """
            insert into entitlement
                (id, user_id, effect, permission_code, scope_type, scope_value, reason,
                 valid_from, approved_by, created_at)
            values (:id, :userId, 'GRANT', :permission, :scopeType, :scopeValue, 'test grant',
                    :validFrom, 'test', :now)
            """)
        .param("id", UUID.randomUUID())
        .param("userId", userId)
        .param("permission", permission)
        .param("scopeType", scopeType)
        .param("scopeValue", scopeValue)
        .param("validFrom", now.minusSeconds(60))
        .param("now", now)
        .update();
  }
}
