package com.bank.workforce.bff.client;

import com.bank.common.error.ErrorPropagation;
import com.bank.common.error.PlatformLayer;
import com.bank.common.error.ProblemJsonReader;
import com.bank.common.error.ServiceErrors;
import com.bank.workforce.bff.config.DownstreamProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.URI;
import java.util.function.Supplier;
import java.time.Instant;
import java.util.Map;

@Component
public class IdentityProviderClient {

    private static final String DOWNSTREAM = "idp-adapter";

    private final RestClient client;
    private final ProblemJsonReader problemJsonReader;
    private final ServiceErrors serviceErrors;

    public IdentityProviderClient(RestClient restClient, DownstreamProperties properties,
                                  ProblemJsonReader problemJsonReader,
                                  ServiceErrors serviceErrors) {
        this.client = restClient.mutate().baseUrl(properties.providerAdapterBaseUrl().toString()).build();
        this.problemJsonReader = problemJsonReader;
        this.serviceErrors = serviceErrors;
    }

    public URI authorizationUri(AuthorizationUriRequest request) {
        return propagating("authorizationUri", () ->
            client.post().uri("/internal/v1/auth/authorization-uri").body(request).retrieve()
                .body(AuthorizationUriResponse.class).authorizationUri());
    }

    public ProviderSession exchange(String code, String verifier, String expectedNonce) {
        return propagating("tokenExchange", () ->
            client.post().uri("/internal/v1/auth/token-exchange")
                .body(new TokenExchangeRequest(code, verifier, expectedNonce)).retrieve()
                .body(ProviderSession.class));
    }

    public void revoke(String refreshToken) {
        propagating("revoke", () ->
            client.post().uri("/internal/v1/auth/revoke")
                .body(new RefreshRequest(refreshToken)).retrieve().toBodilessEntity());
    }

    /**
     * Turns the adapter's failure into this service's, keeping the incident id and the origin.
     *
     * <p>One wrapper rather than three copies of the same try/catch — the three calls differ only
     * in the operation they name.
     */
    private <T> T propagating(String operation, Supplier<T> call) {
        try {
            return call.get();
        } catch (RestClientResponseException ex) {
            throw ErrorPropagation
                .from(problemJsonReader.read(ex.getResponseBodyAsString(), ex.getStatusCode().value()))
                .receivedBy(serviceErrors.serviceId(), PlatformLayer.L4)
                .calling(DOWNSTREAM, operation)
                .causedBy(ex)
                .toException();
        }
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
