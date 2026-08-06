package com.bank.identity.provider.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.net.URI;

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
    String bankIdentityProviderHint
) {
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
        return backchannelBaseUrl.resolve("/realms/" + realm + "/protocol/openid-connect/certs").toString();
    }

    public URI revokeUri() {
        return backchannelBaseUrl.resolve("/realms/" + realm + "/protocol/openid-connect/revoke");
    }

    public URI adminUsersUri() {
        return backchannelBaseUrl.resolve("/admin/realms/" + realm + "/users");
    }
}
