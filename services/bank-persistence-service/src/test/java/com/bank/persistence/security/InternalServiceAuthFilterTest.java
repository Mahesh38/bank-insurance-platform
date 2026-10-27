package com.bank.persistence.security;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bank.common.security.internal.InternalCallerAuth;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** /internal/v1/** needs a configured caller key; decrypt needs raw-payloads.decrypt. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Tag("integration")
class InternalServiceAuthFilterTest {

  private static final String ONESB_KEY = "test-persistence-internal-key";
  private static final String ADMIN_KEY = "test-persistence-admin-key1";

  @Autowired MockMvc mockMvc;

  @Test
  void rawPayloadReadWithoutKeyIsUnauthorized() throws Exception {
    mockMvc
        .perform(get("/internal/v1/raw-payloads/any-id").header(InternalCallerAuth.HEADER, ""))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code", is("UNAUTHORIZED")));
  }

  @Test
  void wrongKeyIsUnauthorized() throws Exception {
    mockMvc
        .perform(
            get("/internal/v1/audit-events")
                .param("resourceId", "r-1")
                .header(InternalCallerAuth.HEADER, "not-the-right-key-at-all"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void onesbCannotDecryptRawPayloads() throws Exception {
    mockMvc
        .perform(
            get("/internal/v1/raw-payloads/no-such-id")
                .header(InternalCallerAuth.HEADER, ONESB_KEY))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code", is("FORBIDDEN")));
  }

  @Test
  void onesbCannotReadAuditEvents() throws Exception {
    mockMvc
        .perform(
            get("/internal/v1/audit-events")
                .param("resourceId", "r-1")
                .header(InternalCallerAuth.HEADER, ONESB_KEY))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code", is("FORBIDDEN")));
  }

  @Test
  void privilegedKeyReachesDecryptController() throws Exception {
    mockMvc
        .perform(
            get("/internal/v1/raw-payloads/no-such-id")
                .header(InternalCallerAuth.HEADER, ADMIN_KEY))
        .andExpect(status().isNotFound());
  }

  @Test
  void onesbMayAppendAudit() throws Exception {
    mockMvc
        .perform(
            post("/internal/v1/audit-events")
                .header(InternalCallerAuth.HEADER, ONESB_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"action":"TEST","actorId":"a1","actorType":"SYSTEM","resourceType":"JOB",
                     "resourceId":"r-onesb","outcome":"SUCCESS"}
                    """))
        .andExpect(status().isCreated());
  }

  @Test
  void actuatorIsNotGuarded() throws Exception {
    mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
  }
}
