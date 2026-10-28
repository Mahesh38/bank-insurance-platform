package com.bank.platform.quotation.adapter.suitability;

import com.bank.common.error.ErrorCodes;
import com.bank.common.error.ServiceErrors;
import com.bank.common.security.internal.InternalCallerAuth;
import com.bank.platform.quotation.config.QuotationClientProperties;
import com.bank.platform.quotation.domain.SuitabilityGatePort;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/** {@code GET /internal/v1/evaluations:usable} (S-08, 500 ms, no retry, fail closed). */
@Component
@ConditionalOnProperty(name = "quotation.gates.stub", havingValue = "false", matchIfMissing = true)
public class HttpSuitabilityGateAdapter implements SuitabilityGatePort {

  private static final String COMPONENT = "HttpSuitabilityGateAdapter";

  private final RestClient client;
  private final QuotationClientProperties properties;
  private final ServiceErrors errors;

  public HttpSuitabilityGateAdapter(
      @Qualifier("suitabilityRestClient") RestClient client,
      QuotationClientProperties properties,
      ServiceErrors errors) {
    this.client = client;
    this.properties = properties;
    this.errors = errors;
  }

  @Override
  public void requireUsable(
      String customerId, String lob, String productClass, String evaluationId) {
    try {
      client
          .get()
          .uri(
              uriBuilder ->
                  uriBuilder
                      .path("/internal/v1/evaluations:usable")
                      .queryParam("customerId", customerId)
                      .queryParam("lob", lob)
                      .queryParam("productClass", productClass)
                      .queryParam("evaluationId", evaluationId)
                      .build())
          .header(InternalCallerAuth.HEADER, properties.suitability().internalServiceKey())
          .retrieve()
          .toBodilessEntity();
    } catch (RestClientResponseException e) {
      HttpStatusCode status = e.getStatusCode();
      if (status.value() == 403) {
        throw errors
            .error(ErrorCodes.SUITABILITY_REQUIRED)
            .component(COMPONENT)
            .operation("requireUsable")
            .reason("suitability refused usable evaluation")
            .build();
      }
      throw upstream(e);
    } catch (RestClientException e) {
      throw upstream(e);
    }
  }

  private RuntimeException upstream(Exception cause) {
    return errors
        .error(ErrorCodes.UPSTREAM_UNAVAILABLE)
        .component(COMPONENT)
        .operation("requireUsable")
        .reason("suitability gate unreachable")
        .cause(cause)
        .build();
  }
}
