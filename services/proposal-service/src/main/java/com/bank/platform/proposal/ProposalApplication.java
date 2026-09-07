package com.bank.platform.proposal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Skeleton entry point for bounded context #11 — Proposal & UW-Tracking.
 *
 * <p>Business logic is intentionally absent. This module exists so engineers, CI and GitLab group
 * policies can align to the target microservices topology before feature work begins.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class ProposalApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProposalApplication.class, args);
    }
}
