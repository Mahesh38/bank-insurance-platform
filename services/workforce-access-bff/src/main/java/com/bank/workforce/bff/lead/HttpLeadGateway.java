package com.bank.workforce.bff.lead;

import com.bank.common.error.ErrorPropagation;
import com.bank.common.error.PlatformLayer;
import com.bank.common.error.ProblemJsonReader;
import com.bank.common.error.ServiceErrors;
import com.bank.common.error.ServiceException;
import com.bank.workforce.bff.config.DownstreamProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;

@Component
@ConditionalOnProperty(name = "workforce.downstream.lead-mode", havingValue = "http")
public class HttpLeadGateway implements LeadGateway {

    private final RestClient client;
    private final ProblemJsonReader problemJsonReader;
    private final ServiceErrors serviceErrors;

    public HttpLeadGateway(
            RestClient restClient,
            DownstreamProperties properties,
            ProblemJsonReader problemJsonReader,
            ServiceErrors serviceErrors) {
        this.client = restClient.mutate().baseUrl(properties.leadServiceBaseUrl().toString()).build();
        this.problemJsonReader = problemJsonReader;
        this.serviceErrors = serviceErrors;
    }

    @Override
    public LeadRecord create(CreateLeadCommand command) {
        try {
            return client.post()
                .uri("/internal/v1/leads")
                .header("X-Actor-Id", command.actorId())
                .body(command)
                .retrieve()
                .body(LeadRecord.class);
        } catch (RestClientResponseException ex) {
            throw propagate(ex, "createLead");
        }
    }

    @Override
    public LeadRecord get(String leadId, String actorId) {
        try {
            return client.get()
                .uri("/internal/v1/leads/{leadId}", leadId)
                .header("X-Actor-Id", actorId)
                .retrieve()
                .body(LeadRecord.class);
        } catch (RestClientResponseException ex) {
            throw propagate(ex, "getLead");
        }
    }

    @Override
    public List<LeadRecord> list(String actorId) {
        try {
            LeadPage page = client.get()
                .uri("/internal/v1/leads?owner=me")
                .header("X-Actor-Id", actorId)
                .retrieve()
                .body(LeadPage.class);
            return page == null || page.items() == null ? List.of() : page.items();
        } catch (RestClientResponseException ex) {
            throw propagate(ex, "listLeads");
        }
    }

    @Override
    public LeadRecord startOnboarding(String leadId, String actorId) {
        try {
            return client.post()
                .uri("/internal/v1/leads/{leadId}/onboarding", leadId)
                .header("X-Actor-Id", actorId)
                .retrieve()
                .body(LeadRecord.class);
        } catch (RestClientResponseException ex) {
            throw propagate(ex, "startOnboarding");
        }
    }

    @Override
    public LeadRecord assign(AssignLeadCommand command) {
        try {
            return client.post()
                .uri("/internal/v1/leads/{leadId}/assignments", command.leadId())
                .header("X-Actor-Id", command.actorId())
                .body(command)
                .retrieve()
                .body(LeadRecord.class);
        } catch (RestClientResponseException ex) {
            throw propagate(ex, "assignLead");
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

    public record LeadPage(List<LeadRecord> items) {}
}
