package com.bank.platform.quotation.config;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties({QuotationClientProperties.class, QuotationGatesProperties.class})
public class QuotationClientsConfig {

  @Bean
  @Qualifier("hubSubmitRestClient")
  RestClient hubSubmitRestClient(QuotationClientProperties properties) {
    return timedClient(
        properties.hub().baseUrl(),
        properties.hub().connectTimeoutMs(),
        properties.hub().submitReadTimeoutMs());
  }

  @Bean
  @Qualifier("hubPollRestClient")
  RestClient hubPollRestClient(QuotationClientProperties properties) {
    return timedClient(
        properties.hub().baseUrl(),
        properties.hub().connectTimeoutMs(),
        properties.hub().pollReadTimeoutMs());
  }

  @Bean
  @Qualifier("suitabilityRestClient")
  @ConditionalOnProperty(
      name = "quotation.gates.stub",
      havingValue = "false",
      matchIfMissing = true)
  RestClient suitabilityRestClient(QuotationClientProperties properties) {
    return timedClient(
        properties.suitability().baseUrl(),
        properties.suitability().connectTimeoutMs(),
        properties.suitability().readTimeoutMs());
  }

  @Bean
  @Qualifier("consentRestClient")
  @ConditionalOnProperty(
      name = "quotation.gates.stub",
      havingValue = "false",
      matchIfMissing = true)
  RestClient consentRestClient(QuotationClientProperties properties) {
    return timedClient(
        properties.consent().baseUrl(),
        properties.consent().connectTimeoutMs(),
        properties.consent().readTimeoutMs());
  }

  private static RestClient timedClient(String baseUrl, int connectTimeoutMs, int readTimeoutMs) {
    String url = baseUrl == null || baseUrl.isBlank() ? "http://localhost" : baseUrl;
    HttpClient httpClient =
        HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofMillis(connectTimeoutMs))
            .build();
    JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
    factory.setReadTimeout(Duration.ofMillis(readTimeoutMs));
    return RestClient.builder().baseUrl(url).requestFactory(factory).build();
  }
}
