package com.bank.workforce.bff.api;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bank.workforce.bff.config.DownstreamProperties;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Contract after {@code SUG-20261028-1lg}: IAM-001 immediate-session {@code /login} is gone; Login
 * BRD owns {@code /login}; partner OIDC begins at {@code /partner/authorization}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Tag("IAM-001")
@Tag("integration")
class BankLoginIT {

  private static final WireMockServer ADAPTER = new WireMockServer(wireMockConfig().dynamicPort());
  private static final WireMockServer AUTHZ = new WireMockServer(wireMockConfig().dynamicPort());

  static {
    ADAPTER.start();
    AUTHZ.start();
  }

  @AfterAll
  static void stopWireMocks() {
    ADAPTER.stop();
    AUTHZ.stop();
  }

  @DynamicPropertySource
  static void bindWireMockBaseUrls(DynamicPropertyRegistry registry) {
    registry.add("workforce.downstream.provider-adapter-base-url", ADAPTER::baseUrl);
    registry.add("workforce.downstream.authorization-service-base-url", AUTHZ::baseUrl);
  }

  @Autowired private MockMvc mockMvc;

  @BeforeEach
  void resetStubs() {
    ADAPTER.resetAll();
    AUTHZ.resetAll();
  }

  @Test
  void legacyBankAdImmediateSessionBodyIsRejectedAndNeverCallsAdVerify() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"clientType":"WEB","identitySource":"BANK_AD","employeeId":"EMP001","password":"local-stub-password"}
                    """))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(cookie().doesNotExist("WORKFORCE_SESSION"));

    ADAPTER.verify(exactly(0), postRequestedFor(urlEqualTo("/internal/v1/auth/ad-verify")));
    AUTHZ.verify(exactly(0), postRequestedFor(urlEqualTo("/internal/v1/identities/resolve")));
  }

  @Test
  void legacyPartnerBodyOnLoginIsRejected() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"clientType":"WEB","identitySource":"PARTNER","returnUri":"http://localhost:3000/auth/complete"}
                    """))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(cookie().doesNotExist("WORKFORCE_SESSION"));

    ADAPTER.verify(exactly(0), postRequestedFor(urlEqualTo("/internal/v1/auth/authorization-uri")));
  }

  @Test
  void retiredSignInPathIsNotFound() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/sign-in")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"clientType":"WEB","userType":"BANK_RM","loginId":"E100","password":"x","captchaId":"c","captchaValue":"v"}
                    """))
        .andExpect(status().isNotFound());
  }

  @Test
  void partnerAuthorizationStartsOidcAndDoesNotCallAdVerify() throws Exception {
    ADAPTER.stubFor(
        WireMock.post(urlEqualTo("/internal/v1/auth/authorization-uri"))
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"authorizationUri\":\"https://idp.example.test/authorize\"}")));

    mockMvc
        .perform(
            post("/api/v1/auth/partner/authorization")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"clientType":"WEB","returnUri":"http://localhost:3000/auth/complete"}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.authorizationUri").value("https://idp.example.test/authorize"));

    ADAPTER.verify(exactly(0), postRequestedFor(urlEqualTo("/internal/v1/auth/ad-verify")));
    ADAPTER.verify(
        postRequestedFor(urlEqualTo("/internal/v1/auth/authorization-uri"))
            .withHeader(
                DownstreamProperties.PROVIDER_ADAPTER_INTERNAL_KEY_HEADER,
                equalTo("local-idp-adapter-internal-key")));
  }

  @Test
  void sessionEndpointWithoutOrWithUnknownHandleIsSessionInvalidNotValidation() throws Exception {
    mockMvc
        .perform(get("/api/v1/auth/session"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("SESSION_INVALID"));
    mockMvc
        .perform(
            get("/api/v1/auth/session")
                .cookie(
                    new jakarta.servlet.http.Cookie("WORKFORCE_SESSION", "logged-out-or-unknown")))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("SESSION_INVALID"));
  }
}
