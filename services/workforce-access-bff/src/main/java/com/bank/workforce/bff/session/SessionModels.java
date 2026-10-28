package com.bank.workforce.bff.session;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public final class SessionModels {
  private SessionModels() {}

  public enum ClientType {
    WEB,
    NATIVE
  }

  /** Pending Keycloak OIDC login (partner representatives only). */
  public record PendingLogin(
      String state,
      String nonce,
      String codeVerifier,
      ClientType clientType,
      String returnUri,
      Instant createdAt) {}

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
      Map<String, Object> providerClaims) {}

  public record NativeCompletion(String sessionId) {}
}
