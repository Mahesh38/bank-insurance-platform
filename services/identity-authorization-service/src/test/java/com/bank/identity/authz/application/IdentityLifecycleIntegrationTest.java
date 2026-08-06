package com.bank.identity.authz.application;

import com.bank.identity.authz.domain.IdentityTypes.UserType;
import com.bank.identity.authz.persistence.BusinessUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class IdentityLifecycleIntegrationTest {

    @Autowired PartnerUserAdministrationService partnerUsers;
    @Autowired IdentityResolutionService identities;
    @Autowired BusinessUserRepository users;
    @Autowired JdbcClient jdbc;

    @BeforeEach
    void insurerExists() {
        jdbc.sql("insert into insurer(code, name, active) values ('ICICI_PRU', 'ICICI Prudential', true)")
            .update();
    }

    @Test
    void partnerUserRequiresDifferentCheckerAndCreatesProvisioningOutboxEvent() {
        var submitted = partnerUsers.submit(new PartnerUserAdministrationService.CreatePartnerUserCommand(
            "partner.lifecycle", "partner.lifecycle@example.test", "Partner", "Lifecycle", "ICICI_PRU"), "maker-1");

        assertThat(submitted.status()).isEqualTo("PENDING");
        assertThatThrownBy(() -> partnerUsers.approve(submitted.approvalId(), "maker-1"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Maker cannot approve");

        String status = partnerUsers.approve(submitted.approvalId(), "checker-2");

        assertThat(status).isEqualTo("PENDING");
        assertThat(jdbc.sql("select count(*) from outbox_event where aggregate_id = :userId")
            .param("userId", submitted.userId()).query(Long.class).single()).isEqualTo(1);
        assertThat(users.findById(submitted.userId()).orElseThrow().getProviderSubject()).isNull();
    }

    @Test
    void bankEmployeeIsCreatedJustInTimeAndThenRefreshedByStableProviderSubject() {
        var first = identities.resolve(new IdentityResolutionService.ResolveIdentityCommand(
            "KEYCLOAK", "bank-subject-1", UserType.BANK_EMPLOYEE, "rm.lifecycle",
            "old@example.test", "EMP-1"));
        var second = identities.resolve(new IdentityResolutionService.ResolveIdentityCommand(
            "KEYCLOAK", "bank-subject-1", UserType.BANK_EMPLOYEE, "rm.lifecycle",
            "new@example.test", "EMP-1"));

        assertThat(second.getId()).isEqualTo(first.getId());
        assertThat(second.getEmail()).isEqualTo("new@example.test");
        assertThat(second.getStatus().name()).isEqualTo("ACTIVE");
    }

    @Test
    void partnerCannotSelfRegisterDuringLoginResolution() {
        assertThatThrownBy(() -> identities.resolve(new IdentityResolutionService.ResolveIdentityCommand(
            "KEYCLOAK", "unknown-partner", UserType.INSURER_REPRESENTATIVE, "unknown.partner",
            "unknown@example.test", null)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("must be approved");
    }
}
