package com.bank.identity.authz.application;

import static com.bank.identity.authz.application.IdentityLifecycleIntegrationTest.assertDenied;
import static com.bank.identity.authz.application.IdentityLifecycleIntegrationTest.command;
import static org.assertj.core.api.Assertions.assertThat;

import com.bank.common.error.ErrorCodes;
import com.bank.identity.authz.persistence.BusinessUserRepository;
import com.bank.identity.authz.support.AuthzFixtures;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

/**
 * E5 keeps the retired maker-checker flow behind {@code
 * identity.partner-onboarding.approval-required=true}; this proves it still works when re-enabled.
 */
@SpringBootTest(properties = "identity.partner-onboarding.approval-required=true")
@Transactional
class PartnerOnboardingApprovalRequiredTest {

  @Autowired PartnerUserAdministrationService partnerUsers;
  @Autowired BusinessUserRepository users;
  @Autowired JdbcClient jdbc;

  UUID maker;
  UUID checker;

  @BeforeEach
  void administratorsExist() {
    AuthzFixtures.insurer(jdbc, "ICICI_PRU");
    maker = AuthzFixtures.activeUser(jdbc, "bank.maker", "BANK_EMPLOYEE", null, "BANK_ADMIN");
    checker = AuthzFixtures.activeUser(jdbc, "bank.checker", "BANK_EMPLOYEE", null, "BANK_ADMIN");
  }

  @Test
  void creationWaitsForADifferentCheckerBeforeProvisioning() {
    var created = partnerUsers.create(command("partner.mc", "ICICI_PRU"), maker);

    assertThat(created.approvalRequired()).isTrue();
    assertThat(created.approvalId()).isNotNull();
    assertThat(provisioningEvents(created.userId())).isZero();
    assertDenied(() -> partnerUsers.approve(created.approvalId(), maker), ErrorCodes.FORBIDDEN);

    // Approval does not activate the partner: it stays PENDING until provisioning completes.
    assertThat(partnerUsers.approve(created.approvalId(), checker)).isEqualTo("PENDING");
    assertThat(provisioningEvents(created.userId())).isEqualTo(1);
    assertThat(users.findById(created.userId()).orElseThrow().getProviderSubject()).isNull();
    assertDenied(
        () -> partnerUsers.approve(created.approvalId(), checker), ErrorCodes.ILLEGAL_TRANSITION);
  }

  @Test
  void checkerWithoutPartnerUserApproveIsDenied() {
    var created = partnerUsers.create(command("partner.no.approve", "ICICI_PRU"), maker);
    UUID operations =
        AuthzFixtures.activeUser(jdbc, "ops.user", "BANK_EMPLOYEE", null, "OPERATIONS");

    assertDenied(
        () -> partnerUsers.approve(created.approvalId(), operations), ErrorCodes.DEFAULT_DENY);
  }

  @Test
  void insurerAdministratorCannotApprove() {
    var created = partnerUsers.create(command("partner.partner.checker", "ICICI_PRU"), maker);
    UUID iciciAdmin =
        AuthzFixtures.activeUser(
            jdbc, "icici.checker", "INSURER_REPRESENTATIVE", "ICICI_PRU", "PARTNER_ADMIN");

    assertDenied(
        () -> partnerUsers.approve(created.approvalId(), iciciAdmin), ErrorCodes.FORBIDDEN);
  }

  @Test
  void unknownApprovalIsNotFound() {
    assertDenied(
        () -> partnerUsers.approve(UUID.randomUUID(), checker), ErrorCodes.RESOURCE_NOT_FOUND);
  }

  private long provisioningEvents(UUID userId) {
    return jdbc.sql(
            """
            select count(*) from outbox_event
             where aggregate_id = :userId and event_type = 'IDENTITY_PROVISIONING_REQUESTED'
            """)
        .param("userId", userId)
        .query(Long.class)
        .single();
  }
}
