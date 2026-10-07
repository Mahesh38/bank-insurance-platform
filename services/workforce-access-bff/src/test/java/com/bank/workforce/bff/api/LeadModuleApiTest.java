package com.bank.workforce.bff.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bank.workforce.bff.config.WorkforceSessionProperties;
import com.bank.workforce.bff.session.SessionModels.WorkforceSession;
import com.bank.workforce.bff.session.SessionStore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
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
@Tag("FUNC-029")
class LeadModuleApiTest {

  private static final String SESSION = "lead-module-session";

  @Autowired MockMvc mockMvc;

  @Autowired SessionStore sessions;

  @Autowired WorkforceSessionProperties sessionProperties;

  @Autowired ObjectMapper objectMapper;

  @BeforeEach
  void seedSession() {
    sessions.putSession(
        new WorkforceSession(
            SESSION,
            UUID.fromString("11111111-1111-1111-1111-111111111111"),
            "BANK_EMPLOYEE",
            "ACTIVE",
            1L,
            null,
            "sub-1",
            "rm.one",
            "access-token",
            "refresh-token",
            "id-token",
            Instant.now().plusSeconds(3600),
            Instant.now(),
            Map.of()),
        sessionProperties.sessionTtl());
  }

  @Test
  void countryCodesIncludeIndiaValidationRule() throws Exception {
    mockMvc
        .perform(get("/api/v1/country-codes").header("X-Session-Handle", SESSION))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].countryCode").value("IN"))
        .andExpect(jsonPath("$[0].phoneCode").value("+91"))
        .andExpect(jsonPath("$[0].minLength").value(10))
        .andExpect(jsonPath("$[0].regex").value("^[6-9]\\d{9}$"));
  }

  @Test
  void mobileValidationUsesCountryCatalogue() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/mobile-numbers:validate")
                .header("X-Session-Handle", SESSION)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"countryCode\":\"IN\",\"nationalNumber\":\"9876543210\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.valid").value(true));
  }

  @Test
  void organisationLookupsFollowResourceNesting() throws Exception {
    mockMvc
        .perform(get("/api/v1/branches").header("X-Session-Handle", SESSION))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].branchId").value("BR-MUM-001"))
        .andExpect(jsonPath("$[0].branchCode").value("MUM001"));

    mockMvc
        .perform(get("/api/v1/branches/BR-MUM-001/verticals").header("X-Session-Handle", SESSION))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].verticalId").value("VERT-MUM-RET"));

    mockMvc
        .perform(
            get("/api/v1/branches/BR-MUM-001/specified-persons")
                .header("X-Session-Handle", SESSION))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].empId").value("SP-1001"));

    mockMvc
        .perform(
            get("/api/v1/verticals/VERT-MUM-RET/relationship-managers")
                .header("X-Session-Handle", SESSION))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].empId").value("RM-5001"));
  }

  @Test
  void customerSearchMasksIdentityAndCreateAssignsWithExceptionFlag() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/customers:search")
                .header("X-Session-Handle", SESSION)
                .param("by", "MOBILE")
                .param("q", "9876543210")
                .param("countryCode", "IN"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].maskedMobile").value("XXXXXX3210"))
        .andExpect(jsonPath("$.items[0].eligibility").value("ETB"));

    mockMvc
        .perform(get("/api/v1/customers/CUST-3210").header("X-Session-Handle", SESSION))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.customerId").value("CUST-3210"))
        .andExpect(jsonPath("$.eligibility").value("ETB"));

    MvcResult created =
        mockMvc
            .perform(
                post("/api/v1/leads")
                    .header("X-Session-Handle", SESSION)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"customerId\":\"cust-1\",\"lob\":\"LIFE\",\"productClass\":\"TERM\",\"branchId\":\"BR-MUM-001\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.leadId").isNotEmpty())
            .andExpect(jsonPath("$.dashboardStage").value("NEW"))
            .andReturn();
    JsonNode body = objectMapper.readTree(created.getResponse().getContentAsString());
    String leadId = body.get("leadId").asText();
    assertThat(leadId).hasSize(26);

    mockMvc
        .perform(
            get("/api/v1/customers/cust-1/active-leads")
                .header("X-Session-Handle", SESSION)
                .param("productClass", "TERM"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].leadId").value(leadId))
        .andExpect(jsonPath("$.hasMore").value(false));

    mockMvc
        .perform(
            post("/api/v1/leads/" + leadId + "/assignment")
                .header("X-Session-Handle", SESSION)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"assignedRmId\":\"SP-1001\",\"assignedSpId\":\"SP-1001\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.state").value("ASSIGNED"))
        .andExpect(jsonPath("$.exceptionRequired").value(false));
  }

  @Test
  void assignHoldCustomerReturnsExceptionRequired() throws Exception {
    MvcResult created =
        mockMvc
            .perform(
                post("/api/v1/leads")
                    .header("X-Session-Handle", SESSION)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\"customerId\":\"cust-HOLD-9\",\"lob\":\"LIFE\",\"productClass\":\"TERM\",\"branchId\":\"BR-MUM-001\"}"))
            .andExpect(status().isCreated())
            .andReturn();
    String leadId =
        objectMapper.readTree(created.getResponse().getContentAsString()).get("leadId").asText();

    mockMvc
        .perform(
            post("/api/v1/leads/" + leadId + "/assignment")
                .header("X-Session-Handle", SESSION)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"assignedRmId\":\"SP-1001\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.exceptionRequired").value(true))
        .andExpect(jsonPath("$.exceptionOutcome").value("APPROVAL_REQUIRED"));
  }

  @Test
  void unauthenticatedLeadApisAreRejected() throws Exception {
    mockMvc.perform(get("/api/v1/country-codes")).andExpect(status().isUnauthorized());
  }

  @Test
  void unknownCustomerIsAbsent() throws Exception {
    mockMvc
        .perform(get("/api/v1/customers/UNKNOWN").header("X-Session-Handle", SESSION))
        .andExpect(status().isNotFound());
  }
}
