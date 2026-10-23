package com.bank.identity.authz.api;

import com.bank.identity.authz.application.AuthorizationDecisionService;
import com.bank.identity.authz.application.IdentityResolutionService;
import com.bank.identity.authz.application.PartnerUserAdministrationService;
import com.bank.identity.authz.application.PlatformLoginService;
import com.bank.identity.authz.application.PlatformLoginService.AccountView;
import com.bank.identity.authz.application.PlatformLoginService.PasswordOutcome;
import com.bank.identity.authz.application.PlatformLoginService.PasswordSetResult;
import com.bank.identity.authz.domain.AuthorizationModels.Decision;
import com.bank.identity.authz.domain.AuthorizationModels.DecisionRequest;
import com.bank.identity.authz.domain.AuthorizationModels.Resource;
import com.bank.identity.authz.domain.IdentityTypes.UserType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/v1")
public class IdentityAuthorizationController {

  private final AuthorizationDecisionService decisions;
  private final IdentityResolutionService identities;
  private final PartnerUserAdministrationService partnerUsers;
  private final ActorPrincipalResolver actors;
  private final PlatformLoginService platformLogin;

  public IdentityAuthorizationController(
      AuthorizationDecisionService decisions,
      IdentityResolutionService identities,
      PartnerUserAdministrationService partnerUsers,
      ActorPrincipalResolver actors,
      PlatformLoginService platformLogin) {
    this.decisions = decisions;
    this.identities = identities;
    this.partnerUsers = partnerUsers;
    this.actors = actors;
    this.platformLogin = platformLogin;
  }

  @PostMapping("/authorization/decisions")
  public Decision decide(@Valid @RequestBody AuthorizationDecisionRequest request) {
    return decisions.decide(
        new DecisionRequest(request.subjectId(), request.action(), request.resource()));
  }

  @PostMapping("/identities/resolve")
  public ResolvedIdentityResponse resolve(@Valid @RequestBody ResolveIdentityRequest request) {
    var user =
        identities.resolve(
            new IdentityResolutionService.ResolveIdentityCommand(
                request.provider(),
                request.providerSubject(),
                request.userType(),
                request.username(),
                request.email(),
                request.employeeId()));
    return new ResolvedIdentityResponse(
        user.getId(),
        user.getStatus().name(),
        user.getPolicyVersion(),
        user.getUserType(),
        user.getInsurerCode());
  }

  /**
   * E5: a bank admin creates the partner user; provisioning is queued in the same transaction, so
   * the answer is {@code 202} with status {@code PENDING} until the provider account exists.
   */
  @PostMapping("/partner-users")
  @ResponseStatus(HttpStatus.ACCEPTED)
  public PartnerUserAdministrationService.CreatedPartnerUser createPartnerUser(
      @Valid @RequestBody CreatePartnerUserRequest request,
      @RequestAttribute(InternalServiceAuthFilter.CALLER_ATTRIBUTE) String caller,
      @RequestHeader(value = "X-Actor-Id", required = false) String actorHeader) {
    UUID creatorId = actors.requireActor(caller, actorHeader, "createPartnerUser");
    return partnerUsers.create(
        new PartnerUserAdministrationService.CreatePartnerUserCommand(
            request.username(),
            request.email(),
            request.firstName(),
            request.lastName(),
            request.insurerCode()),
        creatorId);
  }

  /** Only when {@code identity.partner-onboarding.approval-required=true}; otherwise 409. */
  @PostMapping("/approval-requests/{approvalId}/approve")
  public ApprovalResponse approve(
      @PathVariable UUID approvalId,
      @RequestAttribute(InternalServiceAuthFilter.CALLER_ATTRIBUTE) String caller,
      @RequestHeader(value = "X-Actor-Id", required = false) String actorHeader) {
    UUID checkerId = actors.requireActor(caller, actorHeader, "approvePartnerUser");
    return new ApprovalResponse(approvalId, partnerUsers.approve(approvalId, checkerId));
  }

  public record AuthorizationDecisionRequest(
      @NotNull UUID subjectId, @NotBlank String action, @NotNull Resource resource) {}

  public record ResolveIdentityRequest(
      @NotBlank String provider,
      @NotBlank String providerSubject,
      @NotNull UserType userType,
      @NotBlank String username,
      String email,
      String employeeId) {}

  public record ResolvedIdentityResponse(
      UUID businessUserId,
      String status,
      long policyVersion,
      UserType userType,
      String insurerCode) {}

  public record CreatePartnerUserRequest(
      @NotBlank String username,
      @Email @NotBlank String email,
      String firstName,
      String lastName,
      @NotBlank String insurerCode) {}

  public record ApprovalResponse(UUID approvalId, String targetStatus) {}

  @PostMapping("/login/preview")
  public AccountView previewLogin(@Valid @RequestBody LoginIdRequest request) {
    return platformLogin.preview(request.userType(), request.loginId());
  }

  @PostMapping("/login/bank-password-result")
  public PasswordOutcome bankPasswordResult(@Valid @RequestBody BankPasswordResultRequest request) {
    return platformLogin.recordBankPasswordResult(request.businessUserId(), request.accepted());
  }

  @PostMapping("/login/verify-platform-password")
  public PasswordOutcome verifyPlatformPassword(
      @Valid @RequestBody PlatformPasswordRequest request) {
    return platformLogin.verifyPlatformPassword(request.businessUserId(), request.password());
  }

  @PostMapping("/login/complete")
  public AccountView completeLogin(@Valid @RequestBody UserIdRequest request) {
    return platformLogin.completeLogin(request.businessUserId());
  }

  @PostMapping("/login/unlock/assess")
  public AccountView assessUnlock(@Valid @RequestBody LoginIdRequest request) {
    return platformLogin.assessUnlock(request.userType(), request.loginId());
  }

  @PostMapping("/login/unlock/complete")
  public AccountView completeUnlock(@Valid @RequestBody UserIdRequest request) {
    return platformLogin.completeUnlock(request.businessUserId());
  }

  @PostMapping("/login/forgot/assess")
  public AccountView assessForgot(@Valid @RequestBody ForgotRequest request) {
    return platformLogin.assessForgotPassword(request.email());
  }

  @PostMapping("/login/password")
  public PasswordSetResult setPassword(@Valid @RequestBody SetPasswordRequest request) {
    return platformLogin.setPassword(
        request.businessUserId(),
        request.newPassword(),
        request.confirmPassword(),
        request.purpose());
  }

  public record LoginIdRequest(@NotNull UserType userType, @NotBlank String loginId) {}

  public record UserIdRequest(@NotNull UUID businessUserId) {}

  public record BankPasswordResultRequest(@NotNull UUID businessUserId, boolean accepted) {}

  public record PlatformPasswordRequest(@NotNull UUID businessUserId, @NotBlank String password) {
    @Override
    public String toString() {
      return "PlatformPasswordRequest[businessUserId=" + businessUserId + "]";
    }
  }

  public record ForgotRequest(@NotBlank @Email String email) {}

  public record SetPasswordRequest(
      @NotNull UUID businessUserId,
      @NotBlank String newPassword,
      @NotBlank String confirmPassword,
      @NotBlank String purpose) {
    @Override
    public String toString() {
      return "SetPasswordRequest[businessUserId=" + businessUserId + ", purpose=" + purpose + "]";
    }
  }
}
