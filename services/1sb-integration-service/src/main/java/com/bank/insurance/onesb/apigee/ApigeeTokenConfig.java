package com.bank.insurance.onesb.apigee;

import com.bank.common.apigee.ApigeeAccessTokenHolder;
import com.bank.common.apigee.ApigeeTokenClient;
import com.bank.common.apigee.IssuedToken;
import java.time.Clock;
import java.time.Duration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AU-Bima-Platform client-credentials holder for 1sb-integration-service. Lives outside {@code
 * adapter.onesb} because an Apigee token is not a 1SB type (INV-ACL-01, {@code SUG-20261006-atk}).
 *
 * <p>{@code token-mode=http} is refused until Bearer attachment exists ({@code DEP-20260914-apg}).
 */
@Configuration
@EnableConfigurationProperties(ApigeeTokenProperties.class)
public class ApigeeTokenConfig {

  public ApigeeTokenConfig(ApigeeTokenProperties properties) {
    rejectHttpModeUntilMapping(properties.tokenMode());
  }

  @Bean
  ApigeeAccessTokenHolder apigeeAccessTokenHolder(ApigeeTokenClient client, Clock clock) {
    return new ApigeeAccessTokenHolder(client, clock);
  }

  @Bean
  @ConditionalOnProperty(name = "apigee.token-mode", havingValue = "stub", matchIfMissing = true)
  ApigeeTokenClient stubApigeeTokenClient(Clock clock) {
    return () -> new IssuedToken("stub-apigee-token", clock.instant().plus(Duration.ofHours(24)));
  }

  static void rejectHttpModeUntilMapping(String tokenMode) {
    if (tokenMode != null && "http".equalsIgnoreCase(tokenMode.strip())) {
      throw new IllegalStateException(
          "ONESB_APIGEE_TOKEN_MODE=http is refused until Bearer attachment exists (DEP-20260914-apg)");
    }
  }
}
