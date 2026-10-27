package com.bank.platform.audit;

import com.bank.platform.audit.config.InternalAuthProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Skeleton entry point for bounded context #16 — Audit & Compliance.
 *
 * <p>Business logic is intentionally absent. This module exists so engineers, CI and GitLab group
 * policies can align to the target microservices topology before feature work begins.
 */
@SpringBootApplication
@EnableConfigurationProperties(InternalAuthProperties.class)
@ConfigurationPropertiesScan
public class AuditComplianceApplication {

  public static void main(String[] args) {
    SpringApplication.run(AuditComplianceApplication.class, args);
  }
}
