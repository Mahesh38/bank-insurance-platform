package com.bank.platform.lead.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@Tag("FUNC-030")
class LeadControllerTest {

  @Autowired MockMvc mockMvc;

  @Autowired ObjectMapper objectMapper;

  @Test
  void createOnboardAndAssignHappyPath() throws Exception {
    MvcResult created =
        mockMvc
            .perform(
                post("/internal/v1/leads")
                    .header("X-Actor-Id", "rm-1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"customerId\":\"cust-ok\",\"lob\":\"LIFE\",\"productClass\":\"TERM\",\"branchId\":\"BR-1\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.state").value("NEW"))
            .andExpect(jsonPath("$.dashboardStage").value("NEW"))
            .andExpect(jsonPath("$.exceptionOutcome").value("NOT_EVALUATED"))
            .andReturn();
    String leadId =
        objectMapper.readTree(created.getResponse().getContentAsString()).get("leadId").asText();
    assertThat(leadId).hasSize(26);

    mockMvc
        .perform(post("/internal/v1/leads/" + leadId + "/onboarding").header("X-Actor-Id", "rm-1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.exceptionOutcome").value("PASS"));

    mockMvc
        .perform(
            post("/internal/v1/leads/" + leadId + "/assignments")
                .header("X-Actor-Id", "rm-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"assignedRmId\":\"SP-1001\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.state").value("ASSIGNED"))
        .andExpect(jsonPath("$.exceptionRequired").value(false));

    mockMvc
        .perform(get("/internal/v1/leads/" + leadId).header("X-Actor-Id", "rm-1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.journeyId").value(leadId));

    mockMvc
        .perform(
            get("/internal/v1/leads")
                .header("X-Actor-Id", "rm-1")
                .param("owner", "me")
                .param("customerId", "cust-ok")
                .param("productClass", "TERM")
                .param("unfinished", "true"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].leadId").value(leadId));
  }
}
