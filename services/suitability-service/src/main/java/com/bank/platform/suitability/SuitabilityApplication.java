package com.bank.platform.suitability;

import com.bank.platform.suitability.config.InternalAuthProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Skeleton entry point for bounded context #7 — Suitability & Recommendation.
 *
 * <p>Business logic is intentionally absent. This module exists so engineers, CI and GitLab group
 * policies can align to the target microservices topology before feature work begins.
 */
@SpringBootApplication
@EnableConfigurationProperties(InternalAuthProperties.class)
@ConfigurationPropertiesScan
public class SuitabilityApplication {

  public static void main(String[] args) {
    SpringApplication.run(SuitabilityApplication.class, args);
  }
}
