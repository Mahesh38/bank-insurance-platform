package com.bank.identity.provider.api;

import com.bank.identity.provider.domain.IdentityProviderPort;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/internal/v1")
public class IdentityProviderController {

    private final IdentityProviderPort provider;

    public IdentityProviderController(IdentityProviderPort provider) {
        this.provider = provider;
    }

    @PostMapping("/auth/authorization-uri")
    public AuthorizationUriResponse authorizationUri(@Valid @RequestBody AuthorizationUriRequest request) {
        var result = provider.beginAuthorization(new IdentityProviderPort.AuthorizationCommand(
            request.state(), request.nonce(), request.codeChallenge(), request.loginHint(), request.identitySource()));
        return new AuthorizationUriResponse(result.authorizationUri());
    }

    @PostMapping("/auth/token-exchange")
    public ProviderSessionResponse exchange(@Valid @RequestBody TokenExchangeRequest request) {
        return ProviderSessionResponse.from(provider.exchangeCode(
            new IdentityProviderPort.TokenExchangeCommand(request.code(), request.codeVerifier(), request.expectedNonce())));
    }

    @PostMapping("/auth/refresh")
    public ProviderSessionResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return ProviderSessionResponse.from(provider.refresh(request.refreshToken()));
    }

    @PostMapping("/auth/revoke")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(@Valid @RequestBody RefreshRequest request) {
        provider.revoke(request.refreshToken());
    }

    @PostMapping("/identities")
    @ResponseStatus(HttpStatus.CREATED)
    public ProvisionedIdentityResponse provision(@Valid @RequestBody ProvisionIdentityRequest request) {
        var provisioned = provider.provision(new IdentityProviderPort.ProvisionIdentityCommand(
            request.businessUserId(), request.username(), request.email(), request.firstName(), request.lastName(),
            request.insurerCode(), request.enabled()));
        return new ProvisionedIdentityResponse(provisioned.providerSubjectId());
    }

    @PatchMapping("/identities/{providerSubjectId}/status")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setStatus(@PathVariable String providerSubjectId, @Valid @RequestBody IdentityStatusRequest request) {
        provider.setEnabled(providerSubjectId, request.enabled());
    }

    public record AuthorizationUriRequest(
        @NotBlank String state,
        @NotBlank String nonce,
        @NotBlank String codeChallenge,
        String loginHint,
        @NotBlank String identitySource
    ) {}

    public record AuthorizationUriResponse(URI authorizationUri) {}

    public record TokenExchangeRequest(
        @NotBlank String code,
        @NotBlank String codeVerifier,
        @NotBlank String expectedNonce
    ) {}

    public record RefreshRequest(@NotBlank String refreshToken) {}

    public record ProviderSessionResponse(
        String providerSubjectId,
        String username,
        String email,
        String accessToken,
        String refreshToken,
        String idToken,
        Instant accessTokenExpiresAt,
        Map<String, Object> claims
    ) {
        static ProviderSessionResponse from(IdentityProviderPort.ProviderSession session) {
            return new ProviderSessionResponse(session.providerSubjectId(), session.username(), session.email(),
                session.accessToken(), session.refreshToken(), session.idToken(), session.accessTokenExpiresAt(),
                session.claims());
        }
    }

    public record ProvisionIdentityRequest(
        @NotBlank String businessUserId,
        @NotBlank String username,
        @Email @NotBlank String email,
        String firstName,
        String lastName,
        String insurerCode,
        boolean enabled
    ) {}

    public record ProvisionedIdentityResponse(String providerSubjectId) {}

    public record IdentityStatusRequest(@NotNull Boolean enabled) {}
}
