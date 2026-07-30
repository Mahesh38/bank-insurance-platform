package com.bank.insurance.onesb.application;

import com.bank.common.audit.AuditActions;
import com.bank.common.audit.AuditEvent;
import com.bank.common.audit.AuditEventPublisher;
import com.bank.common.audit.AuditOutcomes;
import com.bank.common.error.ErrorCodes;
import com.bank.common.error.ServiceException;
import com.bank.common.secrets.SecretProvider;
import com.bank.insurance.onesb.domain.command.CreatePaymentCommand;
import com.bank.insurance.onesb.domain.model.JobStatus;
import com.bank.insurance.onesb.domain.model.Lob;
import com.bank.insurance.onesb.domain.model.PaymentSession;
import com.bank.insurance.onesb.domain.model.PaymentStatus;
import com.bank.insurance.onesb.domain.model.QuoteJob;
import com.bank.insurance.onesb.domain.port.outbound.JobStorePort;
import com.bank.insurance.onesb.domain.port.outbound.OneSbPaymentPort;
import com.bank.insurance.onesb.domain.port.outbound.PaymentSessionStorePort;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

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

    @Mock JobStorePort jobStore;
    @Mock OneSbPaymentPort paymentPort;
    @Mock PaymentSessionStorePort paymentSessionStore;
    @Mock AuditEventPublisher auditEventPublisher;
    @Mock SecretProvider secretProvider;

    private PaymentService paymentService;
    private ListAppender<ILoggingEvent> logAppender;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(
                jobStore, paymentPort, paymentSessionStore,
                auditEventPublisher, secretProvider);

        Logger logger = (Logger) LoggerFactory.getLogger(PaymentService.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        logger.addAppender(logAppender);
    }

    @Test
    @Tag("COMP-004")
    void createPayment_happyPath_returnsHttpsUrl_audits_andLogsRefOnly() {
        String paymentUrl = "https://payments.test/pay?token=SECRET-TOKEN-XYZ";
        PaymentSession upstream = session("pay-ref-1", null, paymentUrl);
        when(paymentPort.createPaymentSession(eq(null), eq("APP-1"), eq("TERM"),
                eq("https://bank.test/callback"), eq("rm-1")))
                .thenReturn(upstream);
        when(paymentSessionStore.save(any(), eq("rm-1"))).thenReturn("pay-ref-1");
        when(secretProvider.getDistributorId()).thenReturn("TEST_DIST");

        PaymentSession result = paymentService.createPayment(command("APP-1", null, "rm-1"));

        assertThat(result.sessionId()).isEqualTo("pay-ref-1");
        assertThat(result.paymentUrl()).isEqualTo(paymentUrl);
        assertThat(result.expiresAt()).isNotNull();

        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditEventPublisher).publish(captor.capture());
        AuditEvent audit = captor.getValue();
        assertThat(audit.getAction()).isEqualTo(AuditActions.PAYMENT_URL_RETRIEVED);
        assertThat(audit.getOutcome()).isEqualTo(AuditOutcomes.SUCCESS);
        assertThat(audit.getResourceId()).isEqualTo("pay-ref-1");
        assertThat(audit.getDistributorId()).isEqualTo("TEST_DIST");
        assertThat(audit.getMetadata()).containsEntry("paymentRef", "pay-ref-1");
        assertThat(audit.getMetadata().values().stream().map(Object::toString))
                .noneMatch(v -> v.contains(paymentUrl) || v.contains("SECRET-TOKEN"));

        List<String> logMessages = logAppender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .toList();
        assertThat(logMessages).isNotEmpty();
        assertThat(logMessages).anyMatch(m -> m.contains("pay-ref-1"));
        assertThat(logMessages).noneMatch(m -> m.contains(paymentUrl) || m.contains("SECRET-TOKEN"));
    }

    @Test
    void createPayment_withCompletedProposalJob_usesJobIdAndLob() {
        QuoteJob job = new QuoteJob(
                "job-pay-1", JobStatus.COMPLETED, null, Lob.TERM, "j-1",
                List.of(), List.of(), Instant.now(), Instant.now(), "APP-2");
        when(jobStore.findQuoteJob("job-pay-1")).thenReturn(Optional.of(job));
        when(paymentPort.createPaymentSession(eq("job-pay-1"), eq("APP-2"), eq("TERM"),
                anyString(), eq("rm-2")))
                .thenReturn(session("pay-2", "job-pay-1", "https://pay.test/ok"));
        when(paymentSessionStore.save(any(), eq("rm-2"))).thenReturn("pay-2");
        when(secretProvider.getDistributorId()).thenReturn("TEST_DIST");

        PaymentSession result = paymentService.createPayment(command("APP-2", "job-pay-1", "rm-2"));

        assertThat(result.sessionId()).isEqualTo("pay-2");
        verify(paymentPort).createPaymentSession("job-pay-1", "APP-2", "TERM",
                "https://bank.test/callback", "rm-2");
    }

    @Test
    void createPayment_pendingProposalJob_returns409ProposalNotPayable() {
        QuoteJob job = new QuoteJob(
                "job-pending", JobStatus.PENDING, null, Lob.TERM, "j-1",
                List.of(), List.of(), Instant.now(), null, null);
        when(jobStore.findQuoteJob("job-pending")).thenReturn(Optional.of(job));

        assertThatThrownBy(() -> paymentService.createPayment(command("APP-3", "job-pending", "rm")))
                .isInstanceOf(ServiceException.class)
                .satisfies(ex -> {
                    ServiceException se = (ServiceException) ex;
                    assertThat(se.getErrorResponse().getStatus()).isEqualTo(409);
                    assertThat(se.getErrorResponse().getCode())
                            .isEqualTo(ErrorCodes.PROPOSAL_NOT_PAYABLE);
                });

        verify(paymentPort, never()).createPaymentSession(any(), any(), any(), any(), any());
        verify(paymentSessionStore, never()).save(any(), any());
    }

    @Test
    void createPayment_missingProposalJob_returns409() {
        when(jobStore.findQuoteJob("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.createPayment(command("APP-4", "missing", "rm")))
                .isInstanceOf(ServiceException.class)
                .satisfies(ex -> assertThat(((ServiceException) ex).getErrorResponse().getCode())
                        .isEqualTo(ErrorCodes.PROPOSAL_NOT_PAYABLE));
    }

    @Test
    void createPayment_httpPaymentUrl_rejected() {
        when(paymentPort.createPaymentSession(any(), any(), any(), any(), any()))
                .thenReturn(session("pay-http", null, "http://insecure.test/pay"));

        assertThatThrownBy(() -> paymentService.createPayment(command("APP-5", null, "rm")))
                .isInstanceOf(ServiceException.class)
                .satisfies(ex -> assertThat(((ServiceException) ex).getErrorResponse().getCode())
                        .isEqualTo(ErrorCodes.UPSTREAM_BAD_RESPONSE));

        verify(paymentSessionStore, never()).save(any(), any());
    }

    @Test
    void createPayment_blankApplicationNumber_returns422() {
        assertThatThrownBy(() -> paymentService.createPayment(
                new CreatePaymentCommand("  ", null, null, null, null, "idem", "rm")))
                .isInstanceOf(ServiceException.class)
                .satisfies(ex -> assertThat(((ServiceException) ex).getErrorResponse().getCode())
                        .isEqualTo(ErrorCodes.VALIDATION_ERROR));
    }

    private static CreatePaymentCommand command(String appNo, String proposalJobId, String actorId) {
        return new CreatePaymentCommand(
                appNo, proposalJobId, "https://bank.test/callback", Lob.TERM, "j-pay", "idem", actorId);
    }

    private static PaymentSession session(String ref, String jobId, String url) {
        return new PaymentSession(
                ref, jobId, "APP", Lob.TERM, url,
                "https://bank.test/callback", PaymentStatus.AWAITING_PAYMENT,
                ref, Instant.now(), Instant.now().plusSeconds(600));
    }
}
