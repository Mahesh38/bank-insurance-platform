package com.bank.platform.integration.hub.adapter.onesb;

import com.bank.common.error.ErrorCodes;
import com.bank.common.error.ServiceErrors;
import com.bank.common.security.internal.InternalCallerAuth;
import com.bank.platform.integration.hub.domain.CanonicalOffer;
import com.bank.platform.integration.hub.domain.OnesbQuotePort;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * HTTP client for {@code 1sb-integration-service} {@code /v1/quotes}. Wire JSON only — never
 * imports {@code com.bank.insurance.onesb} (INV-ACL-01 / AT-HUB-12).
 */
@Component
public class HttpOnesbQuoteAdapter implements OnesbQuotePort {

  private final RestClient client;
  private final OnesbClientProperties properties;
  private final ServiceErrors errors;

  public HttpOnesbQuoteAdapter(
      RestClient onesbRestClient, OnesbClientProperties properties, ServiceErrors errors) {
    this.client = Objects.requireNonNull(onesbRestClient);
    this.properties = Objects.requireNonNull(properties);
    this.errors = Objects.requireNonNull(errors);
  }

  @Override
  public SubmitOutcome submitQuote(AdapterQuoteSubmit submit) {
    Map<String, Object> body = toBody(submit);
    try {
      JsonNode response =
          client
              .post()
              .uri("/v1/quotes")
              .contentType(MediaType.APPLICATION_JSON)
              .header(InternalCallerAuth.HEADER, properties.internalServiceKey())
              .header("Idempotency-Key", submit.idempotencyKey())
              .header("X-Actor-Id", submit.actorId())
              .header("X-Correlation-Id", submit.correlationId())
              .body(body)
              .retrieve()
              .body(JsonNode.class);
      if (response == null || response.path("jobId").asText(null) == null) {
        throw errors
            .error(ErrorCodes.UPSTREAM_BAD_RESPONSE)
            .component("HttpOnesbQuoteAdapter")
            .operation("submitQuote")
            .reason("adapter submit missing jobId")
            .build();
      }
      return new SubmitOutcome.Accepted(response.path("jobId").asText());
    } catch (ResourceAccessException ex) {
      if (isTimeout(ex)) {
        return new SubmitOutcome.Uncertain();
      }
      throw errors
          .error(ErrorCodes.UPSTREAM_UNAVAILABLE)
          .component("HttpOnesbQuoteAdapter")
          .operation("submitQuote")
          .reason("adapter submit transport failure")
          .build();
    } catch (RestClientResponseException ex) {
      throw translate(ex, "submitQuote");
    }
  }

  @Override
  public AdapterQuoteJob getQuoteJob(String jobId) {
    try {
      JsonNode response =
          client
              .get()
              .uri("/v1/quotes/{jobId}", jobId)
              .header(InternalCallerAuth.HEADER, properties.internalServiceKey())
              .retrieve()
              .body(JsonNode.class);
      if (response == null) {
        throw errors
            .error(ErrorCodes.UPSTREAM_BAD_RESPONSE)
            .component("HttpOnesbQuoteAdapter")
            .operation("getQuoteJob")
            .reason("empty adapter poll body")
            .build();
      }
      return fromJobNode(response);
    } catch (ResourceAccessException ex) {
      throw errors
          .error(ErrorCodes.UPSTREAM_UNAVAILABLE)
          .component("HttpOnesbQuoteAdapter")
          .operation("getQuoteJob")
          .reason("adapter poll transport failure")
          .build();
    } catch (RestClientResponseException ex) {
      throw translate(ex, "getQuoteJob");
    }
  }

