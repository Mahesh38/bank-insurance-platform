package com.bank.platform.journey;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Skeleton entry point for bounded context #9 — Journey Orchestration.
 *
 * <p>Business logic is intentionally absent. This module exists so engineers, CI and GitLab group
 * policies can align to the target microservices topology before feature work begins.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class JourneyOrchestrationApplication {

    public static void main(String[] args) {
        SpringApplication.run(JourneyOrchestrationApplication.class, args);
    }
}
