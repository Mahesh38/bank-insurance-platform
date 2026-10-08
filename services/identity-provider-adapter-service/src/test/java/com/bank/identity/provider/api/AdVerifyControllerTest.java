package com.bank.identity.provider.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bank.identity.provider.config.InternalAuthProperties;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest
@AutoConfigureMockMvc
@Tag("IAM-001")
@Tag("integration")
class AdVerifyControllerTest {

  private static final String INTERNAL_KEY = "local-idp-adapter-internal-key";
  private static final String ACTIVE_BODY =
      """
      {"employeeId":"EMP001","password":"local-stub-password"}
      """;

  @Autowired private MockMvc mockMvc;

  @Test
  void stubVerifyReturnsTrueForActiveBankEmployee() throws Exception {
    mockMvc
        .perform(adVerify(ACTIVE_BODY))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.authenticated").value(true))
        .andExpect(jsonPath("$.accountActive").value(true))
        .andExpect(jsonPath("$.employeeId").value("EMP001"))
        .andExpect(jsonPath("$.password").doesNotExist());
  }

  @Test
  void stubVerifyReturnsFalseForInactiveOrWrongPassword() throws Exception {
    mockMvc
        .perform(
            adVerify(
                """
                {"employeeId":"EMP002","password":"local-stub-password"}
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.authenticated").value(false))
        .andExpect(jsonPath("$.accountActive").value(false))
        .andExpect(jsonPath("$.email").value(nullValue()));

    mockMvc
        .perform(
            adVerify(
                """
                {"employeeId":"EMP001","password":"nope"}
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.authenticated").value(false));
  }

  @Test
  void missingInternalKeyIsUnauthorizedAndDoesNotEchoPassword() throws Exception {
    mockMvc
        .perform(
            post("/internal/v1/auth/ad-verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(ACTIVE_BODY))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
        .andExpect(jsonPath("$.password").doesNotExist())
        .andExpect(jsonPath("$.detail").value("Please sign in and try again."))
        .andExpect(content().string(not(containsString("local-stub-password"))));
  }

  @Test
  void wrongInternalKeyIsUnauthorized() throws Exception {
    mockMvc
        .perform(
            post("/internal/v1/auth/ad-verify")
                .header(InternalAuthProperties.HEADER, "wrong-wrong-wrong-x")
                .contentType(MediaType.APPLICATION_JSON)
                .content(ACTIVE_BODY))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
        .andExpect(jsonPath("$.password").doesNotExist());
  }

  @Test
  void tokenExchangeAlsoRequiresInternalKey() throws Exception {
    mockMvc
        .perform(
            post("/internal/v1/auth/token-exchange")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"code":"x","codeVerifier":"y","expectedNonce":"z"}
                    """))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
  }

  @Test
  void livenessDoesNotRequireInternalKey() throws Exception {
    mockMvc.perform(get("/actuator/health/liveness")).andExpect(status().isOk());
  }

  private static MockHttpServletRequestBuilder adVerify(String body) {
    return post("/internal/v1/auth/ad-verify")
        .header(InternalAuthProperties.HEADER, INTERNAL_KEY)
        .contentType(MediaType.APPLICATION_JSON)
        .content(body);
  }
}
