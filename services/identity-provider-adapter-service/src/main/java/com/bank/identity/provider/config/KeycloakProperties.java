package com.bank.identity.provider.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("identity.provider.keycloak")
public record KeycloakProperties(
    @NotNull URI publicBaseUrl,
    @NotNull URI backchannelBaseUrl,
    @NotBlank String realm,
    @NotBlank String clientId,
    @NotBlank String clientSecret,
    @NotNull URI redirectUri,
    @NotBlank String adminClientId,
    @NotBlank String adminClientSecret,
    String bankIdentityProviderHint,
    Duration connectTimeout,
    Duration readTimeout) {
  // READINESS-REVIEW §3.7: the Keycloak client had no timeout. Defaults nest inside the BFF's
  // 4.5 s identity-provider read budget (idempotency-timeouts-retries.md §2.1).
  public static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(1);
  public static final Duration DEFAULT_READ_TIMEOUT = Duration.ofSeconds(3);

  public KeycloakProperties {
    connectTimeout = connectTimeout != null ? connectTimeout : DEFAULT_CONNECT_TIMEOUT;
    readTimeout = readTimeout != null ? readTimeout : DEFAULT_READ_TIMEOUT;
  }

  public URI realmUri() {
    return publicBaseUrl.resolve("/realms/" + realm);
  }

  public URI tokenUri() {
    return backchannelBaseUrl.resolve("/realms/" + realm + "/protocol/openid-connect/token");
  }

  public URI authorizationUri() {
    return publicBaseUrl.resolve("/realms/" + realm + "/protocol/openid-connect/auth");
  }

  public URI userInfoUri() {
    return backchannelBaseUrl.resolve("/realms/" + realm + "/protocol/openid-connect/userinfo");
  }

  public String issuer() {
    return realmUri().toString();
  }

  public String jwkSetUri() {
    return backchannelBaseUrl
        .resolve("/realms/" + realm + "/protocol/openid-connect/certs")
        .toString();
  }

  public URI revokeUri() {
    return backchannelBaseUrl.resolve("/realms/" + realm + "/protocol/openid-connect/revoke");
  }

  public URI adminUsersUri() {
    return backchannelBaseUrl.resolve("/admin/realms/" + realm + "/users");
  }
}
