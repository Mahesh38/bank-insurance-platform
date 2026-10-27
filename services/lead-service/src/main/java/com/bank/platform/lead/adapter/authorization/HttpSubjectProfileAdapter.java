package com.bank.platform.lead.adapter.authorization;

import com.bank.common.error.ErrorCodes;
import com.bank.common.error.ServiceErrors;
import com.bank.platform.lead.domain.SubjectProfilePort;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * {@code POST /internal/v1/identities/profile} on identity-authorization-service. Fail closed: a
 * missing or errored profile is unauthorized for the exception decision path.
 */
@Component
public class HttpSubjectProfileAdapter implements SubjectProfilePort {

  private static final Logger log = LoggerFactory.getLogger(HttpSubjectProfileAdapter.class);

  static final String INTERNAL_KEY_HEADER = "X-Internal-Service-Key";

  private final RestClient client;
  private final ServiceErrors errors;

  public HttpSubjectProfileAdapter(
      RestClient.Builder builder,
      ServiceErrors errors,
      @Value("${lead.authorization.base-url}") String baseUrl,
      @Value("${lead.authorization.decision-timeout:300ms}") Duration timeout,
      @Value("${lead.authorization.internal-key}") String internalKey) {
    this.errors = errors;
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
  public SubjectProfile load(String businessUserId) {
    try {
      ProfileResponse body =
          client
              .post()
              .uri("/internal/v1/identities/profile")
              .body(new ProfileRequest(businessUserId))
              .retrieve()
              .body(ProfileResponse.class);
      if (body == null || body.userType() == null || body.userType().isBlank()) {
        throw unavailable("profile response was empty");
      }
      return new SubjectProfile(body.userType(), body.roleCodes());
    } catch (RestClientException ex) {
      log.warn("identity profile did not answer ({}): fail closed", ex.getClass().getSimpleName());
      throw unavailable("identity profile did not answer");
    }
  }

  private RuntimeException unavailable(String reason) {
    return errors
        .error(ErrorCodes.UNAUTHORIZED)
        .component("HttpSubjectProfileAdapter")
        .operation("load")
        .reason(reason)
        .build();
  }

  /** Jackson serialises the field as {@code subjectId} for identity-authorization. */
  record ProfileRequest(String subjectId) {}

  record ProfileResponse(String userType, Set<String> roleCodes) {}
}
