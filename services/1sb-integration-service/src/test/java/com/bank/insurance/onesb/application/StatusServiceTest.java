package com.bank.insurance.onesb.application;

import com.bank.common.audit.AuditActions;
import com.bank.common.audit.AuditEvent;
import com.bank.common.audit.AuditEventPublisher;
import com.bank.common.audit.AuditOutcomes;
import com.bank.common.error.ErrorCodes;
import com.bank.common.error.ServiceException;
import com.bank.common.secrets.SecretProvider;
import com.bank.insurance.onesb.domain.command.GetApplicationStatusCommand;
import com.bank.insurance.onesb.domain.model.ApplicationStatus;
import com.bank.insurance.onesb.domain.model.BankStage;
import com.bank.insurance.onesb.domain.model.Lob;
import com.bank.insurance.onesb.domain.port.outbound.OneSbStatusPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("FUNC-009")
class StatusServiceTest {

    @Mock OneSbStatusPort statusPort;
    @Mock AuditEventPublisher auditEventPublisher;
    @Mock SecretProvider secretProvider;

    private StatusService statusService;

    @BeforeEach
    void setUp() {
        statusService = new StatusService(statusPort, auditEventPublisher, secretProvider);
    }

    @Test
    @Tag("COMP-004")
    void getStatus_happyPath_normalisesAndAudits() {
        Instant updated = Instant.parse("2026-07-30T12:00:00Z");
        when(statusPort.fetchStatus("APP-1", "TERM", "XYZ", null))
                .thenReturn(new ApplicationStatus(
                        "APP-1",
                        BankStage.UNKNOWN,
                        "REQUIREMENTS_PENDING",
                        "MFR_PENDING",
                        "Awaiting docs",
                        null,
                        "XYZ",
                        Lob.TERM,
                        updated));
        when(secretProvider.getDistributorId()).thenReturn("TEST_DIST");

        ApplicationStatus result = statusService.getStatus(command("APP-1", "TERM", "XYZ", null));

        assertThat(result.bankStatus()).isEqualTo(BankStage.UW_REQUIREMENTS);
        assertThat(result.onesbStatus()).isEqualTo("REQUIREMENTS_PENDING");
        assertThat(result.manufacturerAppStatus()).isEqualTo("MFR_PENDING");
        assertThat(result.manufacturerAppStatusDesc()).isEqualTo("Awaiting docs");

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditEventPublisher).publish(captor.capture());
        AuditEvent audit = captor.getValue();
        assertThat(audit.getAction()).isEqualTo(AuditActions.APPLICATION_STATUS_CHECKED);
        assertThat(audit.getOutcome()).isEqualTo(AuditOutcomes.SUCCESS);
        assertThat(audit.getDistributorId()).isEqualTo("TEST_DIST");
        assertThat(audit.getMetadata()).containsEntry("applicationNumber", "APP-1");
        assertThat(audit.getMetadata()).containsEntry("bankStatus", "UW_REQUIREMENTS");
        assertThat(audit.getMetadata()).containsEntry("distributorId", "TEST_DIST");
    }

    @Test
    void getStatus_blankOnesbStatus_returns404() {
        when(statusPort.fetchStatus(eq("APP-MISSING"), eq("TERM"), eq("XYZ"), any()))
                .thenReturn(new ApplicationStatus(
                        "APP-MISSING", BankStage.UNKNOWN, "  ", null, null, null,
                        "XYZ", Lob.TERM, Instant.now()));

        assertThatThrownBy(() -> statusService.getStatus(command("APP-MISSING", "TERM", "XYZ", null)))
                .isInstanceOf(ServiceException.class)
                .satisfies(ex -> {
                    ServiceException se = (ServiceException) ex;
                    assertThat(se.getErrorResponse().getStatus()).isEqualTo(404);
                    assertThat(se.getErrorResponse().getCode()).isEqualTo(ErrorCodes.RESOURCE_NOT_FOUND);
                });
        verifyNoInteractions(auditEventPublisher);
    }

    @Test
    void getStatus_nullFromPort_returns404() {
        when(statusPort.fetchStatus("APP-NULL", "TERM", "XYZ", null)).thenReturn(null);

        assertThatThrownBy(() -> statusService.getStatus(command("APP-NULL", "TERM", "XYZ", null)))
                .isInstanceOf(ServiceException.class)
                .satisfies(ex -> assertThat(((ServiceException) ex).getErrorResponse().getCode())
                        .isEqualTo(ErrorCodes.RESOURCE_NOT_FOUND));
    }

    @Test
    void getStatus_missingRequiredFields_returns422() {
        assertThatThrownBy(() -> statusService.getStatus(
                new GetApplicationStatusCommand("  ", null, "", null, "rm", null)))
                .isInstanceOf(ServiceException.class)
                .satisfies(ex -> {
                    ServiceException se = (ServiceException) ex;
                    assertThat(se.getErrorResponse().getStatus()).isEqualTo(422);
                    assertThat(se.getErrorResponse().getCode()).isEqualTo(ErrorCodes.VALIDATION_ERROR);
                    assertThat(se.getErrorResponse().getErrors()).hasSize(3);
                });
        verifyNoInteractions(statusPort);
    }

    @Test
    void getStatus_auditFailure_doesNotPropagate() {
        when(statusPort.fetchStatus("APP-2", "TERM", "XYZ", "POL-1"))
                .thenReturn(new ApplicationStatus(
                        "APP-2", BankStage.UNKNOWN, "POLICY_ISSUED", "ISSUED", "Done",
                        "POL-1", "XYZ", Lob.TERM, Instant.now()));
        when(secretProvider.getDistributorId()).thenReturn("TEST_DIST");
        doThrow(new RuntimeException("audit down")).when(auditEventPublisher).publish(any());

        ApplicationStatus result = statusService.getStatus(command("APP-2", "TERM", "XYZ", "POL-1"));

        assertThat(result.bankStatus()).isEqualTo(BankStage.ISSUED);
        assertThat(result.policyNumber()).isEqualTo("POL-1");
    }

    private static GetApplicationStatusCommand command(String app, String lob, String company,
                                                       String policyNo) {
        return new GetApplicationStatusCommand(app, lob, company, policyNo, "rm-1", "j-1");
    }
}
