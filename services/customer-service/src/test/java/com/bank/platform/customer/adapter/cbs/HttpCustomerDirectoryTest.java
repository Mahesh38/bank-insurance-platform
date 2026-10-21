package com.bank.platform.customer.adapter.cbs;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bank.common.error.ErrorCodes;
import com.bank.common.error.PlatformLayer;
import com.bank.common.error.ServiceErrors;
import com.bank.common.error.ServiceException;
import com.bank.platform.customer.domain.CustomerInquiryPort.CustomerHit;
import com.bank.platform.customer.domain.CustomerInquiryPort.SearchQuery;
import com.bank.platform.customer.domain.CustomerInquiryUnauthorizedException;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestClient;

@Tag("unit")
@Tag("FUNC-031")
class HttpCustomerDirectoryTest {

  @RegisterExtension
  static WireMockExtension wireMock =
      WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

  private HttpCustomerDirectory directory;

  @BeforeEach
  void setUp() {
    HttpClient httpClient =
        HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(2))
            .build();
    JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
    factory.setReadTimeout(Duration.ofSeconds(2));
    RestClient restClient =
        RestClient.builder()
            .requestFactory(factory)
            .messageConverters(
                converters -> converters.add(new MappingJackson2HttpMessageConverter()))
            .build();
    directory =
        new HttpCustomerDirectory(
            restClient,
            new CbsInquiryProperties(
                "http", wireMock.baseUrl(), "/customers/search", "/customers/{customerId}"),
            ServiceErrors.of("customer", PlatformLayer.L5));
  }

  @Test
  void searchSendsBearerAndMapsMaskedHits() {
    wireMock.stubFor(
        get(urlPathEqualTo("/customers/search"))
            .withHeader("Authorization", equalTo("Bearer live-token"))
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        "{\"items\":[{\"customerId\":\"CUST-3210\",\"displayName\":\"A U Customer\",\"initials\":\"AU\",\"maskedCif\":\"XXXXX3210\",\"maskedMobile\":\"XXXXXX3210\",\"eligibility\":\"ETB\",\"source\":\"CBS\"}]}")));

    List<CustomerHit> hits =
        directory.search(new SearchQuery("MOBILE", "9876543210", "IN"), "live-token");

    assertThat(hits).hasSize(1);
    assertThat(hits.get(0).maskedMobile()).isEqualTo("XXXXXX3210");
    assertThat(hits.get(0).source()).isEqualTo("CBS");
  }

  @Test
  void unauthorizedBecomesInquiryUnauthorized() {
    wireMock.stubFor(
        get(urlPathEqualTo("/customers/search")).willReturn(aResponse().withStatus(401)));

    assertThatThrownBy(
            () -> directory.search(new SearchQuery("MOBILE", "9876543210", "IN"), "stale"))
        .isInstanceOf(CustomerInquiryUnauthorizedException.class);
  }

  @Test
  void serverErrorIsUpstreamUnavailableWithoutQueryInMessage() {
    wireMock.stubFor(
        get(urlPathEqualTo("/customers/search")).willReturn(aResponse().withStatus(503)));
    String mobile = "9876543210";

    assertThatThrownBy(() -> directory.search(new SearchQuery("MOBILE", mobile, "IN"), "token"))
        .isInstanceOf(ServiceException.class)
        .satisfies(
            thrown -> {
              ServiceException ex = (ServiceException) thrown;
              assertThat(ex.getErrorResponse().getCode())
                  .isEqualTo(ErrorCodes.UPSTREAM_UNAVAILABLE);
              assertThat(ex.getMessage()).doesNotContain(mobile);
              assertThat(ex.getDiagnostic().getReason()).doesNotContain(mobile);
              assertThat(ex.getDiagnostic().getReason()).contains("HTTP 503");
              assertThat(ex.getCause()).isNull();
            });
  }

  @Test
  void clientErrorIsUpstreamBadResponseWithoutQueryInMessage() {
    wireMock.stubFor(
        get(urlPathEqualTo("/customers/search")).willReturn(aResponse().withStatus(400)));
    String pan = "ABCDE1234F";

    assertThatThrownBy(() -> directory.search(new SearchQuery("PAN", pan, "IN"), "token"))
        .isInstanceOf(ServiceException.class)
        .satisfies(
            thrown -> {
              ServiceException ex = (ServiceException) thrown;
              assertThat(ex.getErrorResponse().getCode())
                  .isEqualTo(ErrorCodes.UPSTREAM_BAD_RESPONSE);
              assertThat(ex.getMessage()).doesNotContain(pan);
              assertThat(ex.getDiagnostic().getReason()).doesNotContain(pan);
            });
  }

  @Test
  void findByIdEncodesPathSegments() {
    wireMock.stubFor(
        get(urlEqualTo("/customers/CUST%2F3210"))
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        "{\"customerId\":\"CUST/3210\",\"displayName\":\"A U Customer\",\"initials\":\"AU\",\"maskedCif\":\"XXXXX3210\",\"maskedMobile\":\"XXXXXX3210\",\"eligibility\":\"ETB\",\"source\":\"CBS\"}")));

    assertThat(directory.findById("CUST/3210", "live-token")).isPresent();
    wireMock.verify(getRequestedFor(urlEqualTo("/customers/CUST%2F3210")));
  }

  @Test
  void findByIdTreats404AsAbsent() {
    wireMock.stubFor(
        get(urlPathEqualTo("/customers/MISSING")).willReturn(aResponse().withStatus(404)));

    assertThat(directory.findById("MISSING", "live-token")).isEmpty();
  }
}
