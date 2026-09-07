package com.bank.platform.reporting;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Skeleton entry point for bounded context #18 — Reporting & MIS.
 *
 * <p>Business logic is intentionally absent. This module exists so engineers, CI and GitLab group
 * policies can align to the target microservices topology before feature work begins.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class ReportingMisApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReportingMisApplication.class, args);
    }
}
