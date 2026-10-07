package com.bank.platform.customer.adapter.apigee;

import com.bank.common.apigee.ApigeeAccessTokenHolder;
import com.bank.common.apigee.ApigeeTokenClient;
import com.bank.platform.customer.domain.AccessTokenPort;
import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(ApigeeClientProperties.class)
public class ApigeeClientConfig {

  @Bean
  Clock utcClock() {
    return Clock.systemUTC();
  }

  @Bean
  RestClient customerRestClient(RestClient.Builder builder) {
    return builder.build();
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
