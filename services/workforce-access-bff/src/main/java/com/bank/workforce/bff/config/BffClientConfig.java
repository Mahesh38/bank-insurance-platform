package com.bank.workforce.bff.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class BffClientConfig {

    @Bean
    RestClient bffRestClient(RestClient.Builder builder) {
        return builder.build();
    }
}
