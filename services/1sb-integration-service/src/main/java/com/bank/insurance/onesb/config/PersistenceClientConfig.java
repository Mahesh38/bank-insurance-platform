package com.bank.insurance.onesb.config;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(PersistenceClientProperties.class)
public class PersistenceClientConfig {

  @Bean
  RestClient persistenceRestClient(PersistenceClientProperties properties) {
    // JDK HttpClient supports PATCH (HttpURLConnection does not).
    HttpClient httpClient =
        HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
    requestFactory.setReadTimeout(Duration.ofSeconds(30));
    return RestClient.builder()
        .baseUrl(properties.baseUrl())
        .defaultHeader(
            PersistenceClientProperties.INTERNAL_KEY_HEADER, properties.internalServiceKey())
        .requestFactory(requestFactory)
        .build();
  }
}
