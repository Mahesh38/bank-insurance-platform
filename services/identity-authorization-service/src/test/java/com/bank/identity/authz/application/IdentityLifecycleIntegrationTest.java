package com.bank.identity.authz.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bank.common.error.ErrorCodes;
import com.bank.common.error.ServiceException;
import com.bank.identity.authz.domain.AuthorizationModels.DecisionRequest;
import com.bank.identity.authz.domain.AuthorizationModels.Resource;
import com.bank.identity.authz.domain.IdentityTypes.UserType;
import com.bank.identity.authz.persistence.BusinessUserRepository;
import com.bank.identity.authz.support.AuthzFixtures;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class IdentityLifecycleIntegrationTest {

  @Autowired PartnerUserAdministrationService partnerUsers;
  @Autowired IdentityResolutionService identities;
  @Autowired BusinessUserRepository users;
  @Autowired JdbcClient jdbc;
  @Autowired AuthorizationDecisionService decisions;

  UUID maker;
  UUID checker;

  @BeforeEach
  void insurerAndAdministratorsExist() {
    AuthzFixtures.insurer(jdbc, "ICICI_PRU");
    AuthzFixtures.insurer(jdbc, "HDFC_LIFE");
    maker = AuthzFixtures.activeUser(jdbc, "bank.admin.1", "BANK_EMPLOYEE", null, "BANK_ADMIN");
    checker = AuthzFixtures.activeUser(jdbc, "bank.admin.2", "BANK_EMPLOYEE", null, "BANK_ADMIN");
  }

  @Test
  void partnerUserRequiresDifferentCheckerAndCreatesProvisioningOutboxEvent() {
    var submitted = partnerUsers.submit(command("partner.lifecycle", "ICICI_PRU"), maker);

    assertThat(submitted.status()).isEqualTo("PENDING");
    assertDenied(() -> partnerUsers.approve(submitted.approvalId(), maker), ErrorCodes.FORBIDDEN);

    String status = partnerUsers.approve(submitted.approvalId(), checker);

    // Approval does not activate the partner: it stays PENDING until provisioning completes.
    assertThat(status).isEqualTo("PENDING");
    assertThat(
            jdbc.sql("select count(*) from outbox_event where aggregate_id = :userId")
                .param("userId", submitted.userId())
                .query(Long.class)
                .single())
        .isEqualTo(1);
    assertThat(users.findById(submitted.userId()).orElseThrow().getProviderSubject()).isNull();
  }

  @Test
  void makerWithoutPartnerUserCreateIsDenied() {
    UUID rm =
        AuthzFixtures.activeUser(jdbc, "rm.plain", "BANK_EMPLOYEE", null, "RELATIONSHIP_MANAGER");

    assertDenied(
        () -> partnerUsers.submit(command("partner.denied", "ICICI_PRU"), rm),
        ErrorCodes.DEFAULT_DENY);
  }

  @Test
  void unknownMakerIsDenied() {
    assertDenied(
        () -> partnerUsers.submit(command("partner.unknown", "ICICI_PRU"), UUID.randomUUID()),
        ErrorCodes.DEFAULT_DENY);
  }

  @Test
  void checkerWithoutPartnerUserApproveIsDenied() {
    var submitted = partnerUsers.submit(command("partner.no.approve", "ICICI_PRU"), maker);
    UUID operations =
        AuthzFixtures.activeUser(jdbc, "ops.user", "BANK_EMPLOYEE", null, "OPERATIONS");

    assertDenied(
        () -> partnerUsers.approve(submitted.approvalId(), operations), ErrorCodes.DEFAULT_DENY);
  }

  @Test
  void partnerAdministratorIsHeldToTheirOwnInsurer() {
    UUID iciciAdmin =
        AuthzFixtures.activeUser(
            jdbc, "icici.admin", "INSURER_REPRESENTATIVE", "ICICI_PRU", "PARTNER_ADMIN");
    UUID hdfcAdmin =
        AuthzFixtures.activeUser(
            jdbc, "hdfc.admin", "INSURER_REPRESENTATIVE", "HDFC_LIFE", "PARTNER_ADMIN");

    assertDenied(
        () -> partnerUsers.submit(command("partner.cross", "HDFC_LIFE"), iciciAdmin),
        ErrorCodes.CROSS_INSURER_DENIED);

    var submitted = partnerUsers.submit(command("partner.own", "ICICI_PRU"), iciciAdmin);
    assertDenied(
        () -> partnerUsers.approve(submitted.approvalId(), hdfcAdmin),
        ErrorCodes.CROSS_INSURER_DENIED);
    assertThat(partnerUsers.approve(submitted.approvalId(), checker)).isEqualTo("PENDING");
  }

  @Test
  void bankEmployeeFromAdVerifyIsCreatedAgainstBankAdProvider() {
    var first =
        identities.resolve(
            new IdentityResolutionService.ResolveIdentityCommand(
                "BANK_AD",
                "EMP001",
                UserType.BANK_EMPLOYEE,
                "EMP001",
                "emp001@example.test",
                "EMP001"));
    var second =
        identities.resolve(
            new IdentityResolutionService.ResolveIdentityCommand(
                "BANK_AD",
                "EMP001",
                UserType.BANK_EMPLOYEE,
                "EMP001",
                "emp001.updated@example.test",
                "EMP001"));

    assertThat(second.getId()).isEqualTo(first.getId());
    assertThat(second.getProvider()).isEqualTo("BANK_AD");
    assertThat(second.getStatus().name()).isEqualTo("ACTIVE");
    assertThat(second.getUserType()).isEqualTo(UserType.BANK_EMPLOYEE);
    assertThat(second.getEmail()).isEqualTo("emp001.updated@example.test");
  }

  @Test
  void bankEmployeeIsCreatedJustInTimeAndThenRefreshedByStableProviderSubject() {
    var first =
        identities.resolve(
            new IdentityResolutionService.ResolveIdentityCommand(
                "KEYCLOAK",
                "bank-subject-1",
                UserType.BANK_EMPLOYEE,
                "rm.lifecycle",
                "old@example.test",
                "EMP-1"));
    var second =
        identities.resolve(
            new IdentityResolutionService.ResolveIdentityCommand(
                "KEYCLOAK",
                "bank-subject-1",
                UserType.BANK_EMPLOYEE,
                "rm.lifecycle",
                "new@example.test",
                "EMP-1"));

    assertThat(second.getId()).isEqualTo(first.getId());
    assertThat(second.getEmail()).isEqualTo("new@example.test");
    assertThat(second.getStatus().name()).isEqualTo("ACTIVE");
  }

  @Test
  void partnerCannotSelfRegisterDuringLoginResolution() {
    assertThatThrownBy(
            () ->
                identities.resolve(
                    new IdentityResolutionService.ResolveIdentityCommand(
                        "KEYCLOAK",
                        "unknown-partner",
                        UserType.INSURER_REPRESENTATIVE,
                        "unknown.partner",
                        "unknown@example.test",
                        null)))
        .isInstanceOf(ServiceException.class)
        .satisfies(
            ex ->
                assertThat(((ServiceException) ex).getErrorResponse().getCode())
                    .isEqualTo(ErrorCodes.FORBIDDEN));
  }

  @Test
  void unknownSubjectIsDeniedNotAServerError() {
    var decision =
        decisions.decide(
            new DecisionRequest(
                java.util.UUID.randomUUID(),
                "lead.create",
                new Resource("lead", null, null, null, null, null, true, false)));

    assertThat(decision.allowed()).isFalse();
    assertThat(decision.reasonCode()).isEqualTo("UNKNOWN_SUBJECT");
  }

  private static PartnerUserAdministrationService.CreatePartnerUserCommand command(
      String username, String insurer) {
    return new PartnerUserAdministrationService.CreatePartnerUserCommand(
        username, username + "@example.test", "Partner", "User", insurer);
  }

  private static void assertDenied(
      org.assertj.core.api.ThrowableAssert.ThrowingCallable call, String code) {
    assertThatThrownBy(call)
        .isInstanceOf(ServiceException.class)
        .satisfies(
            ex -> assertThat(((ServiceException) ex).getErrorResponse().getCode()).isEqualTo(code));
  }
}
