package com.bank.common.apigee;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Live AU Bank Apigee client-credentials mint.
 *
 * <p>UAT contract (owner 2026-10-08): {@code POST
 * https://api.aubankuat.in/oauth/accesstoken?grant_type=client_credentials} with HTTP Basic. The
 * configured URI may omit {@code grant_type}; this client appends it. Client id and secret are
 * constructor arguments from environment — never from git. The request URI is not logged (OBS-4).
 */
public final class HttpApigeeTokenClient implements ApigeeTokenClient {

  private static final Logger log = LoggerFactory.getLogger(HttpApigeeTokenClient.class);
  private static final Duration DEFAULT_TTL = Duration.ofHours(1);

  private final RestClient client;
  private final URI tokenUri;
  private final String clientId;
  private final String clientSecret;
  private final Clock clock;

  public HttpApigeeTokenClient(
      RestClient client, URI tokenUri, String clientId, String clientSecret, Clock clock) {
    this.client = Objects.requireNonNull(client, "client");
    this.tokenUri = Objects.requireNonNull(tokenUri, "tokenUri");
    this.clientId = requireSecret(clientId, "APIGEE_CLIENT_ID");
    this.clientSecret = requireSecret(clientSecret, "APIGEE_CLIENT_SECRET");
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  @Override
  public IssuedToken fetchClientCredentials() {
    try {
      TokenResponse body =
          client
              .post()
              .uri(withClientCredentialsGrant(tokenUri))
              .headers(headers -> headers.setBasicAuth(clientId, clientSecret))
              .retrieve()
              .body(TokenResponse.class);
      if (body == null || body.accessToken() == null || body.accessToken().isBlank()) {
        log.warn("event=APIGEE_TOKEN_INVALID_RESPONSE operation=token reason=omitted_access_token");
        throw new IllegalStateException("Apigee token response omitted access_token");
      }
      Duration ttl =
          body.expiresIn() == null || body.expiresIn() <= 0
              ? DEFAULT_TTL
              : Duration.ofSeconds(body.expiresIn());
      return new IssuedToken(body.accessToken(), clock.instant().plus(ttl));
    } catch (RestClientResponseException ex) {
      log.warn("event=APIGEE_TOKEN_REJECTED operation=token status={}", ex.getStatusCode().value());
      throw new IllegalStateException("Apigee /token rejected the client-credentials grant", ex);
    }
  }

  /** AU Bank UAT puts {@code grant_type} on the query string, not in a form body. */
  static URI withClientCredentialsGrant(URI tokenUri) {
    String query = tokenUri.getRawQuery();
    if (query != null && query.contains("grant_type=")) {
      return tokenUri;
    }
    String raw = tokenUri.toString();
    String sep = raw.contains("?") ? "&" : "?";
    return URI.create(raw + sep + "grant_type=client_credentials");
  }

  private static String requireSecret(String value, String name) {
    if (value == null || value.isBlank()) {
      throw new IllegalStateException(name + " is required when token-mode=http");
    }
    return value;
  }

  private record TokenResponse(
      @JsonProperty("access_token") String accessToken,
      @JsonProperty("expires_in") Long expiresIn) {}
}
