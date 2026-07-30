package com.bank.insurance.onesb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class OneSbIntegrationApplication {

    public static void main(String[] args) {
        SpringApplication.run(OneSbIntegrationApplication.class, args);
    }
}
