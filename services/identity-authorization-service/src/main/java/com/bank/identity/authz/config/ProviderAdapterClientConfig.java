package com.bank.identity.authz.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class ProviderAdapterClientConfig {

    @Bean
    RestClient authorizationRestClient(RestClient.Builder builder) {
        return builder.build();
    }
}
