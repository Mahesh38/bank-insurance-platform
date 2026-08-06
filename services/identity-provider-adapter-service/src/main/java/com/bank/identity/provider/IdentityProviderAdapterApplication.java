package com.bank.identity.provider;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class IdentityProviderAdapterApplication {

    public static void main(String[] args) {
        SpringApplication.run(IdentityProviderAdapterApplication.class, args);
    }
}
