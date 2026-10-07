package com.bank.platform.customer.adapter.cbs;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
                "http", wireMock.baseUrl(), "/customers/search", "/customers/{customerId}"));
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
}
