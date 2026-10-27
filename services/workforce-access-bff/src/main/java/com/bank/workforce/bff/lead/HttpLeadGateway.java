package com.bank.workforce.bff.lead;

import com.bank.common.error.ErrorCodes;
import com.bank.common.error.ErrorPropagation;
import com.bank.common.error.PlatformLayer;
import com.bank.common.error.ProblemJsonReader;
import com.bank.common.error.ServiceErrors;
import com.bank.common.error.ServiceException;
import com.bank.workforce.bff.config.DownstreamProperties;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
@ConditionalOnProperty(name = "workforce.downstream.lead-mode", havingValue = "http")
public class HttpLeadGateway implements LeadGateway {

  private final RestClient client;
  private final ProblemJsonReader problemJsonReader;
  private final ServiceErrors serviceErrors;

  public HttpLeadGateway(
      @Qualifier("leadRestClient") RestClient restClient,
      DownstreamProperties properties,
      ProblemJsonReader problemJsonReader,
      ServiceErrors serviceErrors) {
    this.client =
        restClient
            .mutate()
            .baseUrl(properties.leadServiceBaseUrl().toString())
            .defaultHeader(
                DownstreamProperties.INTERNAL_SERVICE_KEY_HEADER,
                properties.leadServiceInternalKey())
            .build();
    this.problemJsonReader = problemJsonReader;
    this.serviceErrors = serviceErrors;
  }

  @Override
  public LeadRecord create(CreateLeadCommand command) {
    try {
      return client
          .post()
          .uri("/internal/v1/leads")
          .header("X-Actor-Id", command.actorId())
          .headers(
              h -> {
                if (command.idempotencyKey() != null) {
                  h.set("Idempotency-Key", command.idempotencyKey());
                }
              })
          .body(command)
          .retrieve()
          .body(LeadRecord.class);
    } catch (RestClientResponseException ex) {
      throw propagate(ex, "createLead");
    } catch (RestClientException ex) {
      throw unavailable(ex, "createLead");
    }
  }

  @Override
  public LeadRecord get(String leadId, String actorId) {
    try {
      return client
          .get()
          .uri("/internal/v1/leads/{leadId}", leadId)
          .header("X-Actor-Id", actorId)
          .retrieve()
          .body(LeadRecord.class);
    } catch (RestClientResponseException ex) {
      throw propagate(ex, "getLead");
    } catch (RestClientException ex) {
      throw unavailable(ex, "getLead");
    }
  }

  @Override
  public List<LeadRecord> list(String actorId) {
    try {
      LeadPage page =
          client
              .get()
              .uri("/internal/v1/leads?owner=me")
              .header("X-Actor-Id", actorId)
              .retrieve()
              .body(LeadPage.class);
      return page == null || page.items() == null ? List.of() : page.items();
    } catch (RestClientResponseException ex) {
      throw propagate(ex, "listLeads");
    } catch (RestClientException ex) {
      throw unavailable(ex, "listLeads");
    }
  }

  @Override
  public LeadRecord startOnboarding(String leadId, String actorId) {
    try {
      return client
          .post()
          .uri("/internal/v1/leads/{leadId}/onboarding", leadId)
          .header("X-Actor-Id", actorId)
          .retrieve()
          .body(LeadRecord.class);
    } catch (RestClientResponseException ex) {
      throw propagate(ex, "startOnboarding");
    } catch (RestClientException ex) {
      throw unavailable(ex, "startOnboarding");
    }
  }

  @Override
  public LeadRecord assign(AssignLeadCommand command) {
    try {
      return client
          .post()
          .uri("/internal/v1/leads/{leadId}/assignments", command.leadId())
          .header("X-Actor-Id", command.actorId())
          .body(command)
          .retrieve()
          .body(LeadRecord.class);
    } catch (RestClientResponseException ex) {
      throw propagate(ex, "assignLead");
    } catch (RestClientException ex) {
      throw unavailable(ex, "assignLead");
    }
  }

  private ServiceException propagate(RestClientResponseException ex, String operation) {
    return ErrorPropagation.from(
            problemJsonReader.read(ex.getResponseBodyAsString(), ex.getStatusCode().value()))
        .receivedBy(serviceErrors.serviceId(), PlatformLayer.L4)
        .calling("lead", operation)
        .causedBy(ex)
        .toException();
  }

  // No HTTP answer at all (refused, reset, timed out): a typed 503 the RM can retry, not a 500.
  private ServiceException unavailable(RestClientException ex, String operation) {
    return serviceErrors
        .error(ErrorCodes.UPSTREAM_UNAVAILABLE)
        .component("HttpLeadGateway")
        .operation(operation)
        .reason("Lead service is unavailable")
        .cause(ex)
        .build();
  }

  public record LeadPage(List<LeadRecord> items) {}
}
