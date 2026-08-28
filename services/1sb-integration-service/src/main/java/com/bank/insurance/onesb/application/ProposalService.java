package com.bank.insurance.onesb.application;

import com.bank.common.audit.AuditActions;
import com.bank.common.audit.AuditEvent;
import com.bank.common.audit.AuditEventPublisher;
import com.bank.common.audit.AuditOutcomes;
import com.bank.common.error.ErrorCodes;
import com.bank.common.error.ServiceError;
import com.bank.common.error.ServiceErrorResponse;
import com.bank.common.error.PlatformLayer;
import com.bank.common.error.ServiceException;
import com.bank.common.secrets.SecretProvider;
import com.bank.insurance.onesb.domain.command.SubmitProposalCommand;
import com.bank.insurance.onesb.domain.model.JobStatus;
import com.bank.insurance.onesb.domain.model.Lob;
import com.bank.insurance.onesb.domain.model.OneSbProposalSubmitResult;
import com.bank.insurance.onesb.domain.model.ProposalSchema;
import com.bank.insurance.onesb.domain.model.ProposalSubmitResult;
import com.bank.insurance.onesb.domain.model.QuoteJob;
import com.bank.insurance.onesb.domain.port.inbound.ProposalUseCase;
import com.bank.insurance.onesb.domain.port.outbound.JobPollSchedulerPort;
import com.bank.insurance.onesb.domain.port.outbound.JobStorePort;
import com.bank.insurance.onesb.domain.port.outbound.OneSbProposalPort;
import com.bank.insurance.onesb.lob.LobProposalHandler;
import com.bank.insurance.onesb.lob.LobProposalHandlerRegistry;
import com.bank.insurance.onesb.lob.life.term.TermProposalHandler;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

/**
 * Proposal orchestration:
 * <ul>
 *   <li>FUNC-004 schema: optional quote expiry → LOB path → 1SB GET</li>
 *   <li>FUNC-005 submit: agentId gate → consent WARN → job → LOB payload → 1SB POST</li>
 * </ul>
 */
@Service
public class ProposalService implements ProposalUseCase {

    private final JobStorePort jobStore;
    private final LobProposalHandlerRegistry handlerRegistry;
    private final OneSbProposalPort proposalPort;
    private final JobPollSchedulerPort pollScheduler;
    private final AuditEventPublisher auditEventPublisher;
    private final SecretProvider secretProvider;

    public ProposalService(JobStorePort jobStore,
                           LobProposalHandlerRegistry handlerRegistry,
                           OneSbProposalPort proposalPort,
                           JobPollSchedulerPort pollScheduler,
                           AuditEventPublisher auditEventPublisher,
                           SecretProvider secretProvider) {
        this.jobStore = jobStore;
        this.handlerRegistry = handlerRegistry;
        this.proposalPort = proposalPort;
        this.pollScheduler = pollScheduler;
        this.auditEventPublisher = auditEventPublisher;
        this.secretProvider = secretProvider;
    }

    @Override
    public ProposalSchema getSchema(Lob lob, String productCode, String manufacturerId,
                                    String version, String quoteJobId) {
        if (lob == null) {
            throw missingLob("getSchema");
        }
        if (StringUtils.hasText(quoteJobId)) {
            assertQuoteUsable(quoteJobId);
        }

        LobProposalHandler handler = handlerRegistry.get(lob);
        String path = handler.schemaPath(productCode, manufacturerId, version);
        return proposalPort.getSchema(lob, productCode, manufacturerId, version, path);
    }

