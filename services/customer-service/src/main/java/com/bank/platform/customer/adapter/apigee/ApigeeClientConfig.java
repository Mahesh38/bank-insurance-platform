package com.bank.platform.customer.adapter.apigee;

import com.bank.common.apigee.ApigeeAccessTokenHolder;
import com.bank.common.apigee.ApigeeTokenClient;
import com.bank.platform.customer.domain.AccessTokenPort;
import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(ApigeeClientProperties.class)
public class ApigeeClientConfig {

  @Bean
  Clock utcClock() {
    return Clock.systemUTC();
  }

  @Bean
  RestClient customerRestClient(RestClient.Builder builder, ApigeeClientProperties properties) {
    return builder
        .requestFactory(timedFactory(properties.connectTimeoutMs(), properties.readTimeoutMs()))
        .build();
  }

  static JdkClientHttpRequestFactory timedFactory(int connectTimeoutMs, int readTimeoutMs) {
    HttpClient httpClient =
        HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofMillis(connectTimeoutMs))
            .build();
    JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
    factory.setReadTimeout(Duration.ofMillis(readTimeoutMs));
    return factory;
  }

  @Bean
  AccessTokenPort accessTokenPort(ApigeeTokenClient client, Clock clock) {
    ApigeeAccessTokenHolder holder = new ApigeeAccessTokenHolder(client, clock);
    return new AccessTokenPort() {
      @Override
      public String currentAccessToken() {
        return holder.currentAccessToken();
      }

      @Override
      public void invalidate() {
        holder.invalidate();
      }
    };
  }
}
