package com.bank.workforce.bff.config;

import java.net.http.HttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * One {@link RestClient} per downstream, each with its seam budget ({@link DownstreamTimeouts}).
 */
@Configuration
public class BffClientConfig {

  @Bean
  RestClient leadRestClient(RestClient.Builder builder, DownstreamTimeouts timeouts) {
    return restClient(builder, timeouts.lead());
  }

  @Bean
  RestClient customerRestClient(RestClient.Builder builder, DownstreamTimeouts timeouts) {
    return restClient(builder, timeouts.customer());
  }

  @Bean
  RestClient authorizationRestClient(RestClient.Builder builder, DownstreamTimeouts timeouts) {
    return restClient(builder, timeouts.authorization());
  }

  @Bean
  RestClient identityProviderRestClient(RestClient.Builder builder, DownstreamTimeouts timeouts) {
    return restClient(builder, timeouts.identityProvider());
  }

  public static RestClient restClient(
      RestClient.Builder builder, DownstreamTimeouts.Budget budget) {
    HttpClient httpClient =
        HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(budget.connect())
            .build();
    JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
    factory.setReadTimeout(budget.read());
    return builder.clone().requestFactory(factory).build();
  }
}
