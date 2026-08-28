package com.bank.insurance.onesb.application;

import com.bank.common.audit.AuditActions;
import com.bank.common.audit.AuditEvent;
import com.bank.common.audit.AuditEventPublisher;
import com.bank.common.audit.AuditOutcomes;
import com.bank.common.error.ErrorCodes;
import com.bank.common.error.ServiceErrorResponse;
import com.bank.common.error.PlatformLayer;
import com.bank.common.error.ServiceException;
import com.bank.insurance.onesb.domain.command.CreatePaymentCommand;
import com.bank.insurance.onesb.domain.model.JobStatus;
import com.bank.insurance.onesb.domain.model.OneSbPaymentUrlResult;
import com.bank.insurance.onesb.domain.model.PaymentSession;
import com.bank.insurance.onesb.domain.model.PaymentStatus;
import com.bank.insurance.onesb.domain.model.QuoteJob;
import com.bank.insurance.onesb.domain.port.inbound.PaymentUseCase;
import com.bank.insurance.onesb.domain.port.outbound.JobStorePort;
import com.bank.insurance.onesb.domain.port.outbound.OneSbPaymentPort;
import com.bank.insurance.onesb.domain.port.outbound.PaymentSessionStorePort;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Payment orchestration (FUNC-007): payability check → 1SB payment-url call → persist session
 * → audit. No LOB handler — payment is LOB-agnostic (architecture §2 component diagram).
 */
@Service
public class PaymentService implements PaymentUseCase {

    private static final Set<JobStatus> PAYABLE_JOB_STATUSES = Set.of(JobStatus.COMPLETED, JobStatus.PARTIAL);

    private final JobStorePort jobStore;
    private final OneSbPaymentPort paymentPort;
    private final PaymentSessionStorePort sessionStore;
    private final AuditEventPublisher auditEventPublisher;

    public PaymentService(JobStorePort jobStore,
                          OneSbPaymentPort paymentPort,
                          PaymentSessionStorePort sessionStore,
                          AuditEventPublisher auditEventPublisher) {
        this.jobStore = jobStore;
        this.paymentPort = paymentPort;
        this.sessionStore = sessionStore;
        this.auditEventPublisher = auditEventPublisher;
    }

    @Override
    public PaymentSession createPaymentSession(CreatePaymentCommand command) {
        if (command.lob() == null) {
            throw validationError("lob is required");
        }
        if (!StringUtils.hasText(command.applicationNumber())) {
            throw validationError("applicationNumber is required");
        }

        if (StringUtils.hasText(command.jobId())) {
            assertPayable(command.jobId());
        }

        String actorId = StringUtils.hasText(command.actorId()) ? command.actorId() : "system";

        OneSbPaymentUrlResult upstream = paymentPort.createPaymentUrl(command);
        if (!upstream.paymentUrl().toLowerCase(Locale.ROOT).startsWith("https://")) {
            throw ServiceException.of(ErrorCodes.UPSTREAM_BAD_RESPONSE)
                    .service("onesb")
                    .layer(PlatformLayer.L5)
                    .component("PaymentService")
                    .operation("createPaymentUrl")
                    .upstream("1SB", null, null)
                    .reason("1SB returned a non-HTTPS payment URL")
                    .remediation("Do not forward the link. Raise with 1SB — a payment link must be TLS.")
                    .build();
        }

        PaymentSession session = sessionStore.createSession(
                command.jobId(),
                command.applicationNumber(),
                command.lob(),
                upstream.paymentUrl(),
                command.redirectUrl(),
                PaymentStatus.AWAITING_PAYMENT,
                upstream.externalTxnId(),
                upstream.expiresAt(),
                actorId
        );

        publishPaymentUrlRetrieved(command, session, actorId);
        return session;
    }

    private void assertPayable(String jobId) {
        Optional<QuoteJob> found = jobStore.findQuoteJob(jobId);
        if (found.isEmpty()) {
            throw notPayable("Proposal job not found: " + jobId);
        }
        QuoteJob job = found.get();
        if (!PAYABLE_JOB_STATUSES.contains(job.status()) || !StringUtils.hasText(job.applicationNumber())) {
            throw notPayable("Proposal job is not payable: " + jobId + " (status=" + job.status() + ")");
        }
    }

    private static ServiceException notPayable(String detail) {
        return new ServiceException(ServiceErrorResponse.builder()
                .title("Proposal Not Payable")
                .status(409)
                .detail(detail)
                .code(ErrorCodes.PROPOSAL_NOT_PAYABLE)
                .retryable(false)
                .build());
    }

    private static ServiceException validationError(String detail) {
        return new ServiceException(ServiceErrorResponse.builder()
                .title("Validation Failed")
                .status(422)
                .detail(detail)
                .code(ErrorCodes.VALIDATION_ERROR)
                .retryable(false)
                .build());
    }

    /** Audits a reference to the session only — {@code paymentUrl} itself is never logged/audited. */
    private void publishPaymentUrlRetrieved(CreatePaymentCommand command, PaymentSession session, String actorId) {
        try {
            AuditEvent event = AuditEvent.builder()
                    .actorId(actorId)
                    .actorType("USER")
                    .action(AuditActions.PAYMENT_URL_RETRIEVED)
                    .resourceType("PAYMENT_SESSION")
                    .resourceId(session.sessionId())
                    .outcome(AuditOutcomes.SUCCESS)
                    .lob(command.lob().name())
                    .journeyId(command.journeyId())
                    .metadata("paymentSessionId", session.sessionId())
                    .metadata("applicationNumber", session.applicationNumber())
                    .metadata("rmEmployeeId", command.rmEmployeeId())
                    .build();
            auditEventPublisher.publish(event);
        } catch (Exception ignored) {
            // audit must not break payment session creation
        }
    }
}
