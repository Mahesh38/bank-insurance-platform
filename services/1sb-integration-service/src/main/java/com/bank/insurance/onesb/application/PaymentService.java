package com.bank.insurance.onesb.application;

import com.bank.common.audit.AuditActions;
import com.bank.common.audit.AuditEvent;
import com.bank.common.audit.AuditEventPublisher;
import com.bank.common.audit.AuditOutcomes;
import com.bank.common.error.ErrorCodes;
import com.bank.common.error.ServiceError;
import com.bank.common.error.ServiceErrorResponse;
import com.bank.common.error.ServiceException;
import com.bank.common.secrets.SecretProvider;
import com.bank.insurance.onesb.domain.command.CreatePaymentCommand;
import com.bank.insurance.onesb.domain.model.JobStatus;
import com.bank.insurance.onesb.domain.model.Lob;
import com.bank.insurance.onesb.domain.model.PaymentSession;
import com.bank.insurance.onesb.domain.model.QuoteJob;
import com.bank.insurance.onesb.domain.port.inbound.PaymentUseCase;
import com.bank.insurance.onesb.domain.port.outbound.JobStorePort;
import com.bank.insurance.onesb.domain.port.outbound.OneSbPaymentPort;
import com.bank.insurance.onesb.domain.port.outbound.PaymentSessionStorePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Payment orchestration (FUNC-007): optional proposal payable gate → 1SB payment URL →
 * HTTPS validate → persist session → audit. Logs {@code paymentRef} only — never full URL.
 */
@Service
public class PaymentService implements PaymentUseCase {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final JobStorePort jobStore;
    private final OneSbPaymentPort paymentPort;
    private final PaymentSessionStorePort paymentSessionStore;
    private final AuditEventPublisher auditEventPublisher;
    private final SecretProvider secretProvider;

    public PaymentService(JobStorePort jobStore,
                          OneSbPaymentPort paymentPort,
                          PaymentSessionStorePort paymentSessionStore,
                          AuditEventPublisher auditEventPublisher,
                          SecretProvider secretProvider) {
        this.jobStore = jobStore;
        this.paymentPort = paymentPort;
        this.paymentSessionStore = paymentSessionStore;
        this.auditEventPublisher = auditEventPublisher;
        this.secretProvider = secretProvider;
    }

    @Override
    public PaymentSession createPayment(CreatePaymentCommand command) {
        if (!StringUtils.hasText(command.applicationNumber())) {
            throw new ServiceException(ServiceErrorResponse.builder()
                    .title("Validation Failed")
                    .status(422)
                    .detail("applicationNumber is required")
                    .code(ErrorCodes.VALIDATION_ERROR)
                    .retryable(false)
                    .errors(List.of(ServiceError.ofField(
                            ErrorCodes.MISSING_REQUIRED_FIELD,
                            "applicationNumber is required",
                            "applicationNumber")))
                    .build());
        }
        if (!StringUtils.hasText(command.redirectUrl())) {
            throw new ServiceException(ServiceErrorResponse.builder()
                    .title("Validation Failed")
                    .status(422)
                    .detail("redirectUrl is required")
                    .code(ErrorCodes.VALIDATION_ERROR)
                    .retryable(false)
                    .errors(List.of(ServiceError.ofField(
                            ErrorCodes.MISSING_REQUIRED_FIELD,
                            "redirectUrl is required",
                            "redirectUrl")))
                    .build());
        }

        String actorId = StringUtils.hasText(command.actorId()) ? command.actorId() : "system";
        String redirectUrl = command.redirectUrl().trim();

        String jobId = null;
        Lob lob = command.lob() != null ? command.lob() : Lob.TERM;

        if (StringUtils.hasText(command.proposalJobId())) {
            QuoteJob job = assertProposalPayable(command.proposalJobId(), command.applicationNumber());
            jobId = job.jobId();
            if (job.lob() != null) {
                lob = job.lob();
            }
        }

        PaymentSession session = paymentPort.createPaymentSession(
                jobId,
                command.applicationNumber().trim(),
                lob.name(),
                redirectUrl,
                actorId
        );

        assertHttpsPaymentUrl(session.paymentUrl());

        String paymentRef = paymentSessionStore.save(session, actorId);
        PaymentSession stored = withRef(session, paymentRef);

        // Never log paymentUrl — paymentRef only (FUNC-007 AC-2)
        log.info("Payment session created paymentRef={} applicationNumber={}",
                paymentRef, command.applicationNumber());

        publishPaymentUrlRetrieved(command, stored, actorId);
        return stored;
    }

