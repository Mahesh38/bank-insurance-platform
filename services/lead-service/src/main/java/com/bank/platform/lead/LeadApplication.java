package com.bank.platform.lead;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Bounded context #5 — Lead. R0 create / onboard / assign / inbox slice ({@code EPIC-006}).
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class LeadApplication {

    public static void main(String[] args) {
        SpringApplication.run(LeadApplication.class, args);
    }
}
