package com.bank.insurance.onesb.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(PersistenceClientProperties.class)
public class PersistenceClientConfig {

    @Bean
    RestClient persistenceRestClient(PersistenceClientProperties properties, RestClient.Builder builder) {
        return builder.baseUrl(properties.baseUrl()).build();
    }
}
