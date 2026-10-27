package com.bank.platform.integration.direct;

import com.bank.platform.integration.direct.config.InternalAuthProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Skeleton entry point for bounded context #adapter-direct — Direct Insurer Adapter.
 *
 * <p>Business logic is intentionally absent. This module exists so engineers, CI and GitLab group
 * policies can align to the target microservices topology before feature work begins.
 */
@SpringBootApplication
@EnableConfigurationProperties(InternalAuthProperties.class)
@ConfigurationPropertiesScan
public class DirectInsurerAdapterApplication {

  public static void main(String[] args) {
    SpringApplication.run(DirectInsurerAdapterApplication.class, args);
  }
}
