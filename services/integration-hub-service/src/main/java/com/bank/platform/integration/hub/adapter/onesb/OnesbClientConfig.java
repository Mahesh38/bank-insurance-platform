package com.bank.platform.integration.hub.adapter.onesb;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(OnesbClientProperties.class)
public class OnesbClientConfig {

  @Bean
  RestClient onesbSubmitRestClient(OnesbClientProperties properties) {
    return timedClient(properties, properties.submitReadTimeoutMs());
  }

  @Bean
  RestClient onesbPollRestClient(OnesbClientProperties properties) {
    return timedClient(properties, properties.pollReadTimeoutMs());
  }

  private static RestClient timedClient(OnesbClientProperties properties, int readTimeoutMs) {
    HttpClient httpClient =
        HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofMillis(properties.connectTimeoutMs()))
            .build();
    JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
    factory.setReadTimeout(Duration.ofMillis(readTimeoutMs));
    return RestClient.builder().baseUrl(properties.baseUrl()).requestFactory(factory).build();
  }
}
