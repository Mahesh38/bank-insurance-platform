package com.bank.platform.quotation.adapter.hub;

import com.bank.common.error.ErrorCodes;
import com.bank.common.error.ServiceErrors;
import com.bank.common.security.internal.InternalCallerAuth;
import com.bank.platform.quotation.config.QuotationClientProperties;
import com.bank.platform.quotation.domain.HubQuotePort;
import com.bank.platform.quotation.domain.NewQuote;
import com.bank.platform.quotation.domain.QuoteMember;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Hub canonical quote client — {@code POST /internal/v1/provider-requests} submit and poll (POST).
 */
@Component
public class HttpHubQuoteAdapter implements HubQuotePort {

  private static final String COMPONENT = "HttpHubQuoteAdapter";

  private final RestClient submitClient;
  private final RestClient pollClient;
  private final QuotationClientProperties properties;
  private final ServiceErrors errors;

  public HttpHubQuoteAdapter(
      @Qualifier("hubSubmitRestClient") RestClient submitClient,
      @Qualifier("hubPollRestClient") RestClient pollClient,
      QuotationClientProperties properties,
      ServiceErrors errors) {
    this.submitClient = submitClient;
    this.pollClient = pollClient;
    this.properties = properties;
    this.errors = errors;
  }

  @Override
  public Accepted submitQuote(
      String quoteId, NewQuote request, String actorId, String correlationId) {
    Map<String, Object> body = toHubBody(quoteId, request);
    try {
      JsonNode response =
          submitClient
              .post()
              .uri("/internal/v1/provider-requests")
              .contentType(MediaType.APPLICATION_JSON)
              .header(InternalCallerAuth.HEADER, properties.hub().internalServiceKey())
              .header("Idempotency-Key", "quote:" + quoteId + ":submit")
              .header("X-Actor-Id", actorId == null ? "quotation-service" : actorId)
              .headers(
                  headers -> {
                    if (correlationId != null && !correlationId.isBlank()) {
                      headers.set("X-Correlation-Id", correlationId);
                    }
                  })
              .body(body)
              .retrieve()
              .body(JsonNode.class);
      if (response == null) {
        throw upstream("submitQuote", "empty Hub accept body", null);
      }
      return new Accepted(
          text(response, "providerRequestId"), text(response, "provider"), text(response, "state"));
    } catch (RestClientException e) {
      throw upstream("submitQuote", "Hub submit failed", e);
    }
  }

  @Override
  public PollStatus poll(String providerRequestId) {
    try {
      JsonNode response =
          pollClient
              .post()
              .uri("/internal/v1/provider-requests/{id}", providerRequestId)
              .header(InternalCallerAuth.HEADER, properties.hub().internalServiceKey())
              .retrieve()
              .body(JsonNode.class);
      if (response == null) {
        throw upstream("poll", "empty Hub poll body", null);
      }
      return toStatus(response);
    } catch (RestClientException e) {
      throw upstream("poll", "Hub poll failed", e);
    }
  }

  public static Map<String, Object> toHubBody(String quoteId, NewQuote request) {
    Map<String, Object> root = new LinkedHashMap<>();
    root.put("operation", "QUOTE");
    root.put("lob", request.lob());
    root.put("productClass", request.productClass());
    root.put("callerRef", Map.of("context", "quotation", "aggregateId", quoteId));
    root.put("journeyId", request.journeyId());
    root.put("mode", request.mode());

    Map<String, Object> body = new LinkedHashMap<>();
    body.put("category", request.category());
    body.put("requestedCover", request.requestedCover());
    if (request.premiumAmount() != null) {
      body.put("premiumAmount", request.premiumAmount());
    }
    List<Map<String, Object>> members = new ArrayList<>();
    for (QuoteMember member : request.members()) {
      Map<String, Object> m = new LinkedHashMap<>();
      m.put("role", member.role());
      m.put("sequence", member.sequence());
      m.put("dob", member.dob() == null ? null : member.dob().toString());
      m.put("gender", member.gender());
      m.put("tobacco", member.tobacco());
      if (member.annualIncome() != null) {
        m.put("annualIncome", member.annualIncome());
      }
      if (member.pincode() != null) {
        m.put("pincode", member.pincode());
      }
      members.add(m);
    }
    body.put("members", members);
    if (request.preferences() != null) {
      Map<String, Object> prefs = new LinkedHashMap<>();
      NewQuote.Preferences p = request.preferences();
      if (p.premiumFrequency() != null) {
        prefs.put("premiumFrequency", p.premiumFrequency());
      }
      if (p.policyTerm() != null) {
        prefs.put("policyTerm", p.policyTerm());
      }
      if (p.premiumPayingTerm() != null) {
        prefs.put("premiumPayingTerm", p.premiumPayingTerm());
      }
      if (!prefs.isEmpty()) {
        body.put("preferences", prefs);
      }
    }
    root.put("body", body);
    return root;
  }

  private static PollStatus toStatus(JsonNode response) {
    List<HubOffer> offers = new ArrayList<>();
    List<HubPartialError> errors = new ArrayList<>();
    JsonNode result = response.get("result");
    if (result != null) {
      JsonNode offerNodes = result.get("offers");
      if (offerNodes != null && offerNodes.isArray()) {
        offerNodes.forEach(n -> offers.add(toOffer(n)));
      }
      JsonNode errorNodes = result.get("partialErrors");
      if (errorNodes != null && errorNodes.isArray()) {
        errorNodes.forEach(
            n -> errors.add(new HubPartialError(text(n, "insurerCode"), text(n, "code"))));
      }
    }
    return new PollStatus(
        text(response, "providerRequestId"),
        text(response, "state"),
        text(response, "provider"),
        offers,
        errors);
  }

  private static HubOffer toOffer(JsonNode n) {
    return new HubOffer(
        text(n, "providerOfferRef"),
        text(n, "insurerCode"),
        text(n, "productCode"),
        textOrNull(n, "productName"),
        decimal(n, "premiumAmount"),
        textOrNull(n, "premiumFrequency"),
        decimal(n, "sumAssured"),
        n.path("outOfBound").asBoolean(false),
        intOrNull(n, "coverTerm"),
        intOrNull(n, "premiumPayingTerm"));
  }

  private RuntimeException upstream(String operation, String reason, Exception cause) {
    var builder =
        errors
            .error(ErrorCodes.UPSTREAM_UNAVAILABLE)
            .component(COMPONENT)
            .operation(operation)
            .reason(reason);
    if (cause != null) {
      builder.cause(cause);
    }
    return builder.build();
  }

  private static String text(JsonNode n, String field) {
    JsonNode v = n.get(field);
    return v == null || v.isNull() ? null : v.asText();
  }

  private static String textOrNull(JsonNode n, String field) {
    return text(n, field);
  }

  private static BigDecimal decimal(JsonNode n, String field) {
    JsonNode v = n.get(field);
    if (v == null || v.isNull()) {
      return null;
    }
    return v.isNumber() ? v.decimalValue() : new BigDecimal(v.asText());
  }

  private static Integer intOrNull(JsonNode n, String field) {
    JsonNode v = n.get(field);
    if (v == null || v.isNull()) {
      return null;
    }
    return v.asInt();
  }
}
