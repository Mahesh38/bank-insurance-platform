package com.bank.identity.provider.domain;

import java.net.URI;
import java.time.Instant;
import java.util.Map;

/** Provider-neutral identity operations. Provider-specific DTOs must not cross this port. */
public interface IdentityProviderPort {

    AuthorizationResult beginAuthorization(AuthorizationCommand command);

    ProviderSession exchangeCode(TokenExchangeCommand command);

    ProviderSession refresh(String refreshToken);

    void revoke(String refreshToken);

    ProvisionedIdentity provision(ProvisionIdentityCommand command);

    void setEnabled(String providerSubjectId, boolean enabled);

    record AuthorizationCommand(
        String state,
        String nonce,
        String codeChallenge,
        String loginHint,
        String identitySource
    ) {}

    record AuthorizationResult(URI authorizationUri) {}

    record TokenExchangeCommand(String code, String codeVerifier, String expectedNonce) {}

    record ProviderSession(
        String providerSubjectId,
        String username,
        String email,
        String accessToken,
        String refreshToken,
        String idToken,
        Instant accessTokenExpiresAt,
        Map<String, Object> claims
    ) {}

    record ProvisionIdentityCommand(
        String businessUserId,
        String username,
        String email,
        String firstName,
        String lastName,
        String insurerCode,
        boolean enabled
    ) {}

    record ProvisionedIdentity(String providerSubjectId) {}
}
