package com.bank.workforce.bff.customer;

import com.bank.common.error.ErrorPropagation;
import com.bank.common.error.PlatformLayer;
import com.bank.common.error.ProblemJsonReader;
import com.bank.common.error.ServiceErrors;
import com.bank.workforce.bff.config.DownstreamProperties;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@ConditionalOnProperty(name = "workforce.downstream.customer-mode", havingValue = "http")
public class HttpCustomerGateway implements CustomerGateway {

  private final RestClient client;
  private final ProblemJsonReader problemJsonReader;
  private final ServiceErrors serviceErrors;

  public HttpCustomerGateway(
      RestClient restClient,
      DownstreamProperties properties,
      ProblemJsonReader problemJsonReader,
      ServiceErrors serviceErrors) {
    this.client =
        restClient.mutate().baseUrl(properties.customerServiceBaseUrl().toString()).build();
    this.problemJsonReader = problemJsonReader;
    this.serviceErrors = serviceErrors;
  }

  @Override
  public List<CustomerHit> search(String by, String query, String countryCode) {
    try {
      String uri =
          UriComponentsBuilder.fromPath("/internal/v1/customers/search")
              .queryParam("by", by)
              .queryParam("q", query)
              .queryParam("countryCode", countryCode)
              .build(true)
              .toUriString();
      SearchPage page = client.get().uri(uri).retrieve().body(SearchPage.class);
      return page == null || page.items() == null ? List.of() : page.items();
    } catch (RestClientResponseException ex) {
      throw ErrorPropagation.from(
              problemJsonReader.read(ex.getResponseBodyAsString(), ex.getStatusCode().value()))
          .receivedBy(serviceErrors.serviceId(), PlatformLayer.L4)
          .calling("customer", "search")
          .causedBy(ex)
          .toException();
    }
  }

  @Override
  public CustomerHit get(String customerId) {
    try {
      return client
          .get()
          .uri("/internal/v1/customers/{customerId}", customerId)
          .retrieve()
          .body(CustomerHit.class);
    } catch (RestClientResponseException ex) {
      throw ErrorPropagation.from(
              problemJsonReader.read(ex.getResponseBodyAsString(), ex.getStatusCode().value()))
          .receivedBy(serviceErrors.serviceId(), PlatformLayer.L4)
          .calling("customer", "get")
          .causedBy(ex)
          .toException();
    }
  }

  public record SearchPage(List<CustomerHit> items) {}
}
