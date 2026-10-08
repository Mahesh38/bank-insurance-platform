package com.bank.workforce.bff.api;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.test.web.servlet.MvcResult;

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
  void bankEmployeeLoginIssuesHttpOnlySessionAndSessionEndpointReadsIt() throws Exception {
    stubAd(true, true);
    stubResolve("ACTIVE", "BANK_EMPLOYEE");

    MvcResult login =
        mockMvc
            .perform(
                post("/api/v1/auth/login")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                    {"clientType":"WEB","identitySource":"BANK_AD","employeeId":"EMP001","password":"local-stub-password"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.authenticated").value(true))
            .andExpect(jsonPath("$.userType").value("BANK_EMPLOYEE"))
            .andExpect(jsonPath("$.status").value("ACTIVE"))
            .andExpect(jsonPath("$.sessionHandle").value(nullValue()))
            .andExpect(jsonPath("$.password").doesNotExist())
            .andExpect(cookie().exists("WORKFORCE_SESSION"))
            .andExpect(cookie().httpOnly("WORKFORCE_SESSION", true))
            .andReturn();

    String sessionCookie = login.getResponse().getCookie("WORKFORCE_SESSION").getValue();
    mockMvc
        .perform(
            get("/api/v1/auth/session")
                .cookie(new jakarta.servlet.http.Cookie("WORKFORCE_SESSION", sessionCookie)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.userType").value("BANK_EMPLOYEE"))
        .andExpect(jsonPath("$.status").value("ACTIVE"))
        .andExpect(jsonPath("$.username").value("EMP001"));

    ADAPTER.verify(
        postRequestedFor(urlEqualTo("/internal/v1/auth/ad-verify"))
            .withRequestBody(
                equalToJson(
                    """
                {"employeeId":"EMP001","password":"local-stub-password"}
                """)));
    AUTHZ.verify(
        postRequestedFor(urlEqualTo("/internal/v1/identities/resolve"))
            .withRequestBody(containing("BANK_AD"))
            .withRequestBody(containing("BANK_EMPLOYEE")));
  }

  @Test
  void falseFromAdAndInactiveIdentityReturnTheSameGenericUnauthorized() throws Exception {
    stubAd(false, false);

    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"clientType":"WEB","identitySource":"BANK_AD","employeeId":"EMP001","password":"wrong"}
                    """))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"))
        .andExpect(jsonPath("$.detail").value("The sign-in could not be completed."))
        .andExpect(cookie().doesNotExist("WORKFORCE_SESSION"));

    AUTHZ.verify(exactly(0), postRequestedFor(urlEqualTo("/internal/v1/identities/resolve")));

    ADAPTER.resetAll();
    AUTHZ.resetAll();
    stubAd(true, true);
    stubResolve("SUSPENDED", "BANK_EMPLOYEE");

    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"clientType":"WEB","identitySource":"BANK_AD","employeeId":"EMP001","password":"local-stub-password"}
                    """))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"))
        .andExpect(jsonPath("$.detail").value("The sign-in could not be completed."));
  }

  @Test
  void nativeBankLoginReturnsOpaqueHandleAndNeverAnOauthToken() throws Exception {
    stubAd(true, true);
    stubResolve("ACTIVE", "BANK_EMPLOYEE");

    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"clientType":"NATIVE","identitySource":"BANK_AD","employeeId":"EMP001","password":"local-stub-password"}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.authenticated").value(true))
        .andExpect(jsonPath("$.sessionHandle").isNotEmpty())
        .andExpect(jsonPath("$.accessToken").doesNotExist())
        .andExpect(jsonPath("$.refreshToken").doesNotExist())
        .andExpect(cookie().doesNotExist("WORKFORCE_SESSION"));
  }

  @Test
  void missingEmployeeIdIsValidationNotAuthentication() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"clientType":"WEB","identitySource":"BANK_AD","password":"local-stub-password"}
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("SCHEMA_INVALID"));
    ADAPTER.verify(exactly(0), postRequestedFor(urlEqualTo("/internal/v1/auth/ad-verify")));
  }

  @Test
  void partnerLoginStillStartsOidcAndDoesNotCallAdVerify() throws Exception {
    ADAPTER.stubFor(
        WireMock.post(urlEqualTo("/internal/v1/auth/authorization-uri"))
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"authorizationUri\":\"https://idp.example.test/authorize\"}")));

    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"clientType":"WEB","identitySource":"PARTNER","returnUri":"http://localhost:3000/auth/complete"}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.authorizationUri").value("https://idp.example.test/authorize"));

    ADAPTER.verify(exactly(0), postRequestedFor(urlEqualTo("/internal/v1/auth/ad-verify")));
  }

  private static void stubAd(boolean authenticated, boolean accountActive) {
    ADAPTER.stubFor(
        WireMock.post(urlEqualTo("/internal/v1/auth/ad-verify"))
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        """
                {"authenticated":%s,"accountActive":%s,"employeeId":"EMP001","username":"EMP001","email":"emp001@example.test"}
                """
                            .formatted(authenticated, accountActive))));
  }

  private static void stubResolve(String status, String userType) {
    AUTHZ.stubFor(
        WireMock.post(urlEqualTo("/internal/v1/identities/resolve"))
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        """
                {"businessUserId":"39c1b574-df9e-43b4-bca8-38400f3d3410","status":"%s","policyVersion":1,"userType":"%s","insurerCode":null}
                """
                            .formatted(status, userType))));
  }
}
