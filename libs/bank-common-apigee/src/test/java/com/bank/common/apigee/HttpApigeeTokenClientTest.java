package com.bank.common.apigee;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestClient;

@Tag("unit")
@Tag("FUNC-031")
class HttpApigeeTokenClientTest {

  @RegisterExtension
  static WireMockExtension wireMock =
      WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

  private RestClient restClient;
  private final Clock clock = Clock.fixed(Instant.parse("2026-10-07T12:00:00Z"), ZoneOffset.UTC);

  @BeforeEach
  void restClient() {
    HttpClient httpClient =
        HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(2))
            .build();
    JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
    factory.setReadTimeout(Duration.ofSeconds(2));
    restClient =
        RestClient.builder()
            .requestFactory(factory)
            .messageConverters(converters -> converters.add(new MappingJackson2HttpMessageConverter()))
            .build();
  }

  @Test
  void mintsAccessTokenFromClientCredentialsGrant() {
    wireMock.stubFor(
        post("/token")
            .withHeader(
                "Authorization",
                equalTo("Basic " + Base64.getEncoder().encodeToString("id:secret".getBytes())))
            .withHeader("Content-Type", containing("application/x-www-form-urlencoded"))
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"access_token\":\"live-token\",\"expires_in\":86400}")));

    HttpApigeeTokenClient client =
        new HttpApigeeTokenClient(
            restClient, URI.create(wireMock.baseUrl() + "/token"), "id", "secret", clock);

    IssuedToken issued = client.fetchClientCredentials();

    assertThat(issued.value()).isEqualTo("live-token");
    assertThat(issued.expiresAt()).isEqualTo(clock.instant().plusSeconds(86400));
    wireMock.verify(postRequestedFor(urlEqualTo("/token")));
  }

  @Test
  void rejectsBlankSecretsWithoutCallingApigee() {
    assertThatThrownBy(
            () ->
                new HttpApigeeTokenClient(
                    restClient, URI.create(wireMock.baseUrl() + "/token"), "id", " ", clock))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("APIGEE_CLIENT_SECRET");
  }

  @Test
  void tokenHttpFailureDoesNotIncludeSecret() {
    wireMock.stubFor(post("/token").willReturn(aResponse().withStatus(401).withBody("denied")));
    HttpApigeeTokenClient client =
        new HttpApigeeTokenClient(
            restClient, URI.create(wireMock.baseUrl() + "/token"), "id", "super-secret", clock);

    assertThatThrownBy(client::fetchClientCredentials)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageNotContaining("super-secret");
  }
}
