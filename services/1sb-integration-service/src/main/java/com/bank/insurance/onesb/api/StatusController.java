package com.bank.insurance.onesb.api;

import com.bank.insurance.onesb.api.dto.ApplicationStatusResponse;
import com.bank.insurance.onesb.domain.command.GetApplicationStatusCommand;
import com.bank.insurance.onesb.domain.model.ApplicationStatus;
import com.bank.insurance.onesb.domain.port.inbound.StatusUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Bank application status API — {@code GET /v1/status/{applicationNumber}} (FUNC-009).
 */
@RestController
@RequestMapping("/v1/status")
public class StatusController {

    public static final String ACTOR_HEADER = "X-Actor-Id";

    private final StatusUseCase statusUseCase;

    public StatusController(StatusUseCase statusUseCase) {
        this.statusUseCase = statusUseCase;
    }

    @GetMapping("/{applicationNumber}")
    public ResponseEntity<ApplicationStatusResponse> getStatus(
            @PathVariable String applicationNumber,
            @RequestParam String lob,
            @RequestParam String insuranceCompanyCode,
            @RequestParam(required = false) String policyNo,
            @RequestParam(required = false) String journeyId,
            @RequestHeader(value = ACTOR_HEADER, required = false) String actorId) {

        GetApplicationStatusCommand command = new GetApplicationStatusCommand(
                applicationNumber,
                lob,
                insuranceCompanyCode,
                policyNo,
                StringUtils.hasText(actorId) ? actorId : "system",
                journeyId);

        ApplicationStatus status = statusUseCase.getStatus(command);
        return ResponseEntity.ok(toResponse(status));
    }

    private static ApplicationStatusResponse toResponse(ApplicationStatus status) {
        return new ApplicationStatusResponse(
                status.applicationNumber(),
                status.bankStatus() != null ? status.bankStatus().name() : null,
                status.manufacturerAppStatus(),
                status.manufacturerAppStatusDesc(),
                status.policyNumber(),
                status.lastUpdated());
    }
}
