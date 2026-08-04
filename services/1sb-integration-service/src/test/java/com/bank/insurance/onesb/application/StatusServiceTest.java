package com.bank.insurance.onesb.application;

import com.bank.common.audit.AuditActions;
import com.bank.common.audit.AuditEvent;
import com.bank.common.audit.AuditEventPublisher;
import com.bank.common.error.ErrorCodes;
import com.bank.common.error.ServiceException;
import com.bank.insurance.onesb.domain.model.ApplicationStatus;
import com.bank.insurance.onesb.domain.model.BankApplicationStatus;
import com.bank.insurance.onesb.domain.model.Lob;
import com.bank.insurance.onesb.domain.model.OneSbApplicationStatusResult;
import com.bank.insurance.onesb.domain.port.outbound.OneSbStatusPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("FUNC-009")
class StatusServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-04T10:00:00Z");

    @Mock OneSbStatusPort statusPort;
    @Mock AuditEventPublisher auditEventPublisher;

    private StatusService statusService;

    @BeforeEach
    void setUp() {
        statusService = new StatusService(statusPort, auditEventPublisher, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void getStatus_happyPath_normalisesAndAudits_omitsRawStatusFromNothingButAuditsIt() {
        when(statusPort.getStatus("APP-1", "HDFC", "T1"))
                .thenReturn(Optional.of(new OneSbApplicationStatusResult(
                        "APP-1", "REQUIREMENTS_PENDING", "Income proof pending", null)));

        ApplicationStatus result = statusService.getStatus("APP-1", Lob.TERM, "HDFC", "T1", "actor-1");

        assertThat(result.bankStatus()).isEqualTo(BankApplicationStatus.UW_REQUIREMENTS);
        assertThat(result.manufacturerSubStatus()).isEqualTo("Income proof pending");
        assertThat(result.lastUpdated()).isEqualTo(NOW);

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditEventPublisher).publish(captor.capture());
        assertThat(captor.getValue().getAction()).isEqualTo(AuditActions.APPLICATION_STATUS_CHECKED);
        assertThat(captor.getValue().getMetadata()).containsEntry("rawStatus", "REQUIREMENTS_PENDING");
        assertThat(captor.getValue().getMetadata()).containsEntry("bankStatus", "UW_REQUIREMENTS");
    }

    @Test
    void getStatus_notFound_throws404() {
        when(statusPort.getStatus("APP-missing", "HDFC", null)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> statusService.getStatus("APP-missing", Lob.TERM, "HDFC", null, "actor-1"))
                .isInstanceOf(ServiceException.class)
                .satisfies(ex -> {
                    ServiceException se = (ServiceException) ex;
                    assertThat(se.getHttpStatus()).isEqualTo(404);
                    assertThat(se.getErrorResponse().getCode()).isEqualTo(ErrorCodes.RESOURCE_NOT_FOUND);
                });
    }

    @Test
    void getStatus_missingInsurerCode_throwsValidation_noOneSbCall() {
        assertThatThrownBy(() -> statusService.getStatus("APP-1", Lob.TERM, null, null, "actor-1"))
                .isInstanceOf(ServiceException.class)
                .satisfies(ex -> assertThat(((ServiceException) ex).getErrorResponse().getCode())
                        .isEqualTo(ErrorCodes.VALIDATION_ERROR));
    }

    @Test
    void getStatus_missingLob_throwsValidation() {
        assertThatThrownBy(() -> statusService.getStatus("APP-1", null, "HDFC", null, "actor-1"))
                .isInstanceOf(ServiceException.class)
                .satisfies(ex -> assertThat(((ServiceException) ex).getErrorResponse().getCode())
                        .isEqualTo(ErrorCodes.VALIDATION_ERROR));
    }

    @ParameterizedTest
    @CsvSource({
            "QUOTE_CREATED,QUOTING",
            "QUOTE_SELECTED,QUOTING",
            "PROPOSAL_SUBMITTED,PROPOSAL",
            "PROPOSAL_ERROR,PROPOSAL",
            "REQUIREMENTS_PENDING,UW_REQUIREMENTS",
            "DOCUMENTS_UPLOADED,UW_REQUIREMENTS",
            "OTP_VERIFIED,UW_REQUIREMENTS",
            "KYC_FAILED,UW_REQUIREMENTS",
            "UNDERWRITING,UW_IN_PROGRESS",
            "SCRUTINY,UW_IN_PROGRESS",
            "ACCEPTED,UW_IN_PROGRESS",
            "COUNTER_OFFER,CUSTOMER_DECISION",
            "AWAITING_CLIENT_APPROVAL,CUSTOMER_DECISION",
            "PAYMENT_INITIATED,PAYMENT",
            "PAYMENT_SUCCESS,PAYMENT",
            "POLICY_ISSUED,ISSUED",
            "REJECTED,CLOSED",
            "CANCELLED,CLOSED",
            "SOME_UNKNOWN_STATUS,UNKNOWN",
    })
    void normalise_mapsPerFieldGuideTable(String raw, BankApplicationStatus expected) {
        assertThat(StatusService.normalise(raw)).isEqualTo(expected);
    }

    @Test
    void normalise_blank_returnsUnknown() {
        assertThat(StatusService.normalise(null)).isEqualTo(BankApplicationStatus.UNKNOWN);
        assertThat(StatusService.normalise("")).isEqualTo(BankApplicationStatus.UNKNOWN);
    }
}
