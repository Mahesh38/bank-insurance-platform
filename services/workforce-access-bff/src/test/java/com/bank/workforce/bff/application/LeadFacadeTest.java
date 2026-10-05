package com.bank.workforce.bff.application;

import com.bank.workforce.bff.lead.LeadGateway;
import com.bank.workforce.bff.lead.LeadGateway.AssignLeadCommand;
import com.bank.workforce.bff.lead.LeadGateway.LeadRecord;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("unit")
@Tag("FUNC-029")
@ExtendWith(MockitoExtension.class)
class LeadFacadeTest {

    @Mock
    LeadGateway gateway;

    @Test
    void assignEvaluatesThenAssignsWhenPass() {
        LeadFacade facade = new LeadFacade(gateway);
        LeadRecord unevaluated = record("NOT_EVALUATED", false, false, "NEW");
        LeadRecord passed = record("PASS", false, false, "NEW");
        LeadRecord assigned = record("PASS", false, false, "ASSIGNED");
        when(gateway.get("L1", "rm-1")).thenReturn(unevaluated);
        when(gateway.startOnboarding("L1", "rm-1")).thenReturn(passed);
        when(gateway.assign(any())).thenReturn(assigned);

        LeadRecord result = facade.assign("L1", "SP-1001", "SP-1001", "rm-1");

        assertThat(result.state()).isEqualTo("ASSIGNED");
        verify(gateway).startOnboarding("L1", "rm-1");
        verify(gateway).assign(new AssignLeadCommand("L1", "SP-1001", "SP-1001", "rm-1"));
    }

    @Test
    void assignReturnsExceptionRequiredWithoutMutatingWhenHold() {
        LeadFacade facade = new LeadFacade(gateway);
        LeadRecord unevaluated = record("NOT_EVALUATED", false, false, "NEW");
        LeadRecord hold = record("APPROVAL_REQUIRED", true, true, "NEW");
        when(gateway.get("L1", "rm-1")).thenReturn(unevaluated);
        when(gateway.startOnboarding("L1", "rm-1")).thenReturn(hold);

        LeadRecord result = facade.assign("L1", "SP-1001", null, "rm-1");

        assertThat(result.exceptionRequired()).isTrue();
        verify(gateway, never()).assign(any());
    }

    private static LeadRecord record(String outcome, boolean hold, boolean required, String state) {
        Instant now = Instant.parse("2026-10-05T12:00:00Z");
        return new LeadRecord(
            "L1", "cust-1", "LIFE", "TERM", "BR-MUM-001", state, "NEW",
            "rm-1", null, hold, outcome, required, "L1", now, now);
    }
}
