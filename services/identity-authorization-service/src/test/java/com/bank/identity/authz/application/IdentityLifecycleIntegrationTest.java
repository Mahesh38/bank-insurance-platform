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

  UUID admin;

  @BeforeEach
  void insurerAndAdministratorExist() {
    AuthzFixtures.insurer(jdbc, "ICICI_PRU");
    AuthzFixtures.insurer(jdbc, "HDFC_LIFE");
    admin = AuthzFixtures.activeUser(jdbc, "bank.admin.1", "BANK_EMPLOYEE", null, "BANK_ADMIN");
  }

  @Test
  void bankAdminCreatesPartnerDirectlyAndProvisioningIsQueuedAtCreation() {
    var created = partnerUsers.create(command("partner.lifecycle", "ICICI_PRU"), admin);

    // E5: no approval step; the user stays PENDING until the provider account exists.
    assertThat(created.status()).isEqualTo("PENDING");
    assertThat(created.approvalRequired()).isFalse();
    assertThat(created.approvalId()).isNull();
    assertThat(eventTypes(created.userId()))
        .containsExactlyInAnyOrder("IDENTITY_PROVISIONING_REQUESTED", "PARTNER_USER_CREATED");
    assertThat(
            jdbc.sql("select count(*) from approval_request where target_id = :userId")
                .param("userId", created.userId())
                .query(Long.class)
                .single())
        .isZero();
    var user = users.findById(created.userId()).orElseThrow();
    assertThat(user.getCreatedBy()).isEqualTo(admin.toString());
    assertThat(user.getProviderSubject()).isNull();
    assertThat(payload(created.userId(), "PARTNER_USER_CREATED"))
        .contains("\"createdBy\":\"" + admin + "\"")
        .contains("\"insurerCode\":\"ICICI_PRU\"")
        .contains("\"approvalRequired\":false");
    assertThat(payload(created.userId(), "IDENTITY_PROVISIONING_REQUESTED"))
        .contains("\"businessUserId\":\"" + created.userId() + "\"");
  }

  @Test
  void approveIsRefusedWhenApprovalIsNotRequired() {
    assertDenied(
        () -> partnerUsers.approve(UUID.randomUUID(), admin), ErrorCodes.ILLEGAL_TRANSITION);
  }

  @Test
  void duplicateUsernameIsAConflict() {
    partnerUsers.create(command("partner.duplicate", "ICICI_PRU"), admin);

    assertDenied(
        () -> partnerUsers.create(command("partner.duplicate", "ICICI_PRU"), admin),
        ErrorCodes.CONFLICT);
  }

  @Test
  void creatorWithoutPartnerUserCreateIsDenied() {
    UUID rm =
        AuthzFixtures.activeUser(jdbc, "rm.plain", "BANK_EMPLOYEE", null, "RELATIONSHIP_MANAGER");

    assertDenied(
        () -> partnerUsers.create(command("partner.denied", "ICICI_PRU"), rm),
        ErrorCodes.DEFAULT_DENY);
  }

  @Test
  void unknownCreatorIsDenied() {
    assertDenied(
        () -> partnerUsers.create(command("partner.unknown", "ICICI_PRU"), UUID.randomUUID()),
        ErrorCodes.DEFAULT_DENY);
  }

  @Test
  void insurerAdministratorCanNoLongerCreatePartnerUsers() {
    UUID iciciAdmin =
        AuthzFixtures.activeUser(
            jdbc, "icici.admin", "INSURER_REPRESENTATIVE", "ICICI_PRU", "PARTNER_ADMIN");

    // Tenant boundary still answers first for another insurer ...
    assertDenied(
        () -> partnerUsers.create(command("partner.cross", "HDFC_LIFE"), iciciAdmin),
        ErrorCodes.CROSS_INSURER_DENIED);
    // ... and for its own insurer the PDP refuses the action to any partner representative (E5).
    assertDenied(
        () -> partnerUsers.create(command("partner.own", "ICICI_PRU"), iciciAdmin),
        ErrorCodes.FORBIDDEN);
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

  private java.util.List<String> eventTypes(UUID userId) {
    return jdbc.sql("select event_type from outbox_event where aggregate_id = :userId")
        .param("userId", userId)
        .query(String.class)
        .list();
  }

  private String payload(UUID userId, String eventType) {
    return jdbc.sql(
            "select payload from outbox_event where aggregate_id = :userId and event_type = :type")
        .param("userId", userId)
        .param("type", eventType)
        .query(String.class)
        .single();
  }

  static PartnerUserAdministrationService.CreatePartnerUserCommand command(
      String username, String insurer) {
    return new PartnerUserAdministrationService.CreatePartnerUserCommand(
        username, username + "@example.test", "Partner", "User", insurer);
  }

  static void assertDenied(
      org.assertj.core.api.ThrowableAssert.ThrowingCallable call, String code) {
    assertThatThrownBy(call)
        .isInstanceOf(ServiceException.class)
        .satisfies(
            ex -> assertThat(((ServiceException) ex).getErrorResponse().getCode()).isEqualTo(code));
  }
}
