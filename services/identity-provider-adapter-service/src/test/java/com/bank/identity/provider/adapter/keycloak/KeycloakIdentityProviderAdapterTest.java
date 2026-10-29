package com.bank.identity.provider.adapter.keycloak;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withCreatedEntity;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withNoContent;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withUnauthorizedRequest;

import com.bank.common.error.ErrorCodes;
import com.bank.common.error.PlatformLayer;
import com.bank.common.error.ServiceErrors;
import com.bank.common.error.ServiceException;
import com.bank.identity.provider.config.KeycloakProperties;
import com.bank.identity.provider.domain.IdentityProviderPort;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class KeycloakIdentityProviderAdapterTest {

  private static final KeycloakProperties PROPERTIES =
      new KeycloakProperties(
          URI.create("https://login.example.test"),
          URI.create("http://keycloak:8080"),
          "bank-insurance",
          "workforce-bff",
          "secret",
          URI.create("https://bff.example/api/v1/auth/callback"),
          "admin-client",
          "admin-secret",
          "bank-ad",
          null,
          null);

  private static final ServiceErrors ERRORS = ServiceErrors.of("idp-adapter", PlatformLayer.L5);

  @Test
  void buildsAuthorizationCodePkceRequestWithoutLeakingProviderDetailsToCaller() {
    var adapter =
        new KeycloakIdentityProviderAdapter(
            PROPERTIES, RestClient.create(), mock(JwtDecoder.class), ERRORS);

    var result =
        adapter.beginAuthorization(
            new IdentityProviderPort.AuthorizationCommand(
                "state-1", "nonce-1", "challenge-1", "rm@example.test", "BANK_AD"));

    assertThat(result.authorizationUri().toString())
        .startsWith(
            "https://login.example.test/realms/bank-insurance/protocol/openid-connect/auth?")
        .contains("response_type=code")
        .contains("code_challenge=challenge-1")
        .contains("code_challenge_method=S256")
        .contains("kc_idp_hint=bank-ad")
        .doesNotContain("secret");
  }

  @Test
  void exchangesAuthorizationCodeAndNormalizesUserInfo() {
    TestClient test = testClient();
    test.server()
        .expect(requestTo(PROPERTIES.tokenUri()))
        .andExpect(method(HttpMethod.POST))
        .andRespond(
            withSuccess(
                """
                {"access_token":"access-1","refresh_token":"refresh-1","id_token":"id-1","expires_in":300}
                """,
                MediaType.APPLICATION_JSON));
    test.server()
        .expect(requestTo(PROPERTIES.userInfoUri()))
        .andExpect(method(HttpMethod.GET))
        .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer access-1"))
        .andRespond(
            withSuccess(
                """
                {"sub":"subject-1","preferred_username":"rm.demo","email":"rm@example.test"}
                """,
                MediaType.APPLICATION_JSON));

    var session =
        test.adapter()
            .exchangeCode(
                new IdentityProviderPort.TokenExchangeCommand("code", "verifier", "nonce-1"));

    assertThat(session.providerSubjectId()).isEqualTo("subject-1");
    assertThat(session.username()).isEqualTo("rm.demo");
    assertThat(session.refreshToken()).isEqualTo("refresh-1");
    assertThat(session.claims()).containsEntry("email", "rm@example.test");
    test.server().verify();
  }

  @Test
  void refreshesAndRevokesProviderSession() {
    TestClient test = testClient();
    test.server()
        .expect(requestTo(PROPERTIES.tokenUri()))
        .andExpect(method(HttpMethod.POST))
        .andRespond(
            withSuccess(
                """
                {"access_token":"access-2","refresh_token":"refresh-2","expires_in":120}
                """,
                MediaType.APPLICATION_JSON));
    test.server()
        .expect(requestTo(PROPERTIES.userInfoUri()))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess("{" + "\"sub\":\"subject-2\"}", MediaType.APPLICATION_JSON));
    test.server()
        .expect(requestTo(PROPERTIES.revokeUri()))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withNoContent());

    var session = test.adapter().refresh("refresh-1");
    test.adapter().revoke(session.refreshToken());

    assertThat(session.accessToken()).isEqualTo("access-2");
    assertThat(session.idToken()).isNull();
    test.server().verify();
  }

  @Test
  void provisioningIsIdempotentForTheSameBusinessIdentity() {
    TestClient test = testClient();
    expectAdminToken(test.server());
    test.server()
        .expect(requestTo(PROPERTIES.adminUsersUri() + "?username=partner.demo&exact=true"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(
            withSuccess(
                """
                [{"id":"provider-1","attributes":{"business_user_id":["business-1"]}}]
                """,
                MediaType.APPLICATION_JSON));

    var result = test.adapter().provision(partnerCommand());

    assertThat(result.providerSubjectId()).isEqualTo("provider-1");
    test.server().verify();
  }

  @Test
  void createsAndEnablesNewProviderIdentity() {
    TestClient test = testClient();
    expectAdminToken(test.server());
    test.server()
        .expect(requestTo(PROPERTIES.adminUsersUri() + "?username=partner.demo&exact=true"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));
    test.server()
        .expect(requestTo(PROPERTIES.adminUsersUri()))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer admin-access"))
        .andRespond(withCreatedEntity(URI.create(PROPERTIES.adminUsersUri() + "/provider-2")));
    expectAdminToken(test.server());
    test.server()
        .expect(requestTo(PROPERTIES.adminUsersUri() + "/provider-2"))
        .andExpect(method(HttpMethod.PUT))
        .andRespond(withNoContent());

    var result = test.adapter().provision(partnerCommand());
    test.adapter().setEnabled(result.providerSubjectId(), true);

    assertThat(result.providerSubjectId()).isEqualTo("provider-2");
    test.server().verify();
  }

  @Test
  void timeoutsDefaultWhenNotConfigured() {
    assertThat(PROPERTIES.connectTimeout()).isEqualTo(KeycloakProperties.DEFAULT_CONNECT_TIMEOUT);
    assertThat(PROPERTIES.readTimeout()).isEqualTo(KeycloakProperties.DEFAULT_READ_TIMEOUT);
  }

  @Test
  void nonceMismatchIsTypedAuthenticationFailureNotServerError() {
    TestClient test = testClient();
    expectTokens(test.server());

    assertAuthenticationFailed(
        () ->
            test.adapter()
                .exchangeCode(
                    new IdentityProviderPort.TokenExchangeCommand(
                        "code", "verifier", "other-nonce")));
  }

  @Test
  void audienceMismatchIsTypedAuthenticationFailure() {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    JwtDecoder decoder = mock(JwtDecoder.class);
    when(decoder.decode("id-1")).thenReturn(jwt("subject-1", "someone-else"));
    var adapter = new KeycloakIdentityProviderAdapter(PROPERTIES, builder.build(), decoder, ERRORS);
    expectTokens(server);

    assertAuthenticationFailed(
        () ->
            adapter.exchangeCode(
                new IdentityProviderPort.TokenExchangeCommand("code", "verifier", "nonce-1")));
  }

  @Test
  void subjectMismatchIsTypedAuthenticationFailure() {
    TestClient test = testClient();
    expectTokens(test.server());
    test.server()
        .expect(requestTo(PROPERTIES.userInfoUri()))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess("{\"sub\":\"another-subject\"}", MediaType.APPLICATION_JSON));

    assertAuthenticationFailed(
        () ->
            test.adapter()
                .exchangeCode(
                    new IdentityProviderPort.TokenExchangeCommand("code", "verifier", "nonce-1")));
  }

  @Test
  void unverifiableIdTokenIsTypedAuthenticationFailure() {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    JwtDecoder decoder = mock(JwtDecoder.class);
    when(decoder.decode("id-1")).thenThrow(new BadJwtException("bad signature"));
    var adapter = new KeycloakIdentityProviderAdapter(PROPERTIES, builder.build(), decoder, ERRORS);
    expectTokens(server);

    assertAuthenticationFailed(
        () ->
            adapter.exchangeCode(
                new IdentityProviderPort.TokenExchangeCommand("code", "verifier", "nonce-1")));
  }

  private static void assertAuthenticationFailed(
      org.assertj.core.api.ThrowableAssert.ThrowingCallable call) {
    assertThatThrownBy(call)
        .isInstanceOf(ServiceException.class)
        .satisfies(
            ex -> {
              var response = ((ServiceException) ex).getErrorResponse();
              assertThat(response.getCode()).isEqualTo(ErrorCodes.AUTHENTICATION_FAILED);
              assertThat(response.getStatus()).isEqualTo(401);
            });
  }

  @Test
  void passwordGrantRevokesTheRefreshTokenAndDoesNotReturnIt() {
    TestClient test = testClient();
    test.server()
        .expect(requestTo(PROPERTIES.tokenUri()))
        .andExpect(method(HttpMethod.POST))
        .andRespond(
            withSuccess(
                """
                {"access_token":"access-1","refresh_token":"refresh-1","expires_in":300}
                """,
                MediaType.APPLICATION_JSON));
    test.server()
        .expect(requestTo(PROPERTIES.revokeUri()))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withNoContent());

    var check = test.adapter().verifyPassword("fls.rm@example.test", "Horse#99");

    assertThat(check.authenticated()).isTrue();
    assertThat(check.accountActive()).isTrue();
    test.server().verify();
  }

  @Test
  void disabledKeycloakAccountDoesNotCountAsAWrongPassword() {
    TestClient test = testClient();
    test.server()
        .expect(requestTo(PROPERTIES.tokenUri()))
        .andExpect(method(HttpMethod.POST))
        .andRespond(
            withBadRequest()
                .body("{\"error\":\"invalid_grant\",\"error_description\":\"Account disabled\"}"));

    var check = test.adapter().verifyPassword("fls.rm@example.test", "Horse#99");

    assertThat(check.authenticated()).isFalse();
    assertThat(check.accountActive()).isFalse();
  }

  @Test
  void wrongPasswordStaysActiveAndAGrantWithoutARefreshTokenIsNotRevoked() {
    TestClient wrongPassword = testClient();
    wrongPassword
        .server()
        .expect(requestTo(PROPERTIES.tokenUri()))
        .andExpect(method(HttpMethod.POST))
        .andRespond(
            withUnauthorizedRequest()
                .body(
                    "{\"error\":\"invalid_grant\",\"error_description\":\"Invalid user credentials\"}"));

    var rejected = wrongPassword.adapter().verifyPassword("fls.rm@example.test", "nope");
    assertThat(rejected.authenticated()).isFalse();
    assertThat(rejected.accountActive()).isTrue();

    TestClient noRefresh = testClient();
    noRefresh
        .server()
        .expect(requestTo(PROPERTIES.tokenUri()))
        .andExpect(method(HttpMethod.POST))
        .andRespond(
            withSuccess(
                "{\"access_token\":\"access-1\",\"expires_in\":300}", MediaType.APPLICATION_JSON));

    var accepted = noRefresh.adapter().verifyPassword("fls.rm@example.test", "Horse#99");
    assertThat(accepted.authenticated()).isTrue();
    assertThat(accepted.accountActive()).isTrue();
    noRefresh.server().verify();
  }

  @Test
  void passwordGrantFailureThatIsNotACredentialRejectionIsUnavailable() {
    TestClient test = testClient();
    test.server()
        .expect(requestTo(PROPERTIES.tokenUri()))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withServerError());

    assertThatThrownBy(() -> test.adapter().verifyPassword("fls.rm@example.test", "Horse#99"))
        .isInstanceOf(ServiceException.class)
        .extracting(ex -> ((ServiceException) ex).getErrorResponse().getCode())
        .isEqualTo(ErrorCodes.IDENTITY_PROVIDER_UNAVAILABLE);
  }

  @Test
  void replacePasswordResetsTheMatchedKeycloakUser() {
    TestClient test = testClient();
    expectAdminToken(test.server());
    test.server()
        .expect(requestTo(PROPERTIES.adminUsersUri() + "?username=fls.rm&exact=true"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess("[{\"id\":\"provider-9\"}]", MediaType.APPLICATION_JSON));
    test.server()
        .expect(requestTo(PROPERTIES.adminUsersUri() + "/provider-9/reset-password"))
        .andExpect(method(HttpMethod.PUT))
        .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer admin-access"))
        .andExpect(
            content().json("{\"type\":\"password\",\"value\":\"Zebra#99\",\"temporary\":false}"))
        .andRespond(withNoContent());

    test.adapter().replacePassword("fls.rm", "Zebra#99");

    test.server().verify();
  }

  @Test
  void replacePasswordFailsWhenKeycloakHasNoMatchingUser() {
    TestClient empty = testClient();
    expectAdminToken(empty.server());
    empty
        .server()
        .expect(requestTo(PROPERTIES.adminUsersUri() + "?username=fls.rm&exact=true"))
        .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

    assertThatThrownBy(() -> empty.adapter().replacePassword("fls.rm", "Zebra#99"))
        .isInstanceOf(ServiceException.class)
        .extracting(ex -> ((ServiceException) ex).getErrorResponse().getCode())
        .isEqualTo(ErrorCodes.RESOURCE_NOT_FOUND);

    TestClient missing = testClient();
    expectAdminToken(missing.server());
    missing
        .server()
        .expect(requestTo(PROPERTIES.adminUsersUri() + "?username=fls.rm&exact=true"))
        .andRespond(withSuccess("null", MediaType.APPLICATION_JSON));

    assertThatThrownBy(() -> missing.adapter().replacePassword("fls.rm", "Zebra#99"))
        .isInstanceOf(ServiceException.class)
        .extracting(ex -> ((ServiceException) ex).getErrorResponse().getCode())
        .isEqualTo(ErrorCodes.RESOURCE_NOT_FOUND);

    TestClient blankId = testClient();
    expectAdminToken(blankId.server());
    blankId
        .server()
        .expect(requestTo(PROPERTIES.adminUsersUri() + "?username=fls.rm&exact=true"))
        .andRespond(withSuccess("[{\"id\":\"\"}]", MediaType.APPLICATION_JSON));

    assertThatThrownBy(() -> blankId.adapter().replacePassword("fls.rm", "Zebra#99"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("missing");
  }

  private static void expectTokens(MockRestServiceServer server) {
    server
        .expect(requestTo(PROPERTIES.tokenUri()))
        .andExpect(method(HttpMethod.POST))
        .andRespond(
            withSuccess(
                """
                {"access_token":"access-1","refresh_token":"refresh-1","id_token":"id-1","expires_in":300}
                """,
                MediaType.APPLICATION_JSON));
  }

  private static Jwt jwt(String subject, String audience) {
    return Jwt.withTokenValue("id-1")
        .header("alg", "RS256")
        .subject(subject)
        .audience(List.of(audience))
        .claim("nonce", "nonce-1")
        .issuedAt(Instant.now().minusSeconds(5))
        .expiresAt(Instant.now().plusSeconds(300))
        .build();
  }

  private static IdentityProviderPort.ProvisionIdentityCommand partnerCommand() {
    return new IdentityProviderPort.ProvisionIdentityCommand(
        "business-1", "partner.demo", "partner@example.test", "Partner", "Demo", "ICICI_PRU", true);
  }

  private static void expectAdminToken(MockRestServiceServer server) {
    server
        .expect(requestTo(PROPERTIES.tokenUri()))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withSuccess("{\"access_token\":\"admin-access\"}", MediaType.APPLICATION_JSON));
  }

  private static TestClient testClient() {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    JwtDecoder decoder = mock(JwtDecoder.class);
    Jwt jwt =
        Jwt.withTokenValue("id-1")
            .header("alg", "RS256")
            .subject("subject-1")
            .audience(List.of("workforce-bff"))
            .claim("nonce", "nonce-1")
            .issuedAt(Instant.now().minusSeconds(5))
            .expiresAt(Instant.now().plusSeconds(300))
            .build();
    when(decoder.decode("id-1")).thenReturn(jwt);
    return new TestClient(
        server, new KeycloakIdentityProviderAdapter(PROPERTIES, builder.build(), decoder, ERRORS));
  }

  private record TestClient(
      MockRestServiceServer server, KeycloakIdentityProviderAdapter adapter) {}
}
