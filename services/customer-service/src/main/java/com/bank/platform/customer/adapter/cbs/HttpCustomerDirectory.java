package com.bank.platform.customer.adapter.cbs;

import com.bank.common.error.ErrorCodes;
import com.bank.common.error.ServiceErrors;
import com.bank.common.error.ServiceException;
import com.bank.platform.customer.domain.CustomerInquiryPort;
import com.bank.platform.customer.domain.CustomerInquiryUnauthorizedException;
import java.net.ConnectException;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.http.HttpConnectTimeoutException;
import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Live CBS inquiry via Apigee. The JSON shape is this adapter's contract until the bank publishes
 * the CBS OpenAPI ({@code DEP-20260914-apg}). Credentials and host stay in environment variables.
 * Failures become catalogue {@code UPSTREAM_*} codes — the request URI (and {@code q}) never enter
 * the exception message (SEC-C2).
 *
 * <p>Seam S-05 (conventions/idempotency-timeouts-retries.md): connect 3 s / read 3 s on the shared
 * client, and exactly one retry on a 5xx or a connect failure. A read timeout is not retried — the
 * request may have reached CBS and the BFF budget (4 s) has no room for a second full wait. Every
 * transport failure is {@code 503 UPSTREAM_UNAVAILABLE}, never a bare 500.
 */
@Component
@ConditionalOnProperty(name = "customer.cbs.inquiry-mode", havingValue = "http")
public class HttpCustomerDirectory implements CustomerInquiryPort {

  private final RestClient client;
  private final CbsInquiryProperties properties;
  private final ServiceErrors errors;

  public HttpCustomerDirectory(
      RestClient restClient, CbsInquiryProperties properties, ServiceErrors errors) {
    this.client = restClient.mutate().baseUrl(properties.baseUri().toString()).build();
    this.properties = properties;
    this.errors = errors;
  }

  @Override
  public List<CustomerHit> search(SearchQuery query, String accessToken) {
    URI uri =
        UriComponentsBuilder.fromPath(properties.searchPath())
            .queryParam("by", query.by())
            .queryParam("q", query.value())
            .queryParam("countryCode", query.countryCode())
            .build(true)
            .toUri();
    SearchPage page;
    try {
      page = get(uri, accessToken, SearchPage.class);
    } catch (ServiceException ex) {
      // A search with no match is an empty page, not an absent resource (AC-CUST-010-2).
      if (ErrorCodes.RESOURCE_NOT_FOUND.equals(ex.getErrorResponse().getCode())) {
        return List.of();
      }
      throw ex;
    }
    return page == null || page.items() == null ? List.of() : page.items();
  }

  @Override
  public Optional<CustomerHit> findById(String customerId, String accessToken) {
    URI uri =
        UriComponentsBuilder.fromPath(properties.getPath())
            .encode()
            .buildAndExpand(customerId)
            .toUri();
    try {
      return Optional.ofNullable(get(uri, accessToken, CustomerHit.class));
    } catch (ServiceException ex) {
      if (ErrorCodes.RESOURCE_NOT_FOUND.equals(ex.getErrorResponse().getCode())) {
        return Optional.empty();
      }
      throw ex;
    }
  }

  static final int MAX_ATTEMPTS = 2;

  private <T> T get(URI uri, String accessToken, Class<T> type) {
    for (int attempt = 1; ; attempt++) {
      try {
        return client
            .get()
            .uri(uri)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .retrieve()
            .body(type);
      } catch (RestClientResponseException ex) {
        if (attempt < MAX_ATTEMPTS && ex.getStatusCode().is5xxServerError()) {
          continue;
        }
        throw translate(ex);
      } catch (ResourceAccessException ex) {
        if (attempt < MAX_ATTEMPTS && isConnectFailure(ex)) {
          continue;
        }
        throw transportFailure(ex);
      } catch (RestClientException ex) {
        throw transportFailure(ex);
      }
    }
  }

  private static boolean isConnectFailure(ResourceAccessException ex) {
    for (Throwable t = ex.getCause(); t != null; t = t.getCause()) {
      if (t instanceof ConnectException
          || t instanceof HttpConnectTimeoutException
          || t instanceof UnknownHostException) {
        return true;
      }
    }
    return false;
  }

  // No cause attached: ResourceAccessException's message carries the full URI, q included (SEC-C2).
  private RuntimeException transportFailure(RestClientException ex) {
    return errors
        .error(ErrorCodes.UPSTREAM_UNAVAILABLE)
        .component("HttpCustomerDirectory")
        .operation("inquiry")
        .reason("CBS inquiry transport failure: " + ex.getClass().getSimpleName())
        .build();
  }

  private RuntimeException translate(RestClientResponseException ex) {
    HttpStatusCode status = ex.getStatusCode();
    if (status == HttpStatus.UNAUTHORIZED) {
      return new CustomerInquiryUnauthorizedException();
    }
    if (status == HttpStatus.NOT_FOUND) {
      return errors
          .error(ErrorCodes.RESOURCE_NOT_FOUND)
          .component("HttpCustomerDirectory")
          .operation("inquiry")
          .reason("CBS inquiry returned HTTP 404")
          .build();
    }
    String code =
        status.is5xxServerError()
            ? ErrorCodes.UPSTREAM_UNAVAILABLE
            : ErrorCodes.UPSTREAM_BAD_RESPONSE;
    return errors
        .error(code)
        .component("HttpCustomerDirectory")
        .operation("inquiry")
        .reason("CBS inquiry returned HTTP " + status.value())
        .build();
  }

  public record SearchPage(List<CustomerHit> items) {}
}
