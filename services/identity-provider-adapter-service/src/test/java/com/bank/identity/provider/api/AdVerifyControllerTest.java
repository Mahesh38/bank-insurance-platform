package com.bank.identity.provider.api;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Tag("IAM-001")
@Tag("integration")
class AdVerifyControllerTest {

  @Autowired private MockMvc mockMvc;

  @Test
  void stubVerifyReturnsTrueForActiveBankEmployee() throws Exception {
    mockMvc
        .perform(
            post("/internal/v1/auth/ad-verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"employeeId":"EMP001","password":"local-stub-password"}
                    """))
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
            post("/internal/v1/auth/ad-verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"employeeId":"EMP002","password":"local-stub-password"}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.authenticated").value(false))
        .andExpect(jsonPath("$.accountActive").value(false))
        .andExpect(jsonPath("$.email").value(nullValue()));

    mockMvc
        .perform(
            post("/internal/v1/auth/ad-verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"employeeId":"EMP001","password":"nope"}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.authenticated").value(false));
  }
}