    @Override
    public ProposalSubmitResult submit(SubmitProposalCommand command) {
        if (command.lob() == null) {
            throw missingLob("submit");
        }

        String agentId = TermProposalHandler.resolveAgentId(command);
        if (!StringUtils.hasText(agentId)) {
            throw ServiceException.of(ErrorCodes.AGENT_ATTRIBUTION_MISSING)
                    .service("onesb")
                    .layer(PlatformLayer.L5)
                    .component("ProposalService")
                    .operation("submit")
                    .reason("agentId is required on proposal submit")
                    .errors(List.of(ServiceError.ofField(
                            ErrorCodes.AGENT_ATTRIBUTION_MISSING,
                            "agentId is required",
                            "agentId")))
                    .build();
        }

        String actorId = StringUtils.hasText(command.actorId()) ? command.actorId() : "system";
        String distributorId = secretProvider.getDistributorId();

        if (!StringUtils.hasText(command.consentRef())) {
            publishConsentRefMissing(command, actorId, agentId, distributorId);
        }

        LobProposalHandler handler = handlerRegistry.get(command.lob());
        String jobId = jobStore.createJob(
                command.lob().name(),
                "PROPOSAL",
                command.journeyId(),
                command.idempotencyKey(),
                actorId
        );

        Object payload = handler.buildSubmitPayload(command);
        OneSbProposalSubmitResult upstream;
        try {
            upstream = proposalPort.submit(jobId, handler.submitPath(), payload);
        } catch (ServiceException ex) {
            if (ErrorCodes.PROPOSAL_REJECTED.equals(ex.getErrorResponse().getCode())) {
                publishProposalRejected(command, jobId, actorId, agentId, distributorId, ex);
            }
            throw ex;
        }

        JobStatus status;
        if (upstream.complete()) {
            jobStore.completeJob(jobId, List.of(), upstream.applicationNumber());
            status = JobStatus.COMPLETED;
        } else {
            String reqId = upstream.externalReqId();
            jobStore.updateJobPolling(jobId, reqId);
            pollScheduler.schedulePoll(jobId, handler.pollPath(reqId));
            status = JobStatus.PENDING;
        }

        publishProposalSubmitted(command, jobId, actorId, agentId, distributorId,
                upstream.applicationNumber(), status);
        return new ProposalSubmitResult(jobId, status);
    }

    /**
     * Load proposal job for GET. Unknown id → 404 {@link ErrorCodes#RESOURCE_NOT_FOUND}.
     * Does not fabricate {@code applicationNumber} — returns whatever is stored on the job.
     */
    @Override
    public QuoteJob getProposalResult(String jobId) {
        return jobStore.findQuoteJob(jobId)
                .orElseThrow(() -> ServiceException.of(ErrorCodes.RESOURCE_NOT_FOUND)
                        .service("onesb")
                        .layer(PlatformLayer.L5)
                        .component("ProposalService")
                        .operation("getProposalResult")
                        .reason("proposal job not found: " + jobId)
                        .build());
    }

    private void assertQuoteUsable(String quoteJobId) {
        Optional<QuoteJob> found = jobStore.findQuoteJob(quoteJobId);
        if (found.isEmpty()) {
            throw quoteExpired("Quote job not found or expired: " + quoteJobId);
        }
        QuoteJob job = found.get();
        JobStatus status = job.status();
        if (status == JobStatus.TIMEOUT || status == JobStatus.FAILED) {
            throw quoteExpired("Quote job is " + status + ": " + quoteJobId);
        }
        if ((status == JobStatus.COMPLETED || status == JobStatus.PARTIAL)
                && (job.offers() == null || job.offers().isEmpty())) {
            throw quoteExpired("Quote job has no offers: " + quoteJobId);
        }
    }

    private static ServiceException missingLob(String operation) {
        return ServiceException.of(ErrorCodes.VALIDATION_ERROR)
                .service("onesb")
                .layer(PlatformLayer.L5)
                .component("ProposalService")
                .operation(operation)
                .reason("lob is required")
                .errors(List.of(ServiceError.ofField(
                        ErrorCodes.MISSING_REQUIRED_FIELD, "lob is required", "lob")))
                .build();
    }

