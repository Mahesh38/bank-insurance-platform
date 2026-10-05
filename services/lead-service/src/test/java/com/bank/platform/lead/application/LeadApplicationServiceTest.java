package com.bank.platform.lead.application;

import com.bank.common.error.ErrorCodes;
import com.bank.common.error.PlatformLayer;
import com.bank.common.error.ServiceErrors;
import com.bank.common.error.ServiceException;
import com.bank.platform.lead.adapter.memory.InMemoryLeadRepository;
import com.bank.platform.lead.application.LeadApplicationService.AssignLeadCommand;
import com.bank.platform.lead.application.LeadApplicationService.CreateLeadCommand;
import com.bank.platform.lead.domain.ExceptionOutcome;
import com.bank.platform.lead.domain.Lead;
import com.bank.platform.lead.domain.LeadState;
import com.bank.platform.lead.domain.Ulid;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("unit")
@Tag("FUNC-030")
class LeadApplicationServiceTest {

    private LeadApplicationService service;

    @BeforeEach
    void setUp() {
        service = new LeadApplicationService(
            new InMemoryLeadRepository(),
            ServiceErrors.of("lead", PlatformLayer.L5),
            Clock.systemUTC());
    }

    @Test
    void createMintsUlidAndDoesNotEvaluateException() {
        Lead lead = service.create(new CreateLeadCommand("cust-1", "LIFE", "TERM", "BR-1", "rm-1"));

        assertThat(Ulid.isValid(lead.leadId())).isTrue();
        assertThat(lead.state()).isEqualTo(LeadState.NEW);
        assertThat(lead.exceptionOutcome()).isEqualTo(ExceptionOutcome.NOT_EVALUATED);
        assertThat(lead.journeyId()).isEqualTo(lead.leadId());
    }

    @Test
    void createConflictsOnUnfinishedDedupeKey() {
        service.create(new CreateLeadCommand("cust-1", "LIFE", "TERM", "BR-1", "rm-1"));

        assertThatThrownBy(() -> service.create(new CreateLeadCommand("cust-1", "LIFE", "TERM", "BR-1", "rm-1")))
            .isInstanceOf(ServiceException.class)
            .extracting(ex -> ((ServiceException) ex).getErrorResponse().getCode())
            .isEqualTo(ErrorCodes.CONFLICT);
    }

    @Test
    void assignRequiresOnboardingAndCertifiedSp() {
        Lead lead = service.create(new CreateLeadCommand("cust-1", "LIFE", "TERM", "BR-1", "rm-1"));

        assertThatThrownBy(() -> service.assign(new AssignLeadCommand(lead.leadId(), "sp-1", null, "rm-1")))
            .isInstanceOf(ServiceException.class)
            .extracting(ex -> ((ServiceException) ex).getErrorResponse().getCode())
            .isEqualTo(ErrorCodes.ILLEGAL_TRANSITION);

        service.startOnboarding(lead.leadId(), "rm-1");
        Lead assigned = service.assign(new AssignLeadCommand(lead.leadId(), "sp-1", "sp-1", "rm-1"));
        assertThat(assigned.state()).isEqualTo(LeadState.ASSIGNED);
        assertThat(assigned.accountableSpId()).isEqualTo("sp-1");
        assertThat(assigned.exceptionOutcome()).isEqualTo(ExceptionOutcome.PASS);
    }

    @Test
    void startOnboardingBlocksSyntheticBlockCustomer() {
        Lead lead = service.create(new CreateLeadCommand("cust-BLOCK-1", "LIFE", "TERM", "BR-1", "rm-1"));

        assertThatThrownBy(() -> service.startOnboarding(lead.leadId(), "rm-1"))
            .isInstanceOf(ServiceException.class)
            .extracting(ex -> ((ServiceException) ex).getErrorResponse().getCode())
            .isEqualTo(ErrorCodes.ILLEGAL_TRANSITION);
    }

    @Test
    void uncertifiedAssigneeIsRejected() {
        Lead lead = service.create(new CreateLeadCommand("cust-1", "LIFE", "TERM", "BR-1", "rm-1"));
        service.startOnboarding(lead.leadId(), "rm-1");

        assertThatThrownBy(() -> service.assign(
            new AssignLeadCommand(lead.leadId(), "UNCERTIFIED", null, "rm-1")))
            .isInstanceOf(ServiceException.class)
            .extracting(ex -> ((ServiceException) ex).getErrorResponse().getCode())
            .isEqualTo(ErrorCodes.SP_CERTIFICATION_REQUIRED);
    }

    @Test
    void foreignBookIsAbsentNotForbidden() {
        Lead lead = service.create(new CreateLeadCommand("cust-1", "LIFE", "TERM", "BR-1", "rm-1"));

        assertThatThrownBy(() -> service.getVisible(lead.leadId(), "other-rm"))
            .isInstanceOf(ServiceException.class)
            .extracting(ex -> ((ServiceException) ex).getErrorResponse().getCode())
            .isEqualTo(ErrorCodes.RESOURCE_NOT_FOUND);
    }
}
