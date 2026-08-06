package com.bank.workforce.bff.session;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public final class SessionModels {
    private SessionModels() {}

    public enum ClientType { WEB, NATIVE }
    public enum IdentitySource { BANK_AD, PARTNER }

    public record PendingLogin(
        String state,
        String nonce,
        String codeVerifier,
        ClientType clientType,
        IdentitySource identitySource,
        String returnUri,
        Instant createdAt
    ) {}

    public record WorkforceSession(
        String sessionId,
        UUID businessUserId,
        String userType,
        String status,
        long policyVersion,
        String insurerCode,
        String providerSubjectId,
        String username,
        String accessToken,
        String refreshToken,
        String idToken,
        Instant accessTokenExpiresAt,
        Instant createdAt,
        Map<String, Object> providerClaims
    ) {}

    public record NativeCompletion(String sessionId) {}
}
