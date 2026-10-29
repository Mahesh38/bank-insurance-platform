package com.bank.workforce.bff.client;

import com.bank.common.error.ErrorCodes;
import com.bank.common.error.ErrorPropagation;
import com.bank.common.error.PlatformLayer;
import com.bank.common.error.ProblemJsonReader;
import com.bank.common.error.ServiceErrors;
import com.bank.workforce.bff.config.DownstreamProperties;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class IdentityAuthorizationClient {

  private static final String DOWNSTREAM = "authz";

  private final RestClient client;
  private final ProblemJsonReader problemJsonReader;
  private final ServiceErrors serviceErrors;

  public IdentityAuthorizationClient(
      @Qualifier("authorizationRestClient") RestClient restClient,
      DownstreamProperties properties,
      ProblemJsonReader problemJsonReader,
      ServiceErrors serviceErrors) {
    this.client =
        restClient
            .mutate()
            .baseUrl(properties.authorizationServiceBaseUrl().toString())
            .defaultHeader(
                DownstreamProperties.AUTHORIZATION_INTERNAL_KEY_HEADER,
                properties.authorizationServiceInternalKey())
            .build();
    this.problemJsonReader = problemJsonReader;
    this.serviceErrors = serviceErrors;
  }

  /** Resolves a Keycloak partner subject to a platform business user. */
  public ResolvedIdentity resolvePartner(IdentityProviderClient.ProviderSession providerSession) {
    String employeeId = claim(providerSession.claims(), "employee_id", "emp_id");
    var request =
        new ResolveIdentityRequest(
            "KEYCLOAK",
            providerSession.providerSubjectId(),
            "INSURER_REPRESENTATIVE",
            providerSession.username(),
            providerSession.email(),
            employeeId);
    try {
      return client
          .post()
          .uri("/internal/v1/identities/resolve")
          .body(request)
          .retrieve()
          .body(ResolvedIdentity.class);
    } catch (RestClientResponseException ex) {
      // The authorization service's refusal is the one the RM can act on. Losing it here —
      // as a bare RestClientResponseException reaching Spring's default 500 — is how a
      // "you are not permitted" becomes an unattributable server error on the RM's screen.
      throw ErrorPropagation.from(
              problemJsonReader.read(ex.getResponseBodyAsString(), ex.getStatusCode().value()))
          .receivedBy(serviceErrors.serviceId(), PlatformLayer.L4)
          .calling(DOWNSTREAM, "resolveIdentity")
          .causedBy(ex)
          .toException();
    } catch (RestClientException ex) {
      throw serviceErrors
          .error(ErrorCodes.IDENTITY_PROVIDER_UNAVAILABLE)
          .component("IdentityAuthorizationClient")
          .operation("resolveIdentity")
          .reason("Authorization service is unavailable")
          .cause(ex)
          .build();
    }
  }

  public AccountView preview(String userType, String loginId) {
    return post(
        "/internal/v1/login/preview",
        new LoginIdRequest(userType, loginId),
        AccountView.class,
        "previewLogin");
  }

  public PasswordOutcome recordBankPassword(UUID businessUserId, boolean accepted) {
    return post(
        "/internal/v1/login/bank-password-result",
        new BankPasswordResultRequest(businessUserId, accepted),
        PasswordOutcome.class,
        "recordBankPassword");
  }

  public PasswordOutcome verifyPlatformPassword(UUID businessUserId, String password) {
    return post(
        "/internal/v1/login/verify-platform-password",
        new PlatformPasswordRequest(businessUserId, password),
        PasswordOutcome.class,
        "verifyPlatformPassword");
  }

  public AccountView completeLogin(UUID businessUserId) {
    return post(
        "/internal/v1/login/complete",
        new UserIdRequest(businessUserId),
        AccountView.class,
        "completeLogin");
  }

  public AccountView assessUnlock(String userType, String loginId) {
    return post(
        "/internal/v1/login/unlock/assess",
        new LoginIdRequest(userType, loginId),
        AccountView.class,
        "assessUnlock");
  }

  public AccountView completeUnlock(UUID businessUserId) {
    return post(
        "/internal/v1/login/unlock/complete",
        new UserIdRequest(businessUserId),
        AccountView.class,
        "completeUnlock");
  }

  public AccountView assessForgotPassword(String email) {
    return post(
        "/internal/v1/login/forgot/assess",
        new ForgotRequest(email),
        AccountView.class,
        "assessForgotPassword");
  }

  public PasswordSetResult setPassword(
      UUID businessUserId, String newPassword, String confirmPassword, String purpose) {
    return post(
        "/internal/v1/login/password",
        new SetPasswordRequest(businessUserId, newPassword, confirmPassword, purpose),
        PasswordSetResult.class,
        "setPassword");
  }

  private <T> T post(String path, Object body, Class<T> type, String operation) {
    try {
      return client.post().uri(path).body(body).retrieve().body(type);
    } catch (RestClientResponseException ex) {
      throw ErrorPropagation.from(
              problemJsonReader.read(ex.getResponseBodyAsString(), ex.getStatusCode().value()))
          .receivedBy(serviceErrors.serviceId(), PlatformLayer.L4)
          .calling(DOWNSTREAM, operation)
          .causedBy(ex)
          .toException();
    } catch (RestClientException ex) {
      throw serviceErrors
          .error(ErrorCodes.IDENTITY_PROVIDER_UNAVAILABLE)
          .component("IdentityAuthorizationClient")
          .operation(operation)
          .reason("Authorization service is unavailable")
          .cause(ex)
          .build();
    }
  }

  private static String claim(Map<String, Object> claims, String... names) {
    for (String name : names) {
      Object value = claims.get(name);
      if (value != null) {
        return value.toString();
      }
    }
    return null;
  }

  private record ResolveIdentityRequest(
      String provider,
      String providerSubject,
      String userType,
      String username,
      String email,
      String employeeId) {}

  public record ResolvedIdentity(
      UUID businessUserId,
      String status,
      long policyVersion,
      String userType,
      String insurerCode) {}

  public record AccountView(
      boolean found,
      String route,
      UUID businessUserId,
      String status,
      long policyVersion,
      String userType,
      String insurerCode,
      String firstName,
      String username,
      String maskedMobile,
      String maskedEmail,
      int attemptsRemaining,
      boolean firstLogin) {}

  public record PasswordOutcome(AccountView account, boolean accepted) {}

  public record PasswordSetResult(String result, String violation) {}

  private record LoginIdRequest(String userType, String loginId) {}

  private record UserIdRequest(UUID businessUserId) {}

  private record BankPasswordResultRequest(UUID businessUserId, boolean accepted) {}

  private record PlatformPasswordRequest(UUID businessUserId, String password) {
    @Override
    public String toString() {
      return "PlatformPasswordRequest[businessUserId=" + businessUserId + "]";
    }
  }

  private record ForgotRequest(String email) {}

  private record SetPasswordRequest(
      UUID businessUserId, String newPassword, String confirmPassword, String purpose) {
    @Override
    public String toString() {
      return "SetPasswordRequest[businessUserId=" + businessUserId + ", purpose=" + purpose + "]";
    }
  }
}
