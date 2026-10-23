package com.bank.platform.lead.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bank.platform.lead.adapter.rules.SampleRuleSets;
import com.bank.platform.lead.domain.LeadAuthorizationPort;
import com.bank.platform.lead.domain.LeadAuthorizationPort.AuthorizationDecision;
import com.bank.platform.lead.domain.LeadExceptionFactsPort;
import com.bank.rules.client.RuleSetResolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * E6 end to end on the engine, JDBC store and H2: a SAMPLE-configured {@code LEAD.EXCEPTION} (age
 * &gt; 60 → APPROVAL_REQUIRED, approver BANK_ADMIN) holds the lead; a partner is refused; a bank
 * admin releases with remarks; assignment proceeds.
 */
@SpringBootTest(
    properties = {"lead.rule-sets.source=test-fixture", "lead.exception-facts=test-fixture"})
@AutoConfigureMockMvc
@Tag("FUNC-030")
class LeadExceptionApprovalFlowTest {

  @TestConfiguration
  static class Fixtures {
    @Bean
    RuleSetResolver sampleRuleSets() {
      return SampleRuleSets.resolverOf(
          SampleRuleSets.configuredLeadException(), SampleRuleSets.seed("LEAD.POLICY"));
    }

    @Bean
    LeadExceptionFactsPort sampleFacts() {
      return subject -> SampleRuleSets.cleanFacts(subject.customerId().contains("old") ? 61 : 40);
    }
  }

  @Autowired MockMvc mockMvc;
  @Autowired ObjectMapper objectMapper;
  @MockitoBean LeadAuthorizationPort authorization;

  @BeforeEach
  void pdpAllows() {
    when(authorization.decide(any(), any(), any(), any()))
        .thenReturn(AuthorizationDecision.allow("ROLE_GRANT"));
  }

  @Test
  void heldLeadIsReleasedByABankAdminOnly() throws Exception {
    String leadId = create("cust-old-1");
    mockMvc
        .perform(post("/internal/v1/leads/" + leadId + "/onboarding").header("X-Actor-Id", "rm-9"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.exceptionOutcome").value("APPROVAL_REQUIRED"))
        .andExpect(jsonPath("$.exceptionHold").value(true));

    decide(leadId, "release", "INSURER_PARTNER_REP", "BANK_ADMIN", "ok")
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    decide(leadId, "release", "BANK_EMPLOYEE", "BANK_ADMIN", "")
        .andExpect(status().isUnprocessableEntity());
    decide(leadId, "release", "BANK_EMPLOYEE", "BRANCH_MANAGER", "ok")
        .andExpect(status().isForbidden());

    decide(leadId, "release", "BANK_EMPLOYEE", "BANK_ADMIN", "Approved: long relationship")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.exceptionHold").value(false))
        .andExpect(jsonPath("$.exceptionOutcome").value("APPROVAL_REQUIRED"));
    decide(leadId, "reject", "BANK_EMPLOYEE", "BANK_ADMIN", "too late")
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("ILLEGAL_TRANSITION"));

    mockMvc
        .perform(
            post("/internal/v1/leads/" + leadId + "/assignments")
                .header("X-Actor-Id", "rm-9")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"assignedRmId\":\"SP-1001\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.state").value("ASSIGNED"));
  }

  @Test
  void rejectionClosesTheLead() throws Exception {
    String leadId = create("cust-old-2");
    mockMvc
        .perform(post("/internal/v1/leads/" + leadId + "/onboarding").header("X-Actor-Id", "rm-9"))
        .andExpect(status().isOk());

    decide(leadId, "reject", "BANK_EMPLOYEE", "BANK_ADMIN", "Rejected: policy")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.state").value("DISQUALIFIED"))
        .andExpect(jsonPath("$.exceptionOutcome").value("BLOCK"));
  }

  @Test
  void cleanCustomerPassesOnTheEngine() throws Exception {
    String leadId = create("cust-young-1");
    mockMvc
        .perform(post("/internal/v1/leads/" + leadId + "/onboarding").header("X-Actor-Id", "rm-9"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.exceptionOutcome").value("PASS"));
  }

  private String create(String customerId) throws Exception {
    var created =
        mockMvc
            .perform(
                post("/internal/v1/leads")
                    .header("X-Actor-Id", "rm-9")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"customerId\":\""
                            + customerId
                            + "\",\"lob\":\"LIFE\",\"productClass\":\"TERM\",\"branchId\":\"BR-1\"}"))
            .andExpect(status().isCreated())
            .andReturn();
    return objectMapper.readTree(created.getResponse().getContentAsString()).get("leadId").asText();
  }

  private org.springframework.test.web.servlet.ResultActions decide(
      String leadId, String verb, String actorType, String roles, String remarks) throws Exception {
    return mockMvc.perform(
        post("/internal/v1/leads/" + leadId + "/exception-hold:" + verb)
            .header("X-Actor-Id", "approver-1")
            .header("X-Actor-Type", actorType)
            .header("X-Actor-Roles", roles)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(java.util.Map.of("remarks", remarks))));
  }
}
