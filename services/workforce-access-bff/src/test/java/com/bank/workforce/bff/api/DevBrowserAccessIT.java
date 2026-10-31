package com.bank.workforce.bff.api;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Dev-profile browser access ({@code SUG-20261031-dvc}): a listed origin receives credentialed CORS
 * headers, and the session cookie is stored without {@code Secure}.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class DevBrowserAccessIT {

  private static final WireMockServer ADAPTER = new WireMockServer(wireMockConfig().dynamicPort());
  private static final WireMockServer AUTHZ = new WireMockServer(wireMockConfig().dynamicPort());
  private static final String BANK_ACCOUNT =
      """
      {"found":true,"route":"READY","businessUserId":"39c1b574-df9e-43b4-bca8-38400f3d3410","status":"ACTIVE","policyVersion":1,"userType":"BANK_EMPLOYEE","insurerCode":null,"firstName":"Ro","username":"E100","maskedMobile":"xx1234","maskedEmail":"ro••••@aubank.in","attemptsRemaining":3,"firstLogin":false}
      """;

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
  static void bind(DynamicPropertyRegistry registry) {
    registry.add("workforce.downstream.provider-adapter-base-url", ADAPTER::baseUrl);
    registry.add("workforce.downstream.authorization-service-base-url", AUTHZ::baseUrl);
  }

  @Autowired private MockMvc mockMvc;

  @Test
  void listedOriginReceivesCredentialedCorsHeaders() throws Exception {
    mockMvc
        .perform(
            options("/api/v1/auth/csrf")
                .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "X-XSRF-TOKEN,Content-Type"))
        .andExpect(status().isOk())
        .andExpect(
            header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000"))
        .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
  }

  @Test
  void aForeignOriginIsRejected() throws Exception {
    mockMvc
        .perform(get("/api/v1/auth/csrf").header(HttpHeaders.ORIGIN, "http://evil.example"))
        .andExpect(status().isForbidden())
        .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
  }

  @Test
  void sessionCookieOnHttpOmitsSecure() throws Exception {
    stubBank();
    MvcResult captcha =
        mockMvc
            .perform(post("/api/v1/auth/captcha").with(csrf()))
            .andExpect(status().isOk())
            .andReturn();
    String captchaId = JsonPath.read(captcha.getResponse().getContentAsString(), "$.captchaId");
    String display = JsonPath.read(captcha.getResponse().getContentAsString(), "$.display");
    MvcResult signIn =
        mockMvc
            .perform(
                post("/api/v1/auth/login")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"clientType":"WEB","userType":"BANK_RM","loginId":"E100","password":"network-secret","captchaId":"%s","captchaValue":"%s"}
                        """
                            .formatted(captchaId, display)))
            .andExpect(status().isOk())
            .andReturn();
    String challengeId = JsonPath.read(signIn.getResponse().getContentAsString(), "$.challengeId");

    mockMvc
        .perform(
            post("/api/v1/auth/otp/verify")
                .with(csrf())
                .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"challengeId":"%s","otp":"482913"}
                    """
                        .formatted(challengeId)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.outcome").value("AUTHENTICATED"))
        .andExpect(cookie().httpOnly("WORKFORCE_SESSION", true))
        .andExpect(cookie().secure("WORKFORCE_SESSION", false))
        .andExpect(
            header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000"))
        .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
  }

  private static void stubBank() {
    AUTHZ.stubFor(
        WireMock.post(urlEqualTo("/internal/v1/login/preview"))
            .willReturn(
                aResponse().withHeader("Content-Type", "application/json").withBody(BANK_ACCOUNT)));
    AUTHZ.stubFor(
        WireMock.post(urlEqualTo("/internal/v1/login/bank-password-result"))
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        """
                        {"account":%s,"accepted":true}
                        """
                            .formatted(BANK_ACCOUNT))));
    AUTHZ.stubFor(
        WireMock.post(urlEqualTo("/internal/v1/login/complete"))
            .willReturn(
                aResponse().withHeader("Content-Type", "application/json").withBody(BANK_ACCOUNT)));
    ADAPTER.stubFor(
        WireMock.post(urlEqualTo("/internal/v1/auth/ad-verify"))
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        """
                        {"authenticated":true,"accountActive":true,"employeeId":"E100","username":"E100","email":"e100@aubank.in"}
                        """)));
  }
}
