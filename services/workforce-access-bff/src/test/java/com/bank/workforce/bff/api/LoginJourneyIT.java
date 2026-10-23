package com.bank.workforce.bff.api;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
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

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
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
class LoginJourneyIT {

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
  static void bind(DynamicPropertyRegistry registry) {
    registry.add("workforce.downstream.provider-adapter-base-url", ADAPTER::baseUrl);
    registry.add("workforce.downstream.authorization-service-base-url", AUTHZ::baseUrl);
    registry.add("workforce.login.otp-code", () -> "482913");
  }

  @Autowired private MockMvc mockMvc;

  @BeforeEach
  void resetStubs() {
    ADAPTER.resetAll();
    AUTHZ.resetAll();
  }

  @Test
  void bankSignInSendsAnOtpAndOnlyThenIssuesTheSessionCookie() throws Exception {
    stubAccount("READY");
    ADAPTER.stubFor(
        WireMock.post(urlEqualTo("/internal/v1/auth/ad-verify"))
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        """
                        {"authenticated":true,"accountActive":true,"employeeId":"E100","username":"E100","email":"e100@aubank.in"}
                        """)));

    String[] captcha = captcha();
    MvcResult signIn =
        mockMvc
            .perform(
                post("/api/v1/auth/sign-in")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"clientType":"WEB","userType":"BANK_RM","loginId":"E100","password":"network-secret","captchaId":"%s","captchaValue":"%s"}
                        """
                            .formatted(captcha[0], captcha[1])))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.outcome").value("OTP_SENT"))
            .andExpect(jsonPath("$.maskedMobile").value("xx1234"))
            .andExpect(jsonPath("$.sessionHandle").doesNotExist())
            .andExpect(cookie().doesNotExist("WORKFORCE_SESSION"))
            .andReturn();
    assertAbsent(signIn, "482913");
    assertAbsent(signIn, "network-secret");

    String challengeId = JsonPath.read(signIn.getResponse().getContentAsString(), "$.challengeId");
    MvcResult verified =
        mockMvc
            .perform(
                post("/api/v1/auth/otp/verify")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"challengeId":"%s","otp":"482913"}
                        """
                            .formatted(challengeId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.outcome").value("AUTHENTICATED"))
            .andExpect(jsonPath("$.title").value("Welcome back, Ro"))
            .andExpect(jsonPath("$.authenticated").value(true))
            .andExpect(jsonPath("$.sessionHandle").doesNotExist())
            .andExpect(cookie().httpOnly("WORKFORCE_SESSION", true))
            .andReturn();
    assertAbsent(verified, "482913");

    String sessionCookie = verified.getResponse().getCookie("WORKFORCE_SESSION").getValue();
    mockMvc
        .perform(
            get("/api/v1/auth/session")
                .cookie(new jakarta.servlet.http.Cookie("WORKFORCE_SESSION", sessionCookie)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.userType").value("BANK_EMPLOYEE"));

    AUTHZ.verify(exactly(1), postRequestedFor(urlEqualTo("/internal/v1/login/complete")));
    AUTHZ.verify(exactly(0), postRequestedFor(urlEqualTo("/internal/v1/identities/resolve")));
  }

  @Test
  void wrongCaptchaDoesNotCallTheDirectoryOrTheAccount() throws Exception {
    String[] captcha = captcha();
    mockMvc
        .perform(
            post("/api/v1/auth/sign-in")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"clientType":"WEB","userType":"INSURANCE_RM","loginId":"rm@aubank.in","password":"Horse#99","captchaId":"%s","captchaValue":"WRONG1"}
                    """
                        .formatted(captcha[0])))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value("VAL-004"))
        .andExpect(jsonPath("$.message").value("Wrong captcha. Try again."));

    ADAPTER.verify(exactly(0), postRequestedFor(urlEqualTo("/internal/v1/auth/ad-verify")));
    AUTHZ.verify(exactly(0), postRequestedFor(urlEqualTo("/internal/v1/login/preview")));
  }

  private String[] captcha() throws Exception {
    MvcResult result =
        mockMvc
            .perform(post("/api/v1/auth/captcha").with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.outcome").value("CAPTCHA"))
            .andReturn();
    String body = result.getResponse().getContentAsString();
    return new String[] {JsonPath.read(body, "$.captchaId"), JsonPath.read(body, "$.display")};
  }

  private static void stubAccount(String route) {
    String account =
        """
        {"found":true,"route":"%s","businessUserId":"39c1b574-df9e-43b4-bca8-38400f3d3410","status":"ACTIVE","policyVersion":1,"userType":"BANK_EMPLOYEE","insurerCode":null,"firstName":"Ro","username":"E100","maskedMobile":"xx1234","maskedEmail":"ro••••@aubank.in","attemptsRemaining":3,"firstLogin":false}
        """
            .formatted(route);
    AUTHZ.stubFor(
        WireMock.post(urlEqualTo("/internal/v1/login/preview")).willReturn(json(account)));
    AUTHZ.stubFor(
        WireMock.post(urlEqualTo("/internal/v1/login/bank-password-result"))
            .willReturn(
                json(
                    """
                    {"account":%s,"accepted":true}
                    """
                        .formatted(account))));
    AUTHZ.stubFor(
        WireMock.post(urlEqualTo("/internal/v1/login/complete")).willReturn(json(account)));
  }

  private static com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder json(
      String body) {
    return aResponse().withHeader("Content-Type", "application/json").withBody(body);
  }

  private static void assertAbsent(MvcResult result, String secret) throws Exception {
    org.assertj.core.api.Assertions.assertThat(result.getResponse().getContentAsString())
        .doesNotContain(secret);
  }
}
