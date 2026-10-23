package com.bank.identity.provider.config;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;

@Configuration
public class ProviderClientConfig {

  /** Keycloak token / user-info / admin calls, bounded by {@code identity.provider.keycloak.*}. */
  @Bean
  @Primary
  RestClient providerRestClient(RestClient.Builder builder, KeycloakProperties properties) {
    return builder
        .clone()
        .requestFactory(requestFactory(properties.connectTimeout(), properties.readTimeout()))
        .build();
  }

  @Bean
  @Qualifier("adVerifyRestClient")
  RestClient adVerifyRestClient(BankAdVerifyProperties properties) {
    return RestClient.builder()
        .requestFactory(requestFactory(properties.connectTimeout(), properties.readTimeout()))
        .build();
  }

  @Bean
  JwtDecoder keycloakJwtDecoder(KeycloakProperties properties) {
    // The JWKS fetch is a Keycloak call too; it gets the same budget as the other provider calls.
    RestTemplate jwks =
        new RestTemplate(requestFactory(properties.connectTimeout(), properties.readTimeout()));
    NimbusJwtDecoder decoder =
        NimbusJwtDecoder.withJwkSetUri(properties.jwkSetUri()).restOperations(jwks).build();
    decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(properties.issuer()));
    return decoder;
  }

  static JdkClientHttpRequestFactory requestFactory(Duration connect, Duration read) {
    HttpClient httpClient =
        HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(connect)
            .build();
    JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
    factory.setReadTimeout(read);
    return factory;
  }
}
