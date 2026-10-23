package com.bank.insurance.onesb.config;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.web.client.RestClient;

/**
 * bank-persistence refuses /internal/v1/** without the internal key; the client always sends it.
 */
class PersistenceClientConfigTest {

  @RegisterExtension
  static WireMockExtension wireMock =
      WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

  @Test
  void everyCallCarriesTheInternalServiceKey() {
    wireMock.stubFor(
        get(urlEqualTo("/internal/v1/raw-payloads/p-1")).willReturn(aResponse().withStatus(200)));
    RestClient client =
        new PersistenceClientConfig()
            .persistenceRestClient(
                new PersistenceClientProperties(wireMock.baseUrl(), "test-persistence-key-123"));

    client.get().uri("/internal/v1/raw-payloads/p-1").retrieve().toBodilessEntity();

    wireMock.verify(
        getRequestedFor(urlEqualTo("/internal/v1/raw-payloads/p-1"))
            .withHeader("X-Internal-Service-Key", equalTo("test-persistence-key-123")));
  }

  @Test
  void missingKeyFailsStartupAndToStringRedacts() {
    assertThatThrownBy(() -> new PersistenceClientProperties("http://x", " "))
        .isInstanceOf(IllegalStateException.class);
    assertThat(new PersistenceClientProperties(null, "secret-value-xyz").toString())
        .doesNotContain("secret-value-xyz")
        .contains("http://localhost:8081");
  }
}
