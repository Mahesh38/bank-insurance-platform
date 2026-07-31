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
import com.bank.insurance.onesb.domain.command.GetApplicationStatusCommand;
import com.bank.insurance.onesb.domain.model.ApplicationStatus;
import com.bank.insurance.onesb.domain.model.BankStage;
import com.bank.insurance.onesb.domain.port.inbound.StatusUseCase;
import com.bank.insurance.onesb.domain.port.outbound.OneSbStatusPort;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Application status orchestration (FUNC-009): validate → 1SB prostat → normalise → audit.
 */
@Service
public class StatusService implements StatusUseCase {

    private final OneSbStatusPort statusPort;
    private final AuditEventPublisher auditEventPublisher;
    private final SecretProvider secretProvider;

    public StatusService(OneSbStatusPort statusPort,
                         AuditEventPublisher auditEventPublisher,
                         SecretProvider secretProvider) {
        this.statusPort = statusPort;
        this.auditEventPublisher = auditEventPublisher;
        this.secretProvider = secretProvider;
    }

    @Override
    public ApplicationStatus getStatus(GetApplicationStatusCommand command) {
        validate(command);

        ApplicationStatus fetched = statusPort.fetchStatus(
                command.applicationNumber().trim(),
                command.lob().trim(),
                command.insuranceCompanyCode().trim(),
                StringUtils.hasText(command.policyNo()) ? command.policyNo().trim() : null);

        if (fetched == null || !StringUtils.hasText(fetched.onesbStatus())) {
            throw notFound(command.applicationNumber());
        }

        BankStage bankStatus = StatusNormaliser.toBankStage(fetched.onesbStatus());
        ApplicationStatus result = new ApplicationStatus(
                fetched.applicationNumber(),
                bankStatus,
                fetched.onesbStatus(),
                fetched.manufacturerAppStatus(),
                fetched.manufacturerAppStatusDesc(),
                fetched.policyNumber(),
                fetched.insuranceCompanyCode(),
                fetched.lob(),
                fetched.lastUpdated() != null ? fetched.lastUpdated() : Instant.now());

        String actorId = StringUtils.hasText(command.actorId()) ? command.actorId() : "system";
        publishStatusChecked(command, result, actorId);
        return result;
    }

    private void validate(GetApplicationStatusCommand command) {
        List<ServiceError> errors = new ArrayList<>();
        if (!StringUtils.hasText(command.applicationNumber())) {
            errors.add(ServiceError.ofField(
                    ErrorCodes.MISSING_REQUIRED_FIELD,
                    "applicationNumber is required",
                    "applicationNumber"));
        }
        if (!StringUtils.hasText(command.lob())) {
            errors.add(ServiceError.ofField(
                    ErrorCodes.MISSING_REQUIRED_FIELD,
                    "lob is required",
                    "lob"));
        }
        if (!StringUtils.hasText(command.insuranceCompanyCode())) {
            errors.add(ServiceError.ofField(
                    ErrorCodes.MISSING_REQUIRED_FIELD,
                    "insuranceCompanyCode is required",
                    "insuranceCompanyCode"));
        }
        if (!errors.isEmpty()) {
            throw new ServiceException(ServiceErrorResponse.builder()
                    .title("Validation Failed")
                    .status(422)
                    .detail("Application status request validation failed")
                    .code(ErrorCodes.VALIDATION_ERROR)
                    .retryable(false)
                    .errors(errors)
                    .build());
        }
    }

    private void publishStatusChecked(GetApplicationStatusCommand command,
                                      ApplicationStatus status,
                                      String actorId) {
        try {
            String distributorId = secretProvider.getDistributorId();
            AuditEvent event = AuditEvent.builder()
                    .actorId(actorId)
                    .actorType("USER")
                    .action(AuditActions.APPLICATION_STATUS_CHECKED)
                    .resourceType("APPLICATION")
                    .resourceId(status.applicationNumber())
                    .outcome(AuditOutcomes.SUCCESS)
                    .lob(command.lob())
                    .journeyId(command.journeyId())
                    .distributorId(distributorId)
                    .metadata("distributorId", distributorId)
                    .metadata("bankStatus", status.bankStatus().name())
                    .metadata("applicationNumber", status.applicationNumber())
                    .build();
            auditEventPublisher.publish(event);
        } catch (Exception ignored) {
            // audit must not break status lookup
        }
    }

    private static ServiceException notFound(String applicationNumber) {
        return new ServiceException(ServiceErrorResponse.builder()
                .title("Not Found")
                .status(404)
                .detail("Application not found: " + applicationNumber)
                .code(ErrorCodes.RESOURCE_NOT_FOUND)
                .retryable(false)
                .build());
    }
}
