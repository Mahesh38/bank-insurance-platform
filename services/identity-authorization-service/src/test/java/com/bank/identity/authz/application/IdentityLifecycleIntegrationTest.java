package com.bank.identity.authz.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bank.common.error.ErrorCodes;
import com.bank.common.error.ServiceException;
import com.bank.identity.authz.domain.AuthorizationModels.DecisionRequest;
import com.bank.identity.authz.domain.AuthorizationModels.Resource;
import com.bank.identity.authz.domain.IdentityTypes.UserType;
import com.bank.identity.authz.persistence.BusinessUserRepository;
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

  @BeforeEach
  void insurerExists() {
    jdbc.sql(
            "insert into insurer(code, name, active) values ('ICICI_PRU', 'ICICI Prudential', true)")
        .update();
  }

  @Test
  void partnerUserRequiresDifferentCheckerAndCreatesProvisioningOutboxEvent() {
    var submitted =
        partnerUsers.submit(
            new PartnerUserAdministrationService.CreatePartnerUserCommand(
                "partner.lifecycle",
                "partner.lifecycle@example.test",
                "Partner",
                "Lifecycle",
                "ICICI_PRU"),
            "maker-1");

    assertThat(submitted.status()).isEqualTo("PENDING");
    assertThatThrownBy(() -> partnerUsers.approve(submitted.approvalId(), "maker-1"))
        .isInstanceOf(ServiceException.class)
        .satisfies(
            ex ->
                assertThat(((ServiceException) ex).getErrorResponse().getCode())
                    .isEqualTo(ErrorCodes.FORBIDDEN));

    String status = partnerUsers.approve(submitted.approvalId(), "checker-2");

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
}
