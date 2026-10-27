package com.bank.platform.suitability.adapter.link;

import com.bank.common.security.internal.InternalCallerAuth;
import com.bank.platform.suitability.domain.BrdCopy;
import com.bank.platform.suitability.domain.JourneyModel.ConsentLinks;
import com.bank.platform.suitability.domain.JourneyModel.LinkReceipt;
import com.bank.platform.suitability.domain.JourneyModel.LinkRequest;
import com.bank.platform.suitability.domain.SuitabilityRefusal;
import java.time.Instant;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/** Calls consent-service for the section 9.10 link. The raw token is not part of the receipt. */
public final class HttpConsentLinkClient implements ConsentLinks {

  private final RestClient client;
  private final String key;

  public HttpConsentLinkClient(RestClient client, String key) {
    this.client = client;
    this.key = key;
  }

  @Override
  public boolean holdsQuoteShare(String customerId) {
    try {
      Granted body =
          client
              .get()
              .uri(
                  uri ->
                      uri.path("/internal/v1/consent-links/quote-share")
                          .queryParam("customerId", customerId)
                          .build())
              .header(InternalCallerAuth.HEADER, key)
              .retrieve()
              .body(Granted.class);
      return body != null && body.granted();
    } catch (RestClientResponseException ex) {
      throw mapped(ex);
    } catch (RestClientException ex) {
      throw unavailable();
    }
  }

  @Override
  public LinkReceipt issue(LinkRequest request) {
    try {
      Receipt body =
          client
              .post()
              .uri("/internal/v1/consent-links")
              .header(InternalCallerAuth.HEADER, key)
              .header("X-Actor-Id", request.actorId())
              .contentType(MediaType.APPLICATION_JSON)
              .body(
                  new IssueBody(
                      request.customerId(),
                      request.leadId(),
                      request.journeyId(),
                      request.assessmentId(),
                      request.mobile(),
                      request.email()))
              .retrieve()
              .body(Receipt.class);
      return receipt(body);
    } catch (RestClientResponseException ex) {
      throw mapped(ex);
    } catch (RestClientException ex) {
      throw unavailable();
    }
  }

  @Override
  public LinkReceipt resend(String assessmentId, String actorId) {
    try {
      Receipt body =
          client
              .post()
              .uri("/internal/v1/consent-links/resend")
              .header(InternalCallerAuth.HEADER, key)
              .header("X-Actor-Id", actorId)
              .contentType(MediaType.APPLICATION_JSON)
              .body(new ResendBody(assessmentId))
              .retrieve()
              .body(Receipt.class);
      return receipt(body);
    } catch (RestClientResponseException ex) {
      throw mapped(ex);
    } catch (RestClientException ex) {
      throw unavailable();
    }
  }

  @Override
  public String linkState(String assessmentId) {
    try {
      Receipt body =
          client
              .get()
              .uri(
                  uri ->
                      uri.path("/internal/v1/consent-links")
                          .queryParam("assessmentId", assessmentId)
                          .build())
              .header(InternalCallerAuth.HEADER, key)
              .retrieve()
              .body(Receipt.class);
      return body == null || body.state() == null ? "ABSENT" : body.state();
    } catch (RestClientResponseException ex) {
      if (ex.getStatusCode().value() == 404) {
        return "ABSENT";
      }
      throw mapped(ex);
    } catch (RestClientException ex) {
      throw unavailable();
    }
  }

  private static LinkReceipt receipt(Receipt body) {
    if (body == null) {
      throw unavailable();
    }
    return new LinkReceipt(
        body.state(), body.maskedMobile(), body.maskedEmail(), body.expiresAt(), body.sent());
  }

  private static SuitabilityRefusal mapped(RestClientResponseException ex) {
    if (ex.getStatusCode().value() == 429) {
      return new SuitabilityRefusal("RATE_LIMITED", null, null, BrdCopy.WAIT);
    }
    if (ex.getStatusCode().value() == 404) {
      return new SuitabilityRefusal("RESOURCE_NOT_FOUND", "consent link was not found");
    }
    return unavailable();
  }

  private static SuitabilityRefusal unavailable() {
    return new SuitabilityRefusal("UPSTREAM_UNAVAILABLE", "VAL-028", null, BrdCopy.VAL_028);
  }

  public record IssueBody(
      String customerId,
      String leadId,
      String journeyId,
      String assessmentId,
      String registeredMobile,
      String registeredEmail) {}

  public record ResendBody(String assessmentId) {}

  public record Granted(boolean granted) {}

  public record Receipt(
      String state, String maskedMobile, String maskedEmail, Instant expiresAt, boolean sent) {}
}
