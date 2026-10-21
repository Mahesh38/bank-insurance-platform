package com.bank.workforce.bff.client;

import com.bank.common.error.ErrorCodes;
import com.bank.common.error.ServiceErrors;
import com.bank.common.error.ServiceException;
import com.bank.workforce.bff.config.DownstreamProperties;
import com.bank.workforce.bff.session.SessionModels.WorkforceSession;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Asks the authorization service (the PDP) whether the session's subject may perform an action,
 * before the BFF forwards a mutating call downstream (SUG-20261021-gap).
 *
 * <p>Fails closed: a timeout, a transport error or an unreadable answer is {@code
 * AUTHORIZATION_UNAVAILABLE}, never an allow. The PDP has its own client with a 300 ms budget so a
 * slow PDP cannot hold an RM's request for the general 5 s read timeout.
 */
@Component
public class PolicyDecisionClient {

  private final RestClient client;
  private final ServiceErrors serviceErrors;

  public PolicyDecisionClient(
      RestClient.Builder builder,
      DownstreamProperties properties,
      ServiceErrors serviceErrors,
      @Value("${workforce.authorization.decision-timeout:300ms}") Duration decisionTimeout) {
    HttpClient httpClient =
        HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(decisionTimeout)
            .build();
    JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
    factory.setReadTimeout(decisionTimeout);
    this.client =
        builder
            .clone()
            .requestFactory(factory)
            .baseUrl(properties.authorizationServiceBaseUrl().toString())
            .build();
    this.serviceErrors = serviceErrors;
  }

  /**
   * Returns normally when allowed; throws the typed denial otherwise.
   *
   * @param resourceType e.g. {@code lead}
   * @param resourceId null when the resource does not exist yet (create)
   * @param branchCode the branch the action is scoped to, if known
   */
  public void require(
      WorkforceSession session,
      String action,
      String resourceType,
      String resourceId,
      String branchCode,
      boolean regulatedAction) {
    UUID subjectId = session.businessUserId();
    if (subjectId == null) {
      throw deny(ErrorCodes.DEFAULT_DENY, action, "Session has no resolved business user");
    }
    var request =
        new DecisionRequest(
            subjectId,
            action,
            new Resource(
                resourceType,
                resourceId,
                branchCode,
                session.insurerCode(),
                null,
                Set.of(),
                // Sharing is decided by the owning service for an existing resource; for create
                // there is nothing to share yet.
                resourceId == null,
                regulatedAction));
    Decision decision;
    try {
      decision =
          client
              .post()
              .uri("/internal/v1/authorization/decisions")
              .body(request)
              .retrieve()
              .body(Decision.class);
    } catch (RestClientException ex) {
      throw serviceErrors
          .error(ErrorCodes.AUTHORIZATION_UNAVAILABLE)
          .component("PolicyDecisionClient")
          .operation(action)
          .reason("Policy decision point did not answer")
          .cause(ex)
          .build();
    }
    if (decision == null) {
      throw serviceErrors
          .error(ErrorCodes.AUTHORIZATION_UNAVAILABLE)
          .component("PolicyDecisionClient")
          .operation(action)
          .reason("Policy decision point returned no decision")
          .build();
    }
    if (!decision.allowed()) {
      throw deny(errorCodeFor(decision.reasonCode()), action, decision.reasonCode());
    }
  }

  static String errorCodeFor(String reasonCode) {
    if (reasonCode == null) {
      return ErrorCodes.DEFAULT_DENY;
    }
    return switch (reasonCode) {
      case "ACCOUNT_NOT_ACTIVE" -> ErrorCodes.ACCOUNT_SUSPENDED;
      case "CROSS_INSURER_ACCESS" -> ErrorCodes.CROSS_INSURER_DENIED;
      case "EXPLICIT_DENY" -> ErrorCodes.EXPLICIT_DENY;
      case "BRANCH_OUT_OF_SCOPE" -> ErrorCodes.OUT_OF_BRANCH_SCOPE;
        // Do not confirm that an unshared resource exists.
      case "RESOURCE_NOT_SHARED_WITH_PARTNER" -> ErrorCodes.RESOURCE_NOT_FOUND;
      case "MANDATORY_CERTIFICATION_INVALID" -> ErrorCodes.SP_CERTIFICATION_REQUIRED;
      default -> ErrorCodes.DEFAULT_DENY;
    };
  }

  private ServiceException deny(String code, String action, String reason) {
    return serviceErrors
        .error(code)
        .component("PolicyDecisionClient")
        .operation(action)
        .reason(reason)
        .build();
  }

  record DecisionRequest(UUID subjectId, String action, Resource resource) {}

  record Resource(
      String type,
      String id,
      String branchCode,
      String insurerCode,
      String ownerId,
      Set<String> assignedUserIds,
      boolean partnerVisible,
      boolean regulatedAction) {}

  record Decision(boolean allowed, String reasonCode, String matchedPolicy, long policyVersion) {}
}
