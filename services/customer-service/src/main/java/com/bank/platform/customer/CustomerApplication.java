package com.bank.platform.customer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Bounded context #4 — Customer. ETB search via Apigee token holder ({@code EPIC-006}).
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class CustomerApplication {

    public static void main(String[] args) {
        SpringApplication.run(CustomerApplication.class, args);
    }
}
