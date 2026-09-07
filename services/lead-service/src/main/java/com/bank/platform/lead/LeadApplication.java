package com.bank.platform.lead;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Skeleton entry point for bounded context #5 — Lead.
 *
 * <p>Business logic is intentionally absent. This module exists so engineers, CI and GitLab group
 * policies can align to the target microservices topology before feature work begins.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class LeadApplication {

    public static void main(String[] args) {
        SpringApplication.run(LeadApplication.class, args);
    }
}
