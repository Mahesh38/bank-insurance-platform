package com.bank.insurance.onesb.application;

import com.bank.common.audit.AuditActions;
import com.bank.common.audit.AuditEvent;
import com.bank.common.audit.AuditEventPublisher;
import com.bank.common.audit.AuditOutcomes;
import com.bank.common.error.ErrorCodes;
import com.bank.common.error.ServiceError;
import com.bank.common.error.ServiceErrorResponse;
import com.bank.common.error.PlatformLayer;
import com.bank.common.error.ServiceErrors;
import com.bank.common.error.ServiceException;
import com.bank.insurance.onesb.domain.command.CreateQuoteCommand;
import com.bank.insurance.onesb.domain.model.Lob;
import com.bank.insurance.onesb.domain.model.QuoteJob;
import com.bank.insurance.onesb.domain.port.inbound.QuoteUseCase;
import com.bank.insurance.onesb.domain.port.outbound.JobPollSchedulerPort;
import com.bank.insurance.onesb.domain.port.outbound.JobStorePort;
import com.bank.insurance.onesb.domain.port.outbound.OneSbQuotePort;
import com.bank.insurance.onesb.lob.LobQuoteHandler;
import com.bank.insurance.onesb.lob.LobQuoteHandlerRegistry;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Quote orchestration (Case 2): validate → create job → LOB handler builds payload → 1SB submit → schedule poll.
 */
@Service
public class QuoteService implements QuoteUseCase {

    private final JobStorePort jobStore;
    private final LobQuoteHandlerRegistry handlerRegistry;
    private final OneSbQuotePort quotePort;
    private final JobPollSchedulerPort pollScheduler;
    private final AuditEventPublisher auditEventPublisher;
    private final ServiceErrors serviceErrors;


    public QuoteService(JobStorePort jobStore,
                        LobQuoteHandlerRegistry handlerRegistry,
                        OneSbQuotePort quotePort,
                        JobPollSchedulerPort pollScheduler,
                        AuditEventPublisher auditEventPublisher,
                          ServiceErrors serviceErrors) {
        this.jobStore = jobStore;
        this.handlerRegistry = handlerRegistry;
        this.quotePort = quotePort;
        this.pollScheduler = pollScheduler;
        this.auditEventPublisher = auditEventPublisher;
        this.serviceErrors = serviceErrors;
    }

    @Override
    public String createQuote(CreateQuoteCommand command) {
        validate(command);
        LobQuoteHandler handler = handlerRegistry.get(command.lob());

        String actorId = command.actorId() != null && !command.actorId().isBlank()
                ? command.actorId() : "system";
        String jobId = jobStore.createJob(
                command.lob().name(),
                "QUOTE",
                command.journeyId(),
                command.idempotencyKey(),
                actorId
        );

        Object payload = handler.buildSubmitPayload(command);
        String externalReqId = quotePort.submitQuote(jobId, handler.submitPath(), payload);
        jobStore.updateJobPolling(jobId, externalReqId);
        pollScheduler.scheduleQuotePoll(jobId, command.lob().name(), externalReqId);

        publishQuoteCreated(command, jobId, actorId);
        return jobId;
    }

    /**
     * Load quote job for GET. Always returns the stored {@link QuoteJob} (including
     * {@link JobStatus#TIMEOUT}) so the bank can poll status; unknown id → 404
     * {@link ErrorCodes#RESOURCE_NOT_FOUND}. Does not throw {@link ErrorCodes#QUOTE_TIMEOUT}.
     */
    @Override
    public QuoteJob getQuoteResult(String jobId) {
        return jobStore.findQuoteJob(jobId)
                .orElseThrow(() -> serviceErrors.error(ErrorCodes.RESOURCE_NOT_FOUND)
                        .component("QuoteService")
                        .operation("getQuoteResult")
                        .reason("quote job not found: " + jobId)
                        .build());
    }

    private void validate(CreateQuoteCommand command) {
        List<ServiceError> errors = new ArrayList<>();
        if (command.lob() == null) {
            errors.add(ServiceError.ofField(ErrorCodes.MISSING_REQUIRED_FIELD, "lob is required", "lob"));
        } else if (command.lob() != Lob.TERM) {
            // FUNC-002: only TERM; other LOBs → UNSUPPORTED_LOB via registry after field checks
            // Still collect as unsupported here for clear 422 before port
            errors.add(ServiceError.ofField(
                    ErrorCodes.UNSUPPORTED_LOB, "Only TERM is supported for quote create", "lob"));
        }
        if (command.sumAssured() == null) {
            errors.add(ServiceError.ofField(
                    ErrorCodes.MISSING_REQUIRED_FIELD, "sumAssured is required", "sumAssured"));
        }
        if (command.members() == null || command.members().isEmpty()) {
            errors.add(ServiceError.ofField(
                    ErrorCodes.MISSING_REQUIRED_FIELD, "members must be non-empty", "members"));
        } else {
            for (int i = 0; i < command.members().size(); i++) {
                CreateQuoteCommand.MemberDetail m = command.members().get(i);
                if (m.dob() == null || m.dob().isBlank()) {
                    errors.add(ServiceError.ofField(
                            ErrorCodes.MISSING_REQUIRED_FIELD,
                            "members[" + i + "].dob is required",
                            "members[" + i + "].dob"));
                }
                if (m.gender() == null || m.gender().isBlank()) {
                    errors.add(ServiceError.ofField(
                            ErrorCodes.MISSING_REQUIRED_FIELD,
                            "members[" + i + "].gender is required",
                            "members[" + i + "].gender"));
                }
            }
        }
        if (!errors.isEmpty()) {
            String code = errors.stream()
                    .anyMatch(e -> ErrorCodes.UNSUPPORTED_LOB.equals(e.code()))
                    ? ErrorCodes.UNSUPPORTED_LOB
                    : ErrorCodes.VALIDATION_ERROR;
            throw serviceErrors.error(code)
                    .component("QuoteService")
                    .operation("createQuote")
                    .reason("quote request validation failed: " + errors.size() + " field error(s)")
                    .errors(errors)
                    .build();
        }
    }

    private void publishQuoteCreated(CreateQuoteCommand command, String jobId, String actorId) {
        try {
            String agentId = command.distribution() != null ? command.distribution().agentId() : null;
            AuditEvent event = AuditEvent.builder()
                    .actorId(actorId)
                    .actorType("USER")
                    .action(AuditActions.QUOTE_CREATED)
                    .resourceType("QUOTE_JOB")
                    .resourceId(jobId)
                    .outcome(AuditOutcomes.PENDING)
                    .lob(command.lob() != null ? command.lob().name() : null)
                    .journeyId(command.journeyId())
                    .agentId(agentId)
                    .metadata("jobId", jobId)
                    .build();
            auditEventPublisher.publish(event);
        } catch (Exception e) {
            // audit must not break the create path
        }
    }
}
