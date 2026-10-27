package com.bank.identity.authz.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bank.identity.authz.support.AuthzFixtures;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** READINESS-REVIEW §3.7: identity-authorization had no caller authentication. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Tag("IAM-001")
@Tag("integration")
class InternalCallerAuthenticationTest {

  private static final String BFF_KEY = "local-authz-bff-internal-key";
  private static final String LEAD_KEY = "local-authz-lead-internal-key";
  private static final String HEADER = "X-Internal-Service-Key";
  private static final String DECISION =
      """
      {"subjectId":"%s","action":"lead.read","resource":{"type":"lead"}}
      """
          .formatted(UUID.randomUUID());
  private static final String PARTNER =
      """
      {"username":"partner.web","email":"partner.web@example.test","insurerCode":"ICICI_PRU"}
      """;

  @Autowired MockMvc mockMvc;
  @Autowired JdbcClient jdbc;

  @Test
  void internalEndpointsRefuseACallerWithoutAKey() throws Exception {
    mockMvc
        .perform(
            post("/internal/v1/authorization/decisions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(DECISION))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
  }

  @Test
  void internalEndpointsRefuseAWrongKey() throws Exception {
    mockMvc
        .perform(
            post("/internal/v1/identities/resolve")
                .header(HEADER, "not-a-configured-internal-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void bffAndLeadServiceMayAskForDecisions() throws Exception {
    for (String key : new String[] {BFF_KEY, LEAD_KEY}) {
      mockMvc
          .perform(
              post("/internal/v1/authorization/decisions")
                  .header(HEADER, key)
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(DECISION))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.allowed").value(false))
          .andExpect(jsonPath("$.reasonCode").value("UNKNOWN_SUBJECT"));
    }
  }

  @Test
  void healthProbeNeedsNoKey() throws Exception {
    mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
  }

  @Test
  void onlyTheBffMayAssertAMaker() throws Exception {
    mockMvc
        .perform(
            post("/internal/v1/partner-users")
                .header(HEADER, LEAD_KEY)
                .header("X-Actor-Id", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(PARTNER))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("FORBIDDEN"));
  }

  @Test
  void makerMustBeAnAuthenticatedBusinessUser() throws Exception {
    mockMvc
        .perform(
            post("/internal/v1/partner-users")
                .header(HEADER, BFF_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(PARTNER))
        .andExpect(status().isUnauthorized());
    mockMvc
        .perform(
            post("/internal/v1/partner-users")
                .header(HEADER, BFF_KEY)
                .header("X-Actor-Id", "maker-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(PARTNER))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void bankAdminCreatesPartnerDirectlyAndApproveIsRefusedWhenApprovalIsNotRequired()
      throws Exception {
    AuthzFixtures.insurer(jdbc, "ICICI_PRU");
    UUID admin = AuthzFixtures.activeUser(jdbc, "web.admin", "BANK_EMPLOYEE", null, "BANK_ADMIN");

    mockMvc
        .perform(
            post("/internal/v1/partner-users")
                .header(HEADER, BFF_KEY)
                .header("X-Actor-Id", admin.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(PARTNER))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.status").value("PENDING"))
        .andExpect(jsonPath("$.approvalRequired").value(false))
        .andExpect(jsonPath("$.approvalId").doesNotExist());

    // E5: the maker-checker step is configured off, so there is nothing to approve.
    mockMvc
        .perform(
            post("/internal/v1/approval-requests/{id}/approve", UUID.randomUUID())
                .header(HEADER, BFF_KEY)
                .header("X-Actor-Id", admin.toString()))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("ILLEGAL_TRANSITION"));
  }

  @Test
  void insurerAdministratorCannotCreatePartnerUsers() throws Exception {
    AuthzFixtures.insurer(jdbc, "ICICI_PRU");
    UUID partnerAdmin =
        AuthzFixtures.activeUser(
            jdbc, "web.partner.admin", "INSURER_REPRESENTATIVE", "ICICI_PRU", "PARTNER_ADMIN");

    mockMvc
        .perform(
            post("/internal/v1/partner-users")
                .header(HEADER, BFF_KEY)
                .header("X-Actor-Id", partnerAdmin.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(PARTNER))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("FORBIDDEN"));
  }

  @Test
  void leadKeyCannotSetPassword() throws Exception {
    mockMvc
        .perform(
            post("/internal/v1/login/password")
                .header(HEADER, LEAD_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"businessUserId":"%s","newPassword":"Aa1!aaaa","confirmPassword":"Aa1!aaaa","purpose":"RESET"}
                    """
                        .formatted(UUID.randomUUID())))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("FORBIDDEN"));
  }

  @Test
  void bffKeyMaySetPassword() throws Exception {
    AuthzFixtures.insurer(jdbc, "ICICI_PRU");
    UUID partner =
        AuthzFixtures.activeUser(
            jdbc, "pwd.partner", "INSURER_REPRESENTATIVE", "ICICI_PRU", "PARTNER_SR");

    mockMvc
        .perform(
            post("/internal/v1/login/password")
                .header(HEADER, BFF_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"businessUserId":"%s","newPassword":"Aa1!aaaaaa","confirmPassword":"Aa1!aaaaaa","purpose":"RESET"}
                    """
                        .formatted(partner)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.result").value("SET"));
  }

  @Test
  void profileReturnsRoleCodes() throws Exception {
    UUID subject =
        AuthzFixtures.activeUser(jdbc, "profile.rm", "BANK_EMPLOYEE", null, "RELATIONSHIP_MANAGER");

    mockMvc
        .perform(
            post("/internal/v1/identities/profile")
                .header(HEADER, LEAD_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"subjectId\":\"" + subject + "\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.subjectId").value(subject.toString()))
        .andExpect(jsonPath("$.userType").value("BANK_EMPLOYEE"))
        .andExpect(jsonPath("$.status").value("ACTIVE"))
        .andExpect(jsonPath("$.roleCodes[0]").value("RELATIONSHIP_MANAGER"));
  }

  @Test
  void resolveIsDeniedWithoutPrivilege() throws Exception {
    mockMvc
        .perform(
            post("/internal/v1/identities/resolve")
                .header(HEADER, LEAD_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"provider":"KEYCLOAK","providerSubject":"s-1","userType":"BANK_EMPLOYEE","username":"u1"}
                    """))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("FORBIDDEN"));
  }
}
