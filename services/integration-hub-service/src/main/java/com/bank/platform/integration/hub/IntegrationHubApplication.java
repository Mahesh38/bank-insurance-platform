package com.bank.platform.integration.hub;

import com.bank.platform.integration.hub.config.InternalAuthProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Bounded context #14 — Integration Hub. FUNC-032 delivers QUOTE submit/poll to {@code
 * 1sb-integration-service}; proposal/query operations follow.
 */
@SpringBootApplication
@EnableConfigurationProperties(InternalAuthProperties.class)
@ConfigurationPropertiesScan
public class IntegrationHubApplication {

  public static void main(String[] args) {
    SpringApplication.run(IntegrationHubApplication.class, args);
  }
}
