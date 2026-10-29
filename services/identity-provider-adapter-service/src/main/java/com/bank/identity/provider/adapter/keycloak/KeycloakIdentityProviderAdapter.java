package com.bank.identity.provider.adapter.keycloak;

import com.bank.common.error.ErrorCodes;
import com.bank.common.error.ServiceErrors;
import com.bank.common.error.ServiceException;
import com.bank.identity.provider.config.KeycloakProperties;
import com.bank.identity.provider.domain.IdentityProviderPort;
import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class KeycloakIdentityProviderAdapter implements IdentityProviderPort {

  private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
      new ParameterizedTypeReference<>() {};

  private final KeycloakProperties properties;
  private final RestClient restClient;
  private final JwtDecoder jwtDecoder;
  private final ServiceErrors errors;

  public KeycloakIdentityProviderAdapter(
      KeycloakProperties properties,
      RestClient restClient,
      JwtDecoder jwtDecoder,
      ServiceErrors errors) {
    this.properties = properties;
    this.restClient = restClient;
    this.jwtDecoder = jwtDecoder;
    this.errors = errors;
  }

  @Override
  public AuthorizationResult beginAuthorization(AuthorizationCommand command) {
    UriComponentsBuilder uri =
        UriComponentsBuilder.fromUri(properties.authorizationUri())
            .queryParam("client_id", properties.clientId())
            .queryParam("redirect_uri", properties.redirectUri())
            .queryParam("response_type", "code")
            .queryParam("scope", "openid profile email")
            .queryParam("state", command.state())
            .queryParam("nonce", command.nonce())
            .queryParam("code_challenge", command.codeChallenge())
            .queryParam("code_challenge_method", "S256");
    if (hasText(command.loginHint())) {
      uri.queryParam("login_hint", command.loginHint());
    }
    if ("BANK_AD".equals(command.identitySource())
        && hasText(properties.bankIdentityProviderHint())) {
      uri.queryParam("kc_idp_hint", properties.bankIdentityProviderHint());
    }
    return new AuthorizationResult(uri.build().encode().toUri());
  }

  @Override
  public ProviderSession exchangeCode(TokenExchangeCommand command) {
    MultiValueMap<String, String> form = clientForm();
    form.add("grant_type", "authorization_code");
    form.add("code", command.code());
    form.add("code_verifier", command.codeVerifier());
    form.add("redirect_uri", properties.redirectUri().toString());
    return sessionFromTokens(requestTokens(form), command.expectedNonce());
  }

  @Override
  public ProviderSession refresh(String refreshToken) {
    MultiValueMap<String, String> form = clientForm();
    form.add("grant_type", "refresh_token");
    form.add("refresh_token", refreshToken);
    return sessionFromTokens(requestTokens(form), null);
  }

  /**
   * Password grant for an insurance user. Tokens are revoked before the method returns so this
   * check does not leave a Keycloak session and nothing is returned to the workforce app.
   */
  @Override
  public IdentityProviderPort.PasswordCheck verifyPassword(String username, String password) {
    MultiValueMap<String, String> form = clientForm();
    form.add("grant_type", "password");
    form.add("username", username);
    form.add("password", password);
    form.add("scope", "openid");
    try {
      Map<String, Object> tokens = requestTokens(form);
      String refresh = optionalString(tokens, "refresh_token");
      if (refresh != null) {
        revoke(refresh);
      }
      return new IdentityProviderPort.PasswordCheck(true, true);
    } catch (RestClientResponseException exception) {
      int status = exception.getStatusCode().value();
      if (status == 400 || status == 401) {
        String description = exception.getResponseBodyAsString().toLowerCase();
        boolean disabled = description.contains("disabled");
        return new IdentityProviderPort.PasswordCheck(false, !disabled);
      }
      throw providerUnavailable("verifyPassword", exception);
    }
  }

  /** Replaces the Keycloak password for an insurance user. The value is not logged. */
  @Override
  public void replacePassword(String username, String password) {
    String adminToken = adminAccessToken();
    String userId = findUserId(username, adminToken);
    restClient
        .put()
        .uri(
            properties
                .adminUsersUri()
                .resolve(properties.adminUsersUri().getPath() + "/" + userId + "/reset-password"))
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
        .contentType(MediaType.APPLICATION_JSON)
        .body(Map.of("type", "password", "value", password, "temporary", false))
        .retrieve()
        .toBodilessEntity();
  }

  @Override
  public void revoke(String refreshToken) {
    MultiValueMap<String, String> form = clientForm();
    form.add("token", refreshToken);
    form.add("token_type_hint", "refresh_token");
    restClient
        .post()
        .uri(properties.revokeUri())
        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
        .body(form)
        .retrieve()
        .toBodilessEntity();
  }

  @Override
  public ProvisionedIdentity provision(ProvisionIdentityCommand command) {
    String adminToken = adminAccessToken();
    List<Map<String, Object>> existing =
        restClient
            .get()
            .uri(
                UriComponentsBuilder.fromUri(properties.adminUsersUri())
                    .queryParam("username", command.username())
                    .queryParam("exact", true)
                    .build()
                    .encode()
                    .toUri())
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
            .retrieve()
            .body(new ParameterizedTypeReference<>() {});
    if (existing != null && !existing.isEmpty()) {
      Map<String, Object> match =
          existing.stream()
              .filter(user -> businessUserId(user).equals(command.businessUserId()))
              .findFirst()
              .orElseThrow(
                  () -> new IllegalStateException("Provider username is already assigned"));
      return new ProvisionedIdentity(string(match, "id"));
    }

    Map<String, Object> body = new LinkedHashMap<>();
    body.put("username", command.username());
    body.put("email", command.email());
    body.put("firstName", command.firstName());
    body.put("lastName", command.lastName());
    body.put("enabled", command.enabled());
    body.put("requiredActions", List.of("UPDATE_PASSWORD"));
    Map<String, List<String>> attributes = new LinkedHashMap<>();
    attributes.put("business_user_id", List.of(command.businessUserId()));
    if (hasText(command.insurerCode())) {
      attributes.put("insurer_code", List.of(command.insurerCode()));
    }
    body.put("attributes", attributes);

    var response =
        restClient
            .post()
            .uri(properties.adminUsersUri())
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
            .contentType(MediaType.APPLICATION_JSON)
            .body(body)
            .retrieve()
            .toBodilessEntity();
    URI location =
        Objects.requireNonNull(
            response.getHeaders().getLocation(),
            "Keycloak create-user response did not include Location");
    String path = location.getPath();
    return new ProvisionedIdentity(path.substring(path.lastIndexOf('/') + 1));
  }

  @Override
  public void setEnabled(String providerSubjectId, boolean enabled) {
    restClient
        .put()
        .uri(
            properties
                .adminUsersUri()
                .resolve(properties.adminUsersUri().getPath() + "/" + providerSubjectId))
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminAccessToken())
        .contentType(MediaType.APPLICATION_JSON)
        .body(Map.of("enabled", enabled))
        .retrieve()
        .toBodilessEntity();
  }

  private String findUserId(String username, String adminToken) {
    List<Map<String, Object>> existing =
        restClient
            .get()
            .uri(
                UriComponentsBuilder.fromUri(properties.adminUsersUri())
                    .queryParam("username", username)
                    .queryParam("exact", true)
                    .build()
                    .encode()
                    .toUri())
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
            .retrieve()
            .body(new ParameterizedTypeReference<>() {});
    if (existing == null || existing.isEmpty()) {
      throw errors
          .error(ErrorCodes.RESOURCE_NOT_FOUND)
          .component("KeycloakIdentityProviderAdapter")
          .operation("replacePassword")
          .reason("Keycloak has no user for this insurance username")
          .build();
    }
    return string(existing.getFirst(), "id");
  }

  private ServiceException providerUnavailable(
      String operation, RestClientResponseException cause) {
    return errors
        .error(ErrorCodes.IDENTITY_PROVIDER_UNAVAILABLE)
        .component("KeycloakIdentityProviderAdapter")
        .operation(operation)
        .reason("Keycloak password grant failed")
        .cause(cause)
        .build();
  }

  private MultiValueMap<String, String> clientForm() {
    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("client_id", properties.clientId());
    form.add("client_secret", properties.clientSecret());
    return form;
  }

  private Map<String, Object> requestTokens(MultiValueMap<String, String> form) {
    return Objects.requireNonNull(
        restClient
            .post()
            .uri(properties.tokenUri())
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(form)
            .retrieve()
            .body(MAP_TYPE),
        "Provider returned an empty token response");
  }

  private ProviderSession sessionFromTokens(Map<String, Object> tokens, String expectedNonce) {
    String accessToken = string(tokens, "access_token");
    String idToken = optionalString(tokens, "id_token");
    Jwt verifiedIdToken = idToken == null ? null : decodeIdToken(idToken);
    if (verifiedIdToken != null && !verifiedIdToken.getAudience().contains(properties.clientId())) {
      throw authenticationFailed("Provider ID token audience is invalid");
    }
    if (expectedNonce != null
        && (verifiedIdToken == null
            || !expectedNonce.equals(verifiedIdToken.getClaimAsString("nonce")))) {
      throw authenticationFailed("Provider ID token nonce is invalid");
    }
    Map<String, Object> claims =
        Objects.requireNonNull(
            restClient
                .get()
                .uri(properties.userInfoUri())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .body(MAP_TYPE),
            "Provider returned an empty user-info response");
    if (verifiedIdToken != null && !string(claims, "sub").equals(verifiedIdToken.getSubject())) {
      throw authenticationFailed("Provider subject mismatch");
    }
    long expiresIn = ((Number) tokens.getOrDefault("expires_in", 300)).longValue();
    return new ProviderSession(
        string(claims, "sub"),
        optionalString(claims, "preferred_username"),
        optionalString(claims, "email"),
        accessToken,
        optionalString(tokens, "refresh_token"),
        idToken,
        Instant.now().plusSeconds(expiresIn),
        Map.copyOf(claims));
  }

  private Jwt decodeIdToken(String idToken) {
    try {
      return jwtDecoder.decode(idToken);
    } catch (JwtException exception) {
      throw authenticationFailed("Provider ID token failed verification", exception);
    }
  }

  // A token that fails audience, nonce, subject or signature checks is a rejected login (typed
  // 401 the BFF relays), not a server fault (READINESS-REVIEW §3.7).
  private ServiceException authenticationFailed(String reason) {
    return authenticationFailed(reason, null);
  }

  private ServiceException authenticationFailed(String reason, Throwable cause) {
    var builder =
        errors
            .error(ErrorCodes.AUTHENTICATION_FAILED)
            .component("KeycloakIdentityProviderAdapter")
            .operation("sessionFromTokens")
            .reason(reason);
    if (cause != null) {
      builder = builder.cause(cause);
    }
    return builder.build();
  }

  private String adminAccessToken() {
    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("grant_type", "client_credentials");
    form.add("client_id", properties.adminClientId());
    form.add("client_secret", properties.adminClientSecret());
    return string(requestTokens(form), "access_token");
  }

  private static String string(Map<String, Object> values, String key) {
    Object value = values.get(key);
    if (value == null || value.toString().isBlank()) {
      throw new IllegalStateException("Provider response is missing " + key);
    }
    return value.toString();
  }

  private static String optionalString(Map<String, Object> values, String key) {
    Object value = values.get(key);
    return value == null ? null : value.toString();
  }

  @SuppressWarnings("unchecked")
  private static String businessUserId(Map<String, Object> user) {
    Object rawAttributes = user.get("attributes");
    if (!(rawAttributes instanceof Map<?, ?> attributes)) {
      return "";
    }
    Object rawValues = attributes.get("business_user_id");
    if (!(rawValues instanceof List<?> values) || values.isEmpty()) {
      return "";
    }
    return Objects.toString(values.getFirst(), "");
  }

  private static boolean hasText(String value) {
    return value != null && !value.isBlank();
  }
}
