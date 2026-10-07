package com.bank.insurance.onesb.apigee;

import com.bank.common.apigee.ApigeeAccessTokenHolder;
import com.bank.common.apigee.ApigeeTokenClient;
import com.bank.common.apigee.HttpApigeeTokenClient;
import com.bank.common.apigee.IssuedToken;
import java.time.Clock;
import java.time.Duration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * AU-Bima-Platform client-credentials mint for 1sb-integration-service. Lives outside {@code
 * adapter.onesb} because an Apigee token is not a 1SB type (INV-ACL-01, {@code SUG-20261006-atk}).
 */
@Configuration
@EnableConfigurationProperties(ApigeeTokenProperties.class)
public class ApigeeTokenConfig {

  @Bean
  ApigeeAccessTokenHolder apigeeAccessTokenHolder(ApigeeTokenClient client, Clock clock) {
    return new ApigeeAccessTokenHolder(client, clock);
  }

  @Bean
  @ConditionalOnProperty(name = "apigee.token-mode", havingValue = "stub", matchIfMissing = true)
  ApigeeTokenClient stubApigeeTokenClient(Clock clock) {
    return () -> new IssuedToken("stub-apigee-token", clock.instant().plus(Duration.ofHours(24)));
  }

  @Bean
  @ConditionalOnProperty(name = "apigee.token-mode", havingValue = "http")
  ApigeeTokenClient httpApigeeTokenClient(
      RestClient.Builder builder, ApigeeTokenProperties properties, Clock clock) {
    return new HttpApigeeTokenClient(
        builder.build(),
        properties.tokenUri(),
        properties.clientId(),
        properties.clientSecret(),
        clock);
  }
}