    private QuoteJob assertProposalPayable(String proposalJobId, String applicationNumber) {
        Optional<QuoteJob> found = jobStore.findQuoteJob(proposalJobId);
        if (found.isEmpty()) {
            throw proposalNotPayable("Proposal job not found or not payable: " + proposalJobId);
        }
        QuoteJob job = found.get();
        if (job.status() != JobStatus.COMPLETED) {
            throw proposalNotPayable(
                    "Proposal job is not payable (status=" + job.status() + "): " + proposalJobId);
        }
        if (StringUtils.hasText(job.applicationNumber())
                && StringUtils.hasText(applicationNumber)
                && !job.applicationNumber().equalsIgnoreCase(applicationNumber.trim())) {
            throw proposalNotPayable(
                    "applicationNumber does not match proposal job: " + proposalJobId);
        }
        return job;
    }

    static void assertHttpsPaymentUrl(String paymentUrl) {
        if (!StringUtils.hasText(paymentUrl)) {
            throw badPaymentUrl("paymentUrl is blank");
        }
        String trimmed = paymentUrl.trim();
        if (!trimmed.toLowerCase(Locale.ROOT).startsWith("https://")) {
            throw badPaymentUrl("paymentUrl must be HTTPS");
        }
        try {
            URI uri = URI.create(trimmed);
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null) {
                throw badPaymentUrl("paymentUrl must be a valid HTTPS URL");
            }
        } catch (IllegalArgumentException ex) {
            throw badPaymentUrl("paymentUrl is not a valid URI");
        }
    }

    private void publishPaymentUrlRetrieved(CreatePaymentCommand command,
                                            PaymentSession session,
                                            String actorId) {
        try {
            String distributorId = secretProvider.getDistributorId();
            AuditEvent event = AuditEvent.builder()
                    .actorId(actorId)
                    .actorType("USER")
                    .action(AuditActions.PAYMENT_URL_RETRIEVED)
                    .resourceType("PAYMENT_SESSION")
                    .resourceId(session.sessionId())
                    .outcome(AuditOutcomes.SUCCESS)
                    .lob(session.lob() != null ? session.lob().name() : null)
                    .journeyId(command.journeyId())
                    .distributorId(distributorId)
                    .metadata("paymentRef", session.sessionId())
                    .metadata("applicationNumber", command.applicationNumber())
                    .build();
            auditEventPublisher.publish(event);
        } catch (Exception ignored) {
            // audit must not break payment create
        }
    }

    private static PaymentSession withRef(PaymentSession session, String paymentRef) {
        if (paymentRef == null || paymentRef.equals(session.sessionId())) {
            return session;
        }
        return new PaymentSession(
                paymentRef,
                session.jobId(),
                session.applicationNumber(),
                session.lob(),
                session.paymentUrl(),
                session.redirectUrl(),
                session.status(),
                session.externalTxnId(),
                session.createdAt(),
                session.expiresAt()
        );
    }

    private static ServiceException proposalNotPayable(String detail) {
        return new ServiceException(ServiceErrorResponse.builder()
                .title("Proposal Not Payable")
                .status(409)
                .detail(detail)
                .code(ErrorCodes.PROPOSAL_NOT_PAYABLE)
                .retryable(false)
                .build());
    }

    private static ServiceException badPaymentUrl(String detail) {
        return new ServiceException(ServiceErrorResponse.builder()
                .title("Upstream Bad Response")
                .status(502)
                .detail(detail)
                .code(ErrorCodes.UPSTREAM_BAD_RESPONSE)
                .retryable(false)
                .build());
    }
}