  private static Map<String, Object> toBody(AdapterQuoteSubmit submit) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("lob", submit.lob());
    body.put("mode", submit.mode());
    body.put("category", submit.category());
    body.put("sumAssured", submit.sumAssured());
    if (submit.premiumAmount() != null) {
      body.put("premiumAmount", submit.premiumAmount());
    }
    List<Map<String, Object>> members = new ArrayList<>();
    for (AdapterMember member : submit.members()) {
      Map<String, Object> m = new LinkedHashMap<>();
      m.put("role", member.role());
      m.put("sequenceNumber", member.sequenceNumber());
      m.put("dob", member.dob());
      m.put("gender", member.gender());
      if (member.tobacco() != null) {
        m.put("tobacco", member.tobacco());
      }
      if (member.annualIncome() != null) {
        m.put("annualIncome", member.annualIncome());
      }
      if (member.pincode() != null) {
        m.put("pincode", member.pincode());
      }
      if (member.relationship() != null) {
        m.put("relationship", member.relationship());
      }
      members.add(m);
    }
    body.put("members", members);
    if (submit.preferences() != null && !submit.preferences().isEmpty()) {
      body.put("preferences", submit.preferences());
    }
    if (submit.distribution() != null) {
      Map<String, Object> distribution = new LinkedHashMap<>();
      distribution.put("rmEmployeeId", submit.distribution().rmEmployeeId());
      distribution.put("agentId", submit.distribution().agentId());
      distribution.put("channelType", submit.distribution().channelType());
      body.put("distribution", distribution);
    }
    if (submit.journeyId() != null) {
      body.put("journeyId", submit.journeyId());
    }
    if (submit.selection() != null) {
      Map<String, Object> selection = new LinkedHashMap<>();
      selection.put("insurerCode", submit.selection().insurerCode());
      selection.put("productCodes", submit.selection().productCodes());
      putIfPresent(selection, "planOption", submit.selection().planOption());
      putIfPresent(selection, "coverOption", submit.selection().coverOption());
      putIfPresent(selection, "deathBenefitOption", submit.selection().deathBenefitOption());
      putIfPresent(selection, "policyTerm", submit.selection().policyTerm());
      putIfPresent(selection, "premiumPaymentTerm", submit.selection().premiumPaymentTerm());
      putIfPresent(selection, "premiumFrequency", submit.selection().premiumFrequency());
      putIfPresent(selection, "premiumPaymentOption", submit.selection().premiumPaymentOption());
      body.put("selection", selection);
    }
    return body;
  }

  private static void putIfPresent(Map<String, Object> map, String key, Object value) {
    if (value != null) {
      map.put(key, value);
    }
  }

  private static AdapterQuoteJob fromJobNode(JsonNode node) {
    List<AdapterOffer> offers = new ArrayList<>();
    JsonNode offersNode = node.path("offers");
    if (offersNode.isArray()) {
      for (JsonNode offer : offersNode) {
        offers.add(
            new AdapterOffer(
                text(offer, "offerId"),
                text(offer, "insurerCode"),
                text(offer, "insurerName"),
                text(offer, "productCode"),
                text(offer, "productName"),
                decimal(offer, "premiumAmount"),
                text(offer, "premiumFrequency"),
                decimal(offer, "sumAssured"),
                offer.path("outOfBound").asBoolean(false),
                text(offer, "offerStatus"),
                text(offer, "errorSummary"),
                intOrNull(offer, "coverTerm"),
                intOrNull(offer, "premiumPayingTerm"),
                funds(offer.path("funds"))));
      }
    }
    return new AdapterQuoteJob(
        text(node, "jobId"),
        text(node, "status"),
        text(node, "failureReason"),
        List.copyOf(offers));
  }

  private static List<CanonicalOffer.FundAllocation> funds(JsonNode fundsNode) {
    if (!fundsNode.isArray()) {
      return List.of();
    }
    List<CanonicalOffer.FundAllocation> funds = new ArrayList<>();
    for (JsonNode fund : fundsNode) {
      funds.add(
          new CanonicalOffer.FundAllocation(
              text(fund, "code"), text(fund, "name"), decimal(fund, "allocationPercent")));
    }
    return List.copyOf(funds);
  }

  private static String text(JsonNode node, String field) {
    JsonNode value = node.path(field);
    return value.isMissingNode() || value.isNull() ? null : value.asText();
  }

  private static BigDecimal decimal(JsonNode node, String field) {
    JsonNode value = node.path(field);
    if (value.isMissingNode() || value.isNull()) {
      return null;
    }
    return value.isNumber() ? value.decimalValue() : new BigDecimal(value.asText());
  }

  private static Integer intOrNull(JsonNode node, String field) {
    JsonNode value = node.path(field);
    if (value.isMissingNode() || value.isNull()) {
      return null;
    }
    return value.asInt();
  }

  private static boolean isTimeout(ResourceAccessException ex) {
    for (Throwable t = ex; t != null; t = t.getCause()) {
      if (t instanceof SocketTimeoutException) {
        return true;
      }
    }
    return false;
  }

  private RuntimeException translate(RestClientResponseException ex, String operation) {
    int status = ex.getStatusCode().value();
    String code =
        status == 404
            ? ErrorCodes.RESOURCE_NOT_FOUND
            : status >= 500 ? ErrorCodes.UPSTREAM_UNAVAILABLE : ErrorCodes.UPSTREAM_BAD_RESPONSE;
    return errors
        .error(code)
        .component("HttpOnesbQuoteAdapter")
        .operation(operation)
        .reason("adapter returned HTTP " + status)
        .build();
  }
}
