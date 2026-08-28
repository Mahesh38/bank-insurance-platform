package com.bank.insurance.onesb.adapter.onesb.config;

import com.bank.common.secrets.SecretProvider;
import com.bank.insurance.onesb.adapter.onesb.error.OneSbErrorNormaliser;
import com.bank.common.error.ServiceErrors;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * Wires the 1SB {@link RestClient} with Basic Auth and connect/read timeouts.
 */
@Configuration
@EnableConfigurationProperties(OneSbClientProperties.class)
public class OneSbClientConfig {

    @Bean
    OneSbErrorNormaliser oneSbErrorNormaliser(ObjectMapper objectMapper, ServiceErrors serviceErrors) {
        return new OneSbErrorNormaliser(objectMapper, serviceErrors);
    }

    @Bean
    RestClient oneSbRestClient(OneSbClientProperties properties, SecretProvider secretProvider) {
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofMillis(properties.connectTimeoutMs()))
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofMillis(properties.readTimeoutMs()));

        return RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory)
                .defaultHeaders(headers -> headers.setBasicAuth(
                        secretProvider.getApiKey(),
                        secretProvider.getApiSecret()))
                .build();
    }
}
