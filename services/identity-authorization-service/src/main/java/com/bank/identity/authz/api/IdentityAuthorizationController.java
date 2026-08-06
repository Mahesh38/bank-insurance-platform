package com.bank.identity.authz.api;

import com.bank.identity.authz.application.AuthorizationDecisionService;
import com.bank.identity.authz.application.IdentityResolutionService;
import com.bank.identity.authz.application.PartnerUserAdministrationService;
import com.bank.identity.authz.domain.AuthorizationModels.Decision;
import com.bank.identity.authz.domain.AuthorizationModels.DecisionRequest;
import com.bank.identity.authz.domain.AuthorizationModels.Resource;
import com.bank.identity.authz.domain.IdentityTypes.UserType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/internal/v1")
public class IdentityAuthorizationController {

    private final AuthorizationDecisionService decisions;
    private final IdentityResolutionService identities;
    private final PartnerUserAdministrationService partnerUsers;

    public IdentityAuthorizationController(
        AuthorizationDecisionService decisions,
        IdentityResolutionService identities,
        PartnerUserAdministrationService partnerUsers
    ) {
        this.decisions = decisions;
        this.identities = identities;
        this.partnerUsers = partnerUsers;
    }

    @PostMapping("/authorization/decisions")
    public Decision decide(@Valid @RequestBody AuthorizationDecisionRequest request) {
        return decisions.decide(new DecisionRequest(request.subjectId(), request.action(), request.resource()));
    }

    @PostMapping("/identities/resolve")
    public ResolvedIdentityResponse resolve(@Valid @RequestBody ResolveIdentityRequest request) {
        var user = identities.resolve(new IdentityResolutionService.ResolveIdentityCommand(
            request.provider(), request.providerSubject(), request.userType(), request.username(), request.email(),
            request.employeeId()));
        return new ResolvedIdentityResponse(user.getId(), user.getStatus().name(), user.getPolicyVersion(),
            user.getUserType(), user.getInsurerCode());
    }

    @PostMapping("/partner-users")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public PartnerUserAdministrationService.SubmittedPartnerUser createPartnerUser(
        @Valid @RequestBody CreatePartnerUserRequest request,
        @RequestHeader("X-Actor-Id") String makerId
    ) {
        return partnerUsers.submit(new PartnerUserAdministrationService.CreatePartnerUserCommand(
            request.username(), request.email(), request.firstName(), request.lastName(), request.insurerCode()), makerId);
    }

    @PostMapping("/approval-requests/{approvalId}/approve")
    public ApprovalResponse approve(
        @PathVariable UUID approvalId,
        @RequestHeader("X-Actor-Id") String checkerId
    ) {
        return new ApprovalResponse(approvalId, partnerUsers.approve(approvalId, checkerId));
    }

    public record AuthorizationDecisionRequest(
        @NotNull UUID subjectId,
        @NotBlank String action,
        @NotNull Resource resource
    ) {}

    public record ResolveIdentityRequest(
        @NotBlank String provider,
        @NotBlank String providerSubject,
        @NotNull UserType userType,
        @NotBlank String username,
        String email,
        String employeeId
    ) {}

    public record ResolvedIdentityResponse(
        UUID businessUserId,
        String status,
        long policyVersion,
        UserType userType,
        String insurerCode
    ) {}

    public record CreatePartnerUserRequest(
        @NotBlank String username,
        @Email @NotBlank String email,
        String firstName,
        String lastName,
        @NotBlank String insurerCode
    ) {}

    public record ApprovalResponse(UUID approvalId, String targetStatus) {}
}
