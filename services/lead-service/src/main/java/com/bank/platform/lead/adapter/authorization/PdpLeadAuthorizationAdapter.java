package com.bank.platform.lead.adapter.authorization;

import com.bank.platform.lead.domain.LeadAuthorizationPort;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Lead's own call to {@code POST /internal/v1/authorization/decisions} (S-02 budget: 300 ms, no
 * retry). Fail closed: a timeout, an error status or an empty body is {@code unavailable}, never an
 * allow. A principal that is not a business-user UUID (the BFF falls back to the username when a
 * session has none) is denied without a call.
 *
 * <p>The PDP authenticates its callers ({@code X-Internal-Service-Key}, READINESS-REVIEW §3.7);
 * lead-service presents its own key from {@code lead.authorization.internal-key}.
 */
@Component
public class PdpLeadAuthorizationAdapter implements LeadAuthorizationPort {

  private static final Logger log = LoggerFactory.getLogger(PdpLeadAuthorizationAdapter.class);

  static final String INTERNAL_KEY_HEADER = "X-Internal-Service-Key";
  static final String EXCEPTION_ACTION_PREFIX = "lead.exception.";

  private final RestClient client;

  public PdpLeadAuthorizationAdapter(
      RestClient.Builder builder,
      @Value("${lead.authorization.base-url}") String baseUrl,
      @Value("${lead.authorization.decision-timeout:300ms}") Duration timeout,
      @Value("${lead.authorization.internal-key}") String internalKey) {
    HttpClient httpClient =
        HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(timeout)
            .build();
    JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
    factory.setReadTimeout(timeout);
    this.client =
        builder
            .clone()
            .requestFactory(factory)
            .baseUrl(baseUrl)
            .defaultHeader(INTERNAL_KEY_HEADER, internalKey)
            .build();
  }

  @Override
  public AuthorizationDecision decide(
      String principalId, String action, String leadId, String branchId) {
    UUID subjectId;
    try {
      subjectId = UUID.fromString(principalId);
    } catch (IllegalArgumentException | NullPointerException ex) {
      return AuthorizationDecision.deny("UNKNOWN_SUBJECT");
    }
    // partnerVisible=false is what makes the PDP refuse an insurer representative on an existing
    // lead (RESOURCE_NOT_SHARED_WITH_PARTNER) — and on an exception decision always (E6), so a
    // partner can never approve even if a role grant were misconfigured.
    boolean partnerVisible = leadId == null && !action.startsWith(EXCEPTION_ACTION_PREFIX);
    var request =
        new DecisionRequest(
            subjectId,
            action,
            new Resource("lead", leadId, branchId, null, null, Set.of(), partnerVisible, false));
    try {
      Decision decision =
          client
              .post()
              .uri("/internal/v1/authorization/decisions")
              .body(request)
              .retrieve()
              .body(Decision.class);
      if (decision == null) {
        return AuthorizationDecision.unavailableDecision();
      }
      return decision.allowed()
          ? AuthorizationDecision.allow(decision.reasonCode())
          : AuthorizationDecision.deny(decision.reasonCode());
    } catch (RestClientException ex) {
      log.warn(
          "PDP did not answer for {} ({}): fail closed", action, ex.getClass().getSimpleName());
      return AuthorizationDecision.unavailableDecision();
    }
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
