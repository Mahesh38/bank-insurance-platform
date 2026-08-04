package com.bank.insurance.onesb.application;

import com.bank.common.audit.AuditActions;
import com.bank.common.audit.AuditEvent;
import com.bank.common.audit.AuditEventPublisher;
import com.bank.common.audit.AuditOutcomes;
import com.bank.common.error.ErrorCodes;
import com.bank.common.error.ServiceErrorResponse;
import com.bank.common.error.ServiceException;
import com.bank.insurance.onesb.domain.model.ApplicationStatus;
import com.bank.insurance.onesb.domain.model.BankApplicationStatus;
import com.bank.insurance.onesb.domain.model.Lob;
import com.bank.insurance.onesb.domain.model.OneSbApplicationStatusResult;
import com.bank.insurance.onesb.domain.port.inbound.StatusUseCase;
import com.bank.insurance.onesb.domain.port.outbound.OneSbStatusPort;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Application-status orchestration (FUNC-009): 1SB call → normalise → audit. Live call every
 * time — not cached (architecture §7.2), no status-snapshot persistence (see
 * phase-4/FUNC-009-ASSIGNMENT.md for the scope decision).
 */
@Service
public class StatusService implements StatusUseCase {

    private static final Set<String> UW_REQUIREMENTS_STATUSES = Set.of(
            "REQUIREMENTS_PENDING", "DOCUMENT_UPLOAD_INITIATED", "DOCUMENT_UPLOAD_PENDING",
            "DOCUMENTS_UPLOADED", "REQUIREMENT_VERIFICATION", "OTP_VERIFIED",
            "KYC_SUCCESS", "KYC_FAILED", "INSPECTION_PENDING", "INSPECTION_APPROVED");

    private static final Set<String> UW_IN_PROGRESS_STATUSES = Set.of(
            "UNDERWRITING", "SCRUTINY", "PRE_CONVERSION", "ACCEPTED");

    private static final Set<String> CUSTOMER_DECISION_STATUSES = Set.of(
            "COUNTER_OFFER", "AWAITING_CLIENT_APPROVAL");

    private static final Set<String> CLOSED_STATUSES = Set.of(
            "REJECTED", "CANCELLED", "PROPOSAL_ERROR");

    private final OneSbStatusPort statusPort;
    private final AuditEventPublisher auditEventPublisher;
    private final Clock clock;

    public StatusService(OneSbStatusPort statusPort, AuditEventPublisher auditEventPublisher, Clock clock) {
        this.statusPort = statusPort;
        this.auditEventPublisher = auditEventPublisher;
        this.clock = clock;
    }

    @Override
    public ApplicationStatus getStatus(String applicationNumber, Lob lob, String insurerCode,
                                       String productCode, String actorId) {
        if (lob == null) {
            throw validationError("lob is required");
        }
        if (!StringUtils.hasText(applicationNumber)) {
            throw validationError("applicationNumber is required");
        }
        if (!StringUtils.hasText(insurerCode)) {
            throw validationError("insurerCode is required");
        }

        Optional<OneSbApplicationStatusResult> upstream =
                statusPort.getStatus(applicationNumber, insurerCode, productCode);
        OneSbApplicationStatusResult result = upstream.orElseThrow(() -> new ServiceException(
                ServiceErrorResponse.builder()
                        .title("Not Found")
                        .status(404)
                        .detail("No status found for applicationNumber: " + applicationNumber)
                        .code(ErrorCodes.RESOURCE_NOT_FOUND)
                        .retryable(false)
                        .build()));

        BankApplicationStatus bankStatus = normalise(result.rawStatus());
        ApplicationStatus status = new ApplicationStatus(
                applicationNumber,
                bankStatus,
                result.rawStatus(),
                result.subStatus(),
                result.policyNumber(),
                clock.instant()
        );

        publishStatusChecked(status, lob, StringUtils.hasText(actorId) ? actorId : "system");
        return status;
    }

    /** Pure normalisation — see {@code field-guides/application-status.md} mapping table. */
    static BankApplicationStatus normalise(String rawStatus) {
        if (!StringUtils.hasText(rawStatus)) {
            return BankApplicationStatus.UNKNOWN;
        }
        String status = rawStatus.toUpperCase(Locale.ROOT);
        if (status.startsWith("QUOTE")) {
            return BankApplicationStatus.QUOTING;
        }
        if (status.startsWith("PROPOSAL")) {
            return BankApplicationStatus.PROPOSAL;
        }
        if (UW_REQUIREMENTS_STATUSES.contains(status)) {
            return BankApplicationStatus.UW_REQUIREMENTS;
        }
        if (UW_IN_PROGRESS_STATUSES.contains(status)) {
            return BankApplicationStatus.UW_IN_PROGRESS;
        }
        if (CUSTOMER_DECISION_STATUSES.contains(status)) {
            return BankApplicationStatus.CUSTOMER_DECISION;
        }
        if (status.startsWith("PAYMENT")) {
            return BankApplicationStatus.PAYMENT;
        }
        if (status.equals("POLICY_ISSUED")) {
            return BankApplicationStatus.ISSUED;
        }
        if (CLOSED_STATUSES.contains(status)) {
            return BankApplicationStatus.CLOSED;
        }
        return BankApplicationStatus.UNKNOWN;
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

    private void publishStatusChecked(ApplicationStatus status, Lob lob, String actorId) {
        try {
            AuditEvent event = AuditEvent.builder()
                    .actorId(actorId)
                    .actorType("USER")
                    .action(AuditActions.APPLICATION_STATUS_CHECKED)
                    .resourceType("APPLICATION")
                    .resourceId(status.applicationNumber())
                    .outcome(AuditOutcomes.SUCCESS)
                    .lob(lob.name())
                    .metadata("applicationNumber", status.applicationNumber())
                    .metadata("bankStatus", status.bankStatus().name())
                    .metadata("rawStatus", status.rawStatus())
                    .build();
            auditEventPublisher.publish(event);
        } catch (Exception ignored) {
            // audit must not break status lookup
        }
    }
}
