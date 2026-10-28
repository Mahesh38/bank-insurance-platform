package com.bank.platform.integration.hub.config;

import com.bank.platform.integration.hub.domain.ProviderBreakerRegistry;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(HubBreakerProperties.class)
public class HubResilienceConfig {

  @Bean
  ProviderBreakerRegistry providerBreakerRegistry(HubBreakerProperties properties) {
    return new ProviderBreakerRegistry(
        properties.enabled(),
        properties.failureThreshold(),
        properties.openWaitMs(),
        properties.halfOpenSuccessThreshold());
  }
}
