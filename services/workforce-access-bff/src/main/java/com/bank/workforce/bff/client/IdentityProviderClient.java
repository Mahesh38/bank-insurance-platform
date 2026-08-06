package com.bank.workforce.bff.client;

import com.bank.workforce.bff.config.DownstreamProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.time.Instant;
import java.util.Map;

@Component
public class IdentityProviderClient {

    private final RestClient client;

    public IdentityProviderClient(RestClient restClient, DownstreamProperties properties) {
        this.client = restClient.mutate().baseUrl(properties.providerAdapterBaseUrl().toString()).build();
    }

    public URI authorizationUri(AuthorizationUriRequest request) {
        return client.post().uri("/internal/v1/auth/authorization-uri").body(request).retrieve()
            .body(AuthorizationUriResponse.class).authorizationUri();
    }

    public ProviderSession exchange(String code, String verifier, String expectedNonce) {
        return client.post().uri("/internal/v1/auth/token-exchange")
            .body(new TokenExchangeRequest(code, verifier, expectedNonce)).retrieve().body(ProviderSession.class);
    }

    public void revoke(String refreshToken) {
        client.post().uri("/internal/v1/auth/revoke")
            .body(new RefreshRequest(refreshToken)).retrieve().toBodilessEntity();
    }

    public record AuthorizationUriRequest(
        String state,
        String nonce,
        String codeChallenge,
        String loginHint,
        String identitySource
    ) {}

    private record AuthorizationUriResponse(URI authorizationUri) {}
    private record TokenExchangeRequest(String code, String codeVerifier, String expectedNonce) {}
    private record RefreshRequest(String refreshToken) {}

    public record ProviderSession(
        String providerSubjectId,
        String username,
        String email,
        String accessToken,
        String refreshToken,
        String idToken,
        Instant accessTokenExpiresAt,
        Map<String, Object> claims
    ) {}
}
