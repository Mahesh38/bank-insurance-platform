package com.bank.insurance.onesb.application;

import com.bank.common.audit.AuditActions;
import com.bank.common.audit.AuditEvent;
import com.bank.common.audit.AuditEventPublisher;
import com.bank.common.audit.AuditOutcomes;
import com.bank.common.error.ErrorCodes;
import com.bank.common.error.ServiceException;
import com.bank.insurance.onesb.domain.command.CreatePaymentCommand;
import com.bank.insurance.onesb.domain.model.JobStatus;
import com.bank.insurance.onesb.domain.model.Lob;
import com.bank.insurance.onesb.domain.model.OneSbPaymentUrlResult;
import com.bank.insurance.onesb.domain.model.PaymentSession;
import com.bank.insurance.onesb.domain.model.PaymentStatus;
import com.bank.insurance.onesb.domain.model.QuoteJob;
import com.bank.insurance.onesb.domain.port.outbound.JobStorePort;
import com.bank.insurance.onesb.domain.port.outbound.OneSbPaymentPort;
import com.bank.insurance.onesb.domain.port.outbound.PaymentSessionStorePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("FUNC-007")
class PaymentServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-04T10:00:00Z");

    @Mock JobStorePort jobStore;
    @Mock OneSbPaymentPort paymentPort;
    @Mock PaymentSessionStorePort sessionStore;
    @Mock AuditEventPublisher auditEventPublisher;

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(jobStore, paymentPort, sessionStore, auditEventPublisher);
    }

    @Test
    void createPaymentSession_happyPath_returnsSessionAndAuditsUrlRetrieved() {
        when(paymentPort.createPaymentUrl(any()))
                .thenReturn(new OneSbPaymentUrlResult("https://pay.example.com/abc", "TXN-1", NOW.plusSeconds(1800)));
        PaymentSession stored = new PaymentSession(
                "sess-1", null, "APP-123", Lob.TERM,
                "https://pay.example.com/abc", "https://bank.com/return",
                PaymentStatus.AWAITING_PAYMENT, "TXN-1", NOW, NOW.plusSeconds(1800));
        when(sessionStore.createSession(
                eq(null), eq("APP-123"), eq(Lob.TERM), eq("https://pay.example.com/abc"),
                eq("https://bank.com/return"), eq(PaymentStatus.AWAITING_PAYMENT), eq("TXN-1"),
                eq(NOW.plusSeconds(1800)), eq("actor-1")))
                .thenReturn(stored);

        PaymentSession result = paymentService.createPaymentSession(command(null));

        assertThat(result).isSameAs(stored);

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditEventPublisher).publish(captor.capture());
        AuditEvent event = captor.getValue();
        assertThat(event.getAction()).isEqualTo(AuditActions.PAYMENT_URL_RETRIEVED);
        assertThat(event.getOutcome()).isEqualTo(AuditOutcomes.SUCCESS);
        assertThat(event.getMetadata()).containsEntry("paymentSessionId", "sess-1");
        assertThat(event.getMetadata()).containsEntry("applicationNumber", "APP-123");
        // paymentUrl is a ref only — must never be audited/logged (AC)
        assertThat(event.getMetadata().values()).noneMatch(v -> v != null && v.contains("pay.example.com"));
    }

    @Test
    void createPaymentSession_jobNotCompleted_throwsNotPayable_noOneSbCall() {
        when(jobStore.findQuoteJob("job-pending")).thenReturn(Optional.of(new QuoteJob(
                "job-pending", JobStatus.RUNNING, null, Lob.TERM, "j-1",
                List.of(), List.of(), NOW, null, null)));

        assertThatThrownBy(() -> paymentService.createPaymentSession(command("job-pending")))
                .isInstanceOf(ServiceException.class)
                .satisfies(ex -> {
                    ServiceException se = (ServiceException) ex;
                    assertThat(se.getHttpStatus()).isEqualTo(409);
                    assertThat(se.getErrorResponse().getCode()).isEqualTo(ErrorCodes.PROPOSAL_NOT_PAYABLE);
                });

        verify(paymentPort, never()).createPaymentUrl(any());
        verify(sessionStore, never()).createSession(
                any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void createPaymentSession_jobIdUnknown_throwsNotPayable() {
        when(jobStore.findQuoteJob("job-missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.createPaymentSession(command("job-missing")))
                .isInstanceOf(ServiceException.class)
                .satisfies(ex -> assertThat(((ServiceException) ex).getErrorResponse().getCode())
                        .isEqualTo(ErrorCodes.PROPOSAL_NOT_PAYABLE));

        verify(paymentPort, never()).createPaymentUrl(any());
    }

    @Test
    void createPaymentSession_jobCompletedWithApplicationNumber_isPayable() {
        when(jobStore.findQuoteJob("job-done")).thenReturn(Optional.of(new QuoteJob(
                "job-done", JobStatus.COMPLETED, null, Lob.TERM, "j-1",
                List.of(), List.of(), NOW, NOW, "APP-123")));
        when(paymentPort.createPaymentUrl(any()))
                .thenReturn(new OneSbPaymentUrlResult("https://pay.example.com/abc", "TXN-1", NOW.plusSeconds(1800)));
        when(sessionStore.createSession(
                anyString(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PaymentSession(
                        "sess-2", "job-done", "APP-123", Lob.TERM,
                        "https://pay.example.com/abc", "https://bank.com/return",
                        PaymentStatus.AWAITING_PAYMENT, "TXN-1", NOW, NOW.plusSeconds(1800)));

        PaymentSession result = paymentService.createPaymentSession(command("job-done"));

        assertThat(result.sessionId()).isEqualTo("sess-2");
    }

    @Test
    void createPaymentSession_nonHttpsPaymentUrl_throwsUpstreamBadResponse() {
        when(paymentPort.createPaymentUrl(any()))
                .thenReturn(new OneSbPaymentUrlResult("http://insecure.example.com/pay", "TXN-1", NOW.plusSeconds(1800)));

        assertThatThrownBy(() -> paymentService.createPaymentSession(command(null)))
                .isInstanceOf(ServiceException.class)
                .satisfies(ex -> {
                    ServiceException se = (ServiceException) ex;
                    assertThat(se.getHttpStatus()).isEqualTo(502);
                    assertThat(se.getErrorResponse().getCode()).isEqualTo(ErrorCodes.UPSTREAM_BAD_RESPONSE);
                    assertThat(se.isRetryable()).isTrue();
                });

        verify(sessionStore, never()).createSession(
                any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void createPaymentSession_missingLob_throwsValidation_noOneSbCall() {
        CreatePaymentCommand command = new CreatePaymentCommand(
                null, "APP-1", null, "HDFC", "T1", "https://bank.com/return", "j-1", "INR",
                new CreatePaymentCommand.Payer("A", "B", "9876543210", "a@b.com"),
                new BigDecimal("1000"), "YEARLY", "E1", "AGT-1", "actor-1");

        assertThatThrownBy(() -> paymentService.createPaymentSession(command))
                .isInstanceOf(ServiceException.class)
                .satisfies(ex -> {
                    ServiceException se = (ServiceException) ex;
                    assertThat(se.getHttpStatus()).isEqualTo(422);
                    assertThat(se.getErrorResponse().getCode()).isEqualTo(ErrorCodes.VALIDATION_ERROR);
                });

        verify(paymentPort, never()).createPaymentUrl(any());
    }

    private static CreatePaymentCommand command(String jobId) {
        return new CreatePaymentCommand(
                Lob.TERM, "APP-123", jobId, "HDFC", "T1", "https://bank.com/return", "j-1", "INR",
                new CreatePaymentCommand.Payer("Anil", "Kumar", "9876543210", "anil@example.com"),
                new BigDecimal("12300"), "YEARLY", "E123", "AGT-1", "actor-1");
    }
}
