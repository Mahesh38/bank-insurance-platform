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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * HTTP matrix for Login BRD v3.1 after {@code SUG-20261028-1lg}: bank RM, insurance RM, captcha,
 * password, OTP, unlock and forgot/reset. Downstream authz/adapter are WireMock stubs.
 */
@SpringBootTest
@AutoConfigureMockMvc
class LoginJourneyIT {

  private static final WireMockServer ADAPTER = new WireMockServer(wireMockConfig().dynamicPort());
  private static final WireMockServer AUTHZ = new WireMockServer(wireMockConfig().dynamicPort());
  private static final String BANK_ACCOUNT =
      """
      {"found":true,"route":"READY","businessUserId":"39c1b574-df9e-43b4-bca8-38400f3d3410","status":"ACTIVE","policyVersion":1,"userType":"BANK_EMPLOYEE","insurerCode":null,"firstName":"Ro","username":"E100","maskedMobile":"xx1234","maskedEmail":"ro••••@aubank.in","attemptsRemaining":3,"firstLogin":false}
      """;
  private static final String INSURANCE_ACCOUNT =
      """
      {"found":true,"route":"READY","businessUserId":"a1b2c3d4-e5f6-7890-abcd-ef1234567890","status":"ACTIVE","policyVersion":2,"userType":"INSURER_REPRESENTATIVE","insurerCode":"ICICI","firstName":"Pri","username":"fls.rm@example.test","maskedMobile":"xx5678","maskedEmail":"fl••••@example.test","attemptsRemaining":3,"firstLogin":false}
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
    registry.add("workforce.login.otp-code", () -> "482913");
    registry.add("workforce.login.resend-delay", () -> "PT0S");
  }

  @Autowired private MockMvc mockMvc;

  @Test
  void browserOriginIsIgnoredWhenCorsIsUnset() throws Exception {
    mockMvc
        .perform(get("/api/v1/auth/csrf").header(HttpHeaders.ORIGIN, "http://localhost:3000"))
        .andExpect(status().isOk())
        .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
  }

  @BeforeEach
  void resetStubs() {
    ADAPTER.resetAll();
    AUTHZ.resetAll();
  }

  @Nested
  class Captcha {

    @Test
    void loginWithoutCaptchaFieldsIsValidationError() throws Exception {
      mockMvc
          .perform(
              post("/api/v1/auth/login")
                  .with(csrf())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      """
                      {"clientType":"WEB","userType":"BANK_RM","loginId":"E100","password":"secret"}
                      """))
          .andExpect(status().isUnprocessableEntity())
          .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
          .andExpect(cookie().doesNotExist("WORKFORCE_SESSION"));
      ADAPTER.verify(exactly(0), postRequestedFor(urlEqualTo("/internal/v1/auth/ad-verify")));
    }

    @Test
    void wrongCaptchaWithRightPasswordDoesNotCallDirectoryOrAccount() throws Exception {
      String[] captcha = captcha();
      mockMvc
          .perform(
              post("/api/v1/auth/login")
                  .with(csrf())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      """
                      {"clientType":"WEB","userType":"INSURANCE_RM","loginId":"fls.rm@example.test","password":"Horse#99","captchaId":"%s","captchaValue":"WRONG1"}
                      """
                          .formatted(captcha[0])))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.outcome").value("MESSAGE"))
          .andExpect(jsonPath("$.code").value("VAL-004"))
          .andExpect(jsonPath("$.message").value("Wrong captcha. Try again."))
          .andExpect(cookie().doesNotExist("WORKFORCE_SESSION"));
      ADAPTER.verify(exactly(0), postRequestedFor(urlEqualTo("/internal/v1/auth/ad-verify")));
      AUTHZ.verify(exactly(0), postRequestedFor(urlEqualTo("/internal/v1/login/preview")));
      AUTHZ.verify(
          exactly(0), postRequestedFor(urlEqualTo("/internal/v1/login/verify-platform-password")));
    }

    @Test
    void consumedCaptchaCannotBeReusedEvenWithTheRightValue() throws Exception {
      stubBankReady(true);
      stubAd(true, true);
      String[] captcha = captcha();
      mockMvc
          .perform(loginJson("BANK_RM", "E100", "network-secret", captcha))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.outcome").value("OTP_SENT"));
      mockMvc
          .perform(loginJson("BANK_RM", "E100", "network-secret", captcha))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.outcome").value("MESSAGE"))
          .andExpect(jsonPath("$.code").value("VAL-004"))
          .andExpect(cookie().doesNotExist("WORKFORCE_SESSION"));
      ADAPTER.verify(exactly(1), postRequestedFor(urlEqualTo("/internal/v1/auth/ad-verify")));
    }
  }

  @Nested
  class BankRm {

    @Test
    void happyPathCaptchaLoginOtpIssuesSessionCookie() throws Exception {
      stubBankReady(true);
      stubAd(true, true);
      String[] captcha = captcha();
      MvcResult signIn =
          mockMvc
              .perform(loginJson("BANK_RM", "E100", "network-secret", captcha))
              .andExpect(status().isOk())
              .andExpect(jsonPath("$.outcome").value("OTP_SENT"))
              .andExpect(jsonPath("$.maskedMobile").value("xx1234"))
              .andExpect(jsonPath("$.sessionHandle").doesNotExist())
              .andExpect(cookie().doesNotExist("WORKFORCE_SESSION"))
              .andReturn();
      assertAbsent(signIn, "482913");
      assertAbsent(signIn, "network-secret");

      String challengeId =
          JsonPath.read(signIn.getResponse().getContentAsString(), "$.challengeId");
      MvcResult verified =
          mockMvc
              .perform(otpJson(challengeId, "482913"))
              .andExpect(status().isOk())
              .andExpect(jsonPath("$.outcome").value("AUTHENTICATED"))
              .andExpect(jsonPath("$.title").value("Welcome back, Ro"))
              .andExpect(jsonPath("$.authenticated").value(true))
              .andExpect(cookie().httpOnly("WORKFORCE_SESSION", true))
              .andExpect(cookie().secure("WORKFORCE_SESSION", true))
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
    void wrongPasswordReturnsDesignMessageWithoutSession() throws Exception {
      stubBankReady(false);
      stubAd(false, true);
      String[] captcha = captcha();
      mockMvc
          .perform(loginJson("BANK_RM", "E100", "wrong-password", captcha))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.outcome").value("MESSAGE"))
          .andExpect(jsonPath("$.code").value("VAL-008"))
          .andExpect(cookie().doesNotExist("WORKFORCE_SESSION"));
      AUTHZ.verify(exactly(0), postRequestedFor(urlEqualTo("/internal/v1/login/complete")));
    }

    @Test
    void wrongOtpDoesNotCreateASessionAndCorrectOtpStillCan() throws Exception {
      stubBankReady(true);
      stubAd(true, true);
      String[] captcha = captcha();
      MvcResult signIn =
          mockMvc
              .perform(loginJson("BANK_RM", "E100", "network-secret", captcha))
              .andExpect(jsonPath("$.outcome").value("OTP_SENT"))
              .andReturn();
      String challengeId =
          JsonPath.read(signIn.getResponse().getContentAsString(), "$.challengeId");

      mockMvc
          .perform(otpJson(challengeId, "000000"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.outcome").value("MESSAGE"))
          .andExpect(jsonPath("$.code").value("VAL-014"))
          .andExpect(cookie().doesNotExist("WORKFORCE_SESSION"));
      AUTHZ.verify(exactly(0), postRequestedFor(urlEqualTo("/internal/v1/login/complete")));

      mockMvc
          .perform(otpJson(challengeId, "482913"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.outcome").value("AUTHENTICATED"))
          .andExpect(cookie().exists("WORKFORCE_SESSION"));
      AUTHZ.verify(exactly(1), postRequestedFor(urlEqualTo("/internal/v1/login/complete")));
    }

    @Test
    void unknownEmployeeStopsBeforeDirectoryAndDoesNotIssueOtp() throws Exception {
      AUTHZ.stubFor(
          WireMock.post(urlEqualTo("/internal/v1/login/preview"))
              .willReturn(
                  json(
                      """
                      {"found":false,"route":"UNKNOWN","businessUserId":null,"status":null,"policyVersion":0,"userType":null,"insurerCode":null,"firstName":null,"username":null,"maskedMobile":null,"maskedEmail":null,"attemptsRemaining":3,"firstLogin":false}
                      """)));
      String[] captcha = captcha();
      mockMvc
          .perform(loginJson("BANK_RM", "E404", "network-secret", captcha))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.outcome").value("MESSAGE"))
          .andExpect(jsonPath("$.code").value("VAL-005"))
          .andExpect(cookie().doesNotExist("WORKFORCE_SESSION"));
      ADAPTER.verify(exactly(0), postRequestedFor(urlEqualTo("/internal/v1/auth/ad-verify")));
    }

    @Test
    void lockedBankAccountWithRightPasswordStillReturnsVal010() throws Exception {
      String locked =
          BANK_ACCOUNT
              .replace("\"route\":\"READY\"", "\"route\":\"LOCKED_PASSWORD\"")
              .replace("\"attemptsRemaining\":3", "\"attemptsRemaining\":0");
      AUTHZ.stubFor(
          WireMock.post(urlEqualTo("/internal/v1/login/preview")).willReturn(json(locked)));
      String[] captcha = captcha();
      mockMvc
          .perform(loginJson("BANK_RM", "E100", "network-secret", captcha))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.outcome").value("MESSAGE"))
          .andExpect(jsonPath("$.code").value("VAL-010"))
          .andExpect(cookie().doesNotExist("WORKFORCE_SESSION"));
      ADAPTER.verify(exactly(0), postRequestedFor(urlEqualTo("/internal/v1/auth/ad-verify")));
    }

    @Test
    void fiveWrongOtpsEndTheChallengeWithoutASession() throws Exception {
      stubBankReady(true);
      stubAd(true, true);
      String[] captcha = captcha();
      MvcResult signIn =
          mockMvc
              .perform(loginJson("BANK_RM", "E100", "network-secret", captcha))
              .andExpect(jsonPath("$.outcome").value("OTP_SENT"))
              .andReturn();
      String challengeId =
          JsonPath.read(signIn.getResponse().getContentAsString(), "$.challengeId");
      for (int i = 0; i < 4; i++) {
        mockMvc
            .perform(otpJson(challengeId, "00000" + i))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("VAL-014"));
      }
      mockMvc
          .perform(otpJson(challengeId, "999999"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.outcome").value("MESSAGE"))
          .andExpect(jsonPath("$.code").value("VAL-016"))
          .andExpect(cookie().doesNotExist("WORKFORCE_SESSION"));
      mockMvc
          .perform(otpJson(challengeId, "482913"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.code").value("VAL-015"))
          .andExpect(cookie().doesNotExist("WORKFORCE_SESSION"));
      AUTHZ.verify(exactly(0), postRequestedFor(urlEqualTo("/internal/v1/login/complete")));
    }

    @Test
    void otpResendIssuesANewChallengeAndStillAuthenticates() throws Exception {
      stubBankReady(true);
      stubAd(true, true);
      String[] captcha = captcha();
      MvcResult signIn =
          mockMvc
              .perform(loginJson("BANK_RM", "E100", "network-secret", captcha))
              .andExpect(jsonPath("$.outcome").value("OTP_SENT"))
              .andReturn();
      String challengeId =
          JsonPath.read(signIn.getResponse().getContentAsString(), "$.challengeId");
      mockMvc
          .perform(
              post("/api/v1/auth/otp/resend")
                  .with(csrf())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{\"challengeId\":\"%s\"}".formatted(challengeId)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.outcome").value("OTP_SENT"))
          .andExpect(jsonPath("$.challengeId").value(challengeId));
      mockMvc
          .perform(otpJson(challengeId, "482913"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.outcome").value("AUTHENTICATED"))
          .andExpect(cookie().exists("WORKFORCE_SESSION"));
    }
  }

  @Nested
  class InsuranceRm {

    @Test
    void happyPathUsesPlatformPasswordNotAdVerify() throws Exception {
      stubInsuranceReady(true);
      String[] captcha = captcha();
      MvcResult signIn =
          mockMvc
              .perform(loginJson("INSURANCE_RM", "fls.rm@example.test", "Horse#99", captcha))
              .andExpect(status().isOk())
              .andExpect(jsonPath("$.outcome").value("OTP_SENT"))
              .andExpect(jsonPath("$.maskedEmail").value("fl••••@example.test"))
              .andExpect(cookie().doesNotExist("WORKFORCE_SESSION"))
              .andReturn();
      ADAPTER.verify(exactly(0), postRequestedFor(urlEqualTo("/internal/v1/auth/ad-verify")));
      AUTHZ.verify(
          exactly(1), postRequestedFor(urlEqualTo("/internal/v1/login/verify-platform-password")));

      String challengeId =
          JsonPath.read(signIn.getResponse().getContentAsString(), "$.challengeId");
      mockMvc
          .perform(otpJson(challengeId, "482913"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.outcome").value("AUTHENTICATED"))
          .andExpect(cookie().httpOnly("WORKFORCE_SESSION", true));
    }

    @Test
    void wrongPasswordDoesNotIssueOtp() throws Exception {
      stubInsuranceReady(false);
      String[] captcha = captcha();
      mockMvc
          .perform(loginJson("INSURANCE_RM", "fls.rm@example.test", "Wrong#1", captcha))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.outcome").value("MESSAGE"))
          .andExpect(jsonPath("$.code").value("VAL-008"))
          .andExpect(cookie().doesNotExist("WORKFORCE_SESSION"));
      AUTHZ.verify(exactly(0), postRequestedFor(urlEqualTo("/internal/v1/login/complete")));
    }

    @Test
    void unknownPartnerEmailStopsWithoutPlatformPasswordCheck() throws Exception {
      AUTHZ.stubFor(
          WireMock.post(urlEqualTo("/internal/v1/login/preview"))
              .willReturn(
                  json(
                      """
                      {"found":false,"route":"UNKNOWN","businessUserId":null,"status":null,"policyVersion":0,"userType":null,"insurerCode":null,"firstName":null,"username":null,"maskedMobile":null,"maskedEmail":null,"attemptsRemaining":3,"firstLogin":false}
                      """)));
      String[] captcha = captcha();
      mockMvc
          .perform(loginJson("INSURANCE_RM", "missing@example.test", "Horse#99", captcha))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.outcome").value("MESSAGE"))
          .andExpect(jsonPath("$.code").value("VAL-006"))
          .andExpect(cookie().doesNotExist("WORKFORCE_SESSION"));
      AUTHZ.verify(
          exactly(0), postRequestedFor(urlEqualTo("/internal/v1/login/verify-platform-password")));
      ADAPTER.verify(exactly(0), postRequestedFor(urlEqualTo("/internal/v1/auth/ad-verify")));
    }

    @Test
    void forgotPasswordOtpThenSetPasswordDoesNotLogIn() throws Exception {
      AUTHZ.stubFor(
          WireMock.post(urlEqualTo("/internal/v1/login/forgot/assess"))
              .willReturn(
                  json(INSURANCE_ACCOUNT.replace("\"route\":\"READY\"", "\"route\":\"OK\""))));
      AUTHZ.stubFor(
          WireMock.post(urlEqualTo("/internal/v1/login/password"))
              .willReturn(
                  aResponse()
                      .withHeader("Content-Type", "application/json")
                      .withBody("{\"result\":\"SET\",\"violation\":null}")));

      String[] captcha = captcha();
      MvcResult forgot =
          mockMvc
              .perform(
                  post("/api/v1/auth/forgot-password")
                      .with(csrf())
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(
                          """
                          {"clientType":"WEB","email":"fls.rm@example.test","captchaId":"%s","captchaValue":"%s"}
                          """
                              .formatted(captcha[0], captcha[1])))
              .andExpect(status().isOk())
              .andExpect(jsonPath("$.outcome").value("OTP_SENT"))
              .andExpect(cookie().doesNotExist("WORKFORCE_SESSION"))
              .andReturn();
      String challengeId =
          JsonPath.read(forgot.getResponse().getContentAsString(), "$.challengeId");

      MvcResult verified =
          mockMvc
              .perform(otpJson(challengeId, "482913"))
              .andExpect(status().isOk())
              .andExpect(jsonPath("$.outcome").value("CONTINUATION"))
              .andExpect(jsonPath("$.purpose").value("FORGOT"))
              .andExpect(cookie().doesNotExist("WORKFORCE_SESSION"))
              .andReturn();
      String continuationToken =
          JsonPath.read(verified.getResponse().getContentAsString(), "$.continuationToken");

      mockMvc
          .perform(
              post("/api/v1/auth/password")
                  .with(csrf())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      """
                      {"resetToken":"%s","newPassword":"Fresh#99x","confirmPassword":"Fresh#99x"}
                      """
                          .formatted(continuationToken)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.outcome").value("PASSWORD_SET"))
          .andExpect(cookie().doesNotExist("WORKFORCE_SESSION"));
      AUTHZ.verify(exactly(0), postRequestedFor(urlEqualTo("/internal/v1/login/complete")));
    }
  }

  @Nested
  class Unlock {

    @Test
    void unlockOtpDoesNotCreateAWorkforceSession() throws Exception {
      String unlockAccount = BANK_ACCOUNT.replace("\"route\":\"READY\"", "\"route\":\"UNLOCK_OK\"");
      AUTHZ.stubFor(
          WireMock.post(urlEqualTo("/internal/v1/login/unlock/assess"))
              .willReturn(json(unlockAccount)));
      String activated = BANK_ACCOUNT.replace("\"route\":\"READY\"", "\"route\":\"ACTIVATED\"");
      AUTHZ.stubFor(
          WireMock.post(urlEqualTo("/internal/v1/login/unlock/complete"))
              .willReturn(json(activated)));

      String[] captcha = captcha();
      MvcResult unlock =
          mockMvc
              .perform(
                  post("/api/v1/auth/unlock")
                      .with(csrf())
                      .contentType(MediaType.APPLICATION_JSON)
                      .content(
                          """
                          {"clientType":"WEB","userType":"BANK_RM","loginId":"E100","captchaId":"%s","captchaValue":"%s"}
                          """
                              .formatted(captcha[0], captcha[1])))
              .andExpect(status().isOk())
              .andExpect(jsonPath("$.outcome").value("OTP_SENT"))
              .andReturn();
      String challengeId =
          JsonPath.read(unlock.getResponse().getContentAsString(), "$.challengeId");
      mockMvc
          .perform(otpJson(challengeId, "482913"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.outcome").value("COMPLETED"))
          .andExpect(jsonPath("$.title").value("Account activated"))
          .andExpect(cookie().doesNotExist("WORKFORCE_SESSION"));
      AUTHZ.verify(exactly(0), postRequestedFor(urlEqualTo("/internal/v1/login/complete")));
    }
  }

  private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder loginJson(
      String userType, String loginId, String password, String[] captcha) {
    return post("/api/v1/auth/login")
        .with(csrf())
        .contentType(MediaType.APPLICATION_JSON)
        .content(
            """
            {"clientType":"WEB","userType":"%s","loginId":"%s","password":"%s","captchaId":"%s","captchaValue":"%s"}
            """
                .formatted(userType, loginId, password, captcha[0], captcha[1]));
  }

  private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder otpJson(
      String challengeId, String otp) {
    return post("/api/v1/auth/otp/verify")
        .with(csrf())
        .contentType(MediaType.APPLICATION_JSON)
        .content(
            """
            {"challengeId":"%s","otp":"%s"}
            """
                .formatted(challengeId, otp));
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

  private static void stubBankReady(boolean passwordAccepted) {
    AUTHZ.stubFor(
        WireMock.post(urlEqualTo("/internal/v1/login/preview")).willReturn(json(BANK_ACCOUNT)));
    String rejected =
        BANK_ACCOUNT
            .replace("\"route\":\"READY\"", "\"route\":\"PASSWORD_REJECTED\"")
            .replace("\"attemptsRemaining\":3", "\"attemptsRemaining\":2");
    AUTHZ.stubFor(
        WireMock.post(urlEqualTo("/internal/v1/login/bank-password-result"))
            .willReturn(
                json(
                    """
                    {"account":%s,"accepted":%s}
                    """
                        .formatted(passwordAccepted ? BANK_ACCOUNT : rejected, passwordAccepted))));
    AUTHZ.stubFor(
        WireMock.post(urlEqualTo("/internal/v1/login/complete")).willReturn(json(BANK_ACCOUNT)));
  }

  private static void stubInsuranceReady(boolean passwordAccepted) {
    AUTHZ.stubFor(
        WireMock.post(urlEqualTo("/internal/v1/login/preview"))
            .willReturn(json(INSURANCE_ACCOUNT)));
    String rejected =
        INSURANCE_ACCOUNT
            .replace("\"route\":\"READY\"", "\"route\":\"PASSWORD_REJECTED\"")
            .replace("\"attemptsRemaining\":3", "\"attemptsRemaining\":2");
    AUTHZ.stubFor(
        WireMock.post(urlEqualTo("/internal/v1/login/verify-platform-password"))
            .willReturn(
                json(
                    """
                    {"account":%s,"accepted":%s}
                    """
                        .formatted(
                            passwordAccepted ? INSURANCE_ACCOUNT : rejected, passwordAccepted))));
    AUTHZ.stubFor(
        WireMock.post(urlEqualTo("/internal/v1/login/complete"))
            .willReturn(json(INSURANCE_ACCOUNT)));
  }

  private static void stubAd(boolean authenticated, boolean accountActive) {
    ADAPTER.stubFor(
        WireMock.post(urlEqualTo("/internal/v1/auth/ad-verify"))
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        """
                        {"authenticated":%s,"accountActive":%s,"employeeId":"E100","username":"E100","email":"e100@aubank.in"}
                        """
                            .formatted(authenticated, accountActive))));
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
