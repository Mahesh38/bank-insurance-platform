package com.bank.platform.quotation.adapter.consent;

import com.bank.common.error.ErrorCodes;
import com.bank.common.error.ServiceErrors;
import com.bank.common.security.internal.InternalCallerAuth;
import com.bank.platform.quotation.config.QuotationClientProperties;
import com.bank.platform.quotation.domain.ConsentCheckpointPort;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** {@code GET /internal/v1/consents:check?checkpoint=BEFORE_MULTI_QUOTE} (D5, 500 ms, no retry). */
@Component
@ConditionalOnProperty(name = "quotation.gates.stub", havingValue = "false", matchIfMissing = true)
public class HttpConsentCheckpointAdapter implements ConsentCheckpointPort {

  private static final String COMPONENT = "HttpConsentCheckpointAdapter";
  private static final String CHECKPOINT = "BEFORE_MULTI_QUOTE";

  private final RestClient client;
  private final QuotationClientProperties properties;
  private final ServiceErrors errors;

  public HttpConsentCheckpointAdapter(
      @Qualifier("consentRestClient") RestClient client,
      QuotationClientProperties properties,
      ServiceErrors errors) {
    this.client = client;
    this.properties = properties;
    this.errors = errors;
  }

  @Override
  public String requireBeforeMultiQuote(String customerId) {
    JsonNode body;
    try {
      body =
          client
              .get()
              .uri(
                  uriBuilder ->
                      uriBuilder
                          .path("/internal/v1/consents:check")
                          .queryParam("customerId", customerId)
                          .queryParam("checkpoint", CHECKPOINT)
                          .queryParam("lob", "LIFE")
                          .build())
              .header(InternalCallerAuth.HEADER, properties.consent().internalServiceKey())
              .retrieve()
              .body(JsonNode.class);
    } catch (RestClientException e) {
      throw errors
          .error(ErrorCodes.UPSTREAM_UNAVAILABLE)
          .component(COMPONENT)
          .operation("requireBeforeMultiQuote")
          .reason("consent checkpoint unreachable")
          .cause(e)
          .build();
    }
    if (body == null || !body.path("satisfied").asBoolean(false)) {
      List<String> missing = missingPurposes(body);
      throw errors
          .error(ErrorCodes.CONSENT_REQUIRED)
          .component(COMPONENT)
          .operation("requireBeforeMultiQuote")
          .reason(
              missing.isEmpty()
                  ? "BEFORE_MULTI_QUOTE not satisfied"
                  : "missing purposes " + missing)
          .build();
    }
    String consentId = firstConsentId(body);
    if (consentId == null || consentId.isBlank()) {
      throw errors
          .error(ErrorCodes.UPSTREAM_UNAVAILABLE)
          .component(COMPONENT)
          .operation("requireBeforeMultiQuote")
          .reason("consent check satisfied without consentId")
          .build();
    }
    return consentId;
  }

  private static String firstConsentId(JsonNode body) {
    JsonNode valid = body == null ? null : body.get("valid");
    if (valid == null || !valid.isArray() || valid.isEmpty()) {
      return null;
    }
    return valid.get(0).path("consentId").asText(null);
  }

  private static List<String> missingPurposes(JsonNode body) {
    List<String> missing = new ArrayList<>();
    JsonNode nodes = body == null ? null : body.get("missing");
    if (nodes == null || !nodes.isArray()) {
      return missing;
    }
    nodes.forEach(n -> missing.add(n.path("purpose").asText()));
    return missing;
  }
}
