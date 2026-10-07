package com.bank.platform.customer.adapter.cbs;

import com.bank.platform.customer.domain.CustomerInquiryPort;
import com.bank.platform.customer.domain.CustomerInquiryUnauthorizedException;
import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Live CBS inquiry via Apigee. The JSON shape is this adapter's contract until the bank publishes
 * the CBS OpenAPI ({@code DEP-20260914-apg}). Credentials and host stay in environment variables.
 */
@Component
@ConditionalOnProperty(name = "customer.cbs.inquiry-mode", havingValue = "http")
public class HttpCustomerDirectory implements CustomerInquiryPort {

  private final RestClient client;
  private final CbsInquiryProperties properties;

  public HttpCustomerDirectory(RestClient restClient, CbsInquiryProperties properties) {
    this.client = restClient.mutate().baseUrl(properties.baseUri().toString()).build();
    this.properties = properties;
  }

  @Override
  public List<CustomerHit> search(SearchQuery query, String accessToken) {
    String path =
        UriComponentsBuilder.fromPath(properties.searchPath())
            .queryParam("by", query.by())
            .queryParam("q", query.value())
            .queryParam("countryCode", query.countryCode())
            .build(true)
            .toUriString();
    SearchPage page = get(path, accessToken, SearchPage.class);
    return page == null || page.items() == null ? List.of() : page.items();
  }

  @Override
  public Optional<CustomerHit> findById(String customerId, String accessToken) {
    try {
      CustomerHit hit =
          get(
              properties.getPath().replace("{customerId}", customerId),
              accessToken,
              CustomerHit.class);
      return Optional.ofNullable(hit);
    } catch (RestClientResponseException ex) {
      if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
        return Optional.empty();
      }
      throw translate(ex);
    }
  }

  private <T> T get(String path, String accessToken, Class<T> type) {
    try {
      return client
          .get()
          .uri(path)
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
          .retrieve()
          .body(type);
    } catch (RestClientResponseException ex) {
      throw translate(ex);
    }
  }

  private RuntimeException translate(RestClientResponseException ex) {
    if (ex.getStatusCode() == HttpStatus.UNAUTHORIZED) {
      return new CustomerInquiryUnauthorizedException();
    }
    return ex;
  }

  public record SearchPage(List<CustomerHit> items) {}
}
