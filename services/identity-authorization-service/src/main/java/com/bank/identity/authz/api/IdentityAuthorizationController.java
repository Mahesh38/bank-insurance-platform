package com.bank.identity.authz.api;

import com.bank.common.security.internal.InternalCallerAuth;
import com.bank.identity.authz.application.AuthorizationDecisionService;
import com.bank.identity.authz.application.IdentityProfileService;
import com.bank.identity.authz.application.IdentityProfileService.SubjectProfile;
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
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Set;
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
  private final IdentityProfileService profiles;
  private final PartnerUserAdministrationService partnerUsers;
  private final ActorPrincipalResolver actors;
  private final PlatformLoginService platformLogin;
  private final PrivilegeGuard privileges;

  public IdentityAuthorizationController(
      AuthorizationDecisionService decisions,
      IdentityResolutionService identities,
      IdentityProfileService profiles,
      PartnerUserAdministrationService partnerUsers,
      ActorPrincipalResolver actors,
      PlatformLoginService platformLogin,
      PrivilegeGuard privileges) {
    this.decisions = decisions;
    this.identities = identities;
    this.profiles = profiles;
    this.partnerUsers = partnerUsers;
    this.actors = actors;
    this.platformLogin = platformLogin;
    this.privileges = privileges;
  }

  @PostMapping("/authorization/decisions")
  public Decision decide(
      HttpServletRequest http, @Valid @RequestBody AuthorizationDecisionRequest request) {
    privileges.require(http, InternalCallerAuth.PRIVILEGE_AUTHORIZATION_DECIDE, "decide");
    return decisions.decide(
        new DecisionRequest(request.subjectId(), request.action(), request.resource()));
  }

  @PostMapping("/identities/resolve")
  public ResolvedIdentityResponse resolve(
      HttpServletRequest http, @Valid @RequestBody ResolveIdentityRequest request) {
    privileges.require(http, InternalCallerAuth.PRIVILEGE_IDENTITIES_RESOLVE, "resolve");
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

  @PostMapping("/identities/profile")
  public SubjectProfileResponse profile(
      HttpServletRequest http, @Valid @RequestBody SubjectProfileRequest request) {
    privileges.require(http, InternalCallerAuth.PRIVILEGE_IDENTITIES_PROFILE, "profile");
    SubjectProfile loaded = profiles.load(request.subjectId());
    return new SubjectProfileResponse(
        loaded.subjectId(), loaded.userType(), loaded.roleCodes(), loaded.status().name());
  }

  /**
   * E5: a bank admin creates the partner user; provisioning is queued in the same transaction, so
   * the answer is {@code 202} with status {@code PENDING} until the provider account exists.
   */
  @PostMapping("/partner-users")
  @ResponseStatus(HttpStatus.ACCEPTED)
  public PartnerUserAdministrationService.CreatedPartnerUser createPartnerUser(
      HttpServletRequest http,
      @Valid @RequestBody CreatePartnerUserRequest request,
      @RequestAttribute(InternalCallerAuth.CALLER_ATTRIBUTE) String caller,
      @RequestHeader(value = "X-Actor-Id", required = false) String actorHeader) {
    privileges.require(http, InternalCallerAuth.PRIVILEGE_PARTNER_ADMIN, "createPartnerUser");
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
      HttpServletRequest http,
      @PathVariable UUID approvalId,
      @RequestAttribute(InternalCallerAuth.CALLER_ATTRIBUTE) String caller,
      @RequestHeader(value = "X-Actor-Id", required = false) String actorHeader) {
    privileges.require(http, InternalCallerAuth.PRIVILEGE_PARTNER_ADMIN, "approvePartnerUser");
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

  public record SubjectProfileRequest(@NotNull UUID subjectId) {}

  public record SubjectProfileResponse(
      UUID subjectId, UserType userType, Set<String> roleCodes, String status) {}

  public record CreatePartnerUserRequest(
      @NotBlank String username,
      @Email @NotBlank String email,
      String firstName,
      String lastName,
      @NotBlank String insurerCode) {}

  public record ApprovalResponse(UUID approvalId, String targetStatus) {}

  @PostMapping("/login/preview")
  public AccountView previewLogin(
      HttpServletRequest http, @Valid @RequestBody LoginIdRequest request) {
    privileges.require(http, InternalCallerAuth.PRIVILEGE_LOGIN_ORCHESTRATE, "previewLogin");
    return platformLogin.preview(request.userType(), request.loginId());
  }

  @PostMapping("/login/bank-password-result")
  public PasswordOutcome bankPasswordResult(
      HttpServletRequest http, @Valid @RequestBody BankPasswordResultRequest request) {
    privileges.require(http, InternalCallerAuth.PRIVILEGE_LOGIN_ORCHESTRATE, "bankPasswordResult");
    return platformLogin.recordBankPasswordResult(request.businessUserId(), request.accepted());
  }

  @PostMapping("/login/verify-platform-password")
  public PasswordOutcome verifyPlatformPassword(
      HttpServletRequest http, @Valid @RequestBody PlatformPasswordRequest request) {
    privileges.require(
        http, InternalCallerAuth.PRIVILEGE_LOGIN_ORCHESTRATE, "verifyPlatformPassword");
    return platformLogin.verifyPlatformPassword(request.businessUserId(), request.password());
  }

  @PostMapping("/login/complete")
  public AccountView completeLogin(
      HttpServletRequest http, @Valid @RequestBody UserIdRequest request) {
    privileges.require(http, InternalCallerAuth.PRIVILEGE_LOGIN_ORCHESTRATE, "completeLogin");
    return platformLogin.completeLogin(request.businessUserId());
  }

  @PostMapping("/login/unlock/assess")
  public AccountView assessUnlock(
      HttpServletRequest http, @Valid @RequestBody LoginIdRequest request) {
    privileges.require(http, InternalCallerAuth.PRIVILEGE_LOGIN_ORCHESTRATE, "assessUnlock");
    return platformLogin.assessUnlock(request.userType(), request.loginId());
  }

  @PostMapping("/login/unlock/complete")
  public AccountView completeUnlock(
      HttpServletRequest http, @Valid @RequestBody UserIdRequest request) {
    privileges.require(http, InternalCallerAuth.PRIVILEGE_LOGIN_ORCHESTRATE, "completeUnlock");
    return platformLogin.completeUnlock(request.businessUserId());
  }

  @PostMapping("/login/forgot/assess")
  public AccountView assessForgot(
      HttpServletRequest http, @Valid @RequestBody ForgotRequest request) {
    privileges.require(http, InternalCallerAuth.PRIVILEGE_LOGIN_ORCHESTRATE, "assessForgot");
    return platformLogin.assessForgotPassword(request.email());
  }

  @PostMapping("/login/password")
  public PasswordSetResult setPassword(
      HttpServletRequest http, @Valid @RequestBody SetPasswordRequest request) {
    privileges.require(http, InternalCallerAuth.PRIVILEGE_LOGIN_ORCHESTRATE, "setPassword");
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