    /**
     * FUNC-004 AC-2 answers 410 here, while catalogue 04 section 6 registers QUOTE_EXPIRED as 409.
     * Both are ratified and they describe different conditions — a quote job that is gone, versus
     * an offer selected past its validity window. The override preserves the approved behaviour
     * and records the disagreement; see 07-PLATFORM-ERROR-CONTRACT.md section 13.
     */
    private static ServiceException quoteExpired(String reason) {
        return ServiceException.of(ErrorCodes.QUOTE_EXPIRED)
                .service("onesb")
                .layer(PlatformLayer.L5)
                .component("ProposalService")
                .operation("assertQuoteUsable")
                .statusOverride(410, "FUNC-004 AC-2 (phase-3, TL + QA approved)")
                .reason(reason)
                .build();
    }

    private void publishConsentRefMissing(SubmitProposalCommand command,
                                          String actorId,
                                          String agentId,
                                          String distributorId) {
        try {
            AuditEvent event = AuditEvent.builder()
                    .actorId(actorId)
                    .actorType("USER")
                    .action(AuditActions.CONSENT_REF_MISSING)
                    .resourceType("PROPOSAL")
                    .resourceId(command.journeyId() != null ? command.journeyId() : "unknown")
                    .outcome(AuditOutcomes.WARN)
                    .lob(command.lob() != null ? command.lob().name() : null)
                    .journeyId(command.journeyId())
                    .distributorId(distributorId)
                    .agentId(agentId)
                    .metadata("reason", "consentRef absent on proposal submit")
                    .build();
            auditEventPublisher.publish(event);
        } catch (Exception ignored) {
            // audit must not break submit
        }
    }

    private void publishProposalSubmitted(SubmitProposalCommand command,
                                          String jobId,
                                          String actorId,
                                          String agentId,
                                          String distributorId,
                                          String applicationNumber,
                                          JobStatus status) {
        try {
            AuditEvent.AuditEventBuilder builder = AuditEvent.builder()
                    .actorId(actorId)
                    .actorType("USER")
                    .action(AuditActions.PROPOSAL_SUBMITTED)
                    .resourceType("PROPOSAL_JOB")
                    .resourceId(jobId)
                    .outcome(status == JobStatus.COMPLETED ? AuditOutcomes.SUCCESS : AuditOutcomes.PENDING)
                    .lob(command.lob() != null ? command.lob().name() : null)
                    .journeyId(command.journeyId())
                    .distributorId(distributorId)
                    .agentId(agentId)
                    .metadata("jobId", jobId)
                    .metadata("status", status.name());
            if (StringUtils.hasText(applicationNumber)) {
                builder.metadata("applicationNumber", applicationNumber);
            }
            if (StringUtils.hasText(command.consentRef())) {
                builder.metadata("consentRef", command.consentRef());
            }
            auditEventPublisher.publish(builder.build());
        } catch (Exception ignored) {
            // audit must not break submit
        }
    }

    private void publishProposalRejected(SubmitProposalCommand command,
                                         String jobId,
                                         String actorId,
                                         String agentId,
                                         String distributorId,
                                         ServiceException ex) {
        try {
            AuditEvent event = AuditEvent.builder()
                    .actorId(actorId)
                    .actorType("USER")
                    .action(AuditActions.PROPOSAL_SUBMITTED)
                    .resourceType("PROPOSAL_JOB")
                    .resourceId(jobId)
                    .outcome(AuditOutcomes.REJECTED)
                    .lob(command.lob() != null ? command.lob().name() : null)
                    .journeyId(command.journeyId())
                    .distributorId(distributorId)
                    .agentId(agentId)
                    .metadata("jobId", jobId)
                    .metadata("errorCode", ErrorCodes.PROPOSAL_REJECTED)
                    .metadata("detail", ex.getErrorResponse().getDetail())
                    .build();
            auditEventPublisher.publish(event);
        } catch (Exception ignored) {
            // audit must not break error path
        }
    }
}
