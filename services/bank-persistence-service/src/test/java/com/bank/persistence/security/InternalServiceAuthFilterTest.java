package com.bank.persistence.security;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bank.persistence.config.InternalAuthProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/** /internal/v1/** — including the decrypting raw-payload read — needs the internal service key. */
@SpringBootTest
@ActiveProfiles("test")
@Tag("integration")
class InternalServiceAuthFilterTest {

  @Autowired WebApplicationContext context;
  @Autowired InternalServiceAuthFilter filter;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(context).addFilters(filter).build();
  }

  @Test
  void rawPayloadReadWithoutKeyIsUnauthorized() throws Exception {
    mockMvc
        .perform(get("/internal/v1/raw-payloads/any-id"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code", is("UNAUTHORIZED")));
  }

  @Test
  void wrongKeyIsUnauthorized() throws Exception {
    mockMvc
        .perform(
            get("/internal/v1/audit-events")
                .param("resourceId", "r-1")
                .header(InternalAuthProperties.HEADER, "not-the-right-key-at-all"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void rightKeyReachesTheController() throws Exception {
    mockMvc
        .perform(
            get("/internal/v1/raw-payloads/no-such-id")
                .header(InternalAuthProperties.HEADER, "test-persistence-internal-key"))
        .andExpect(status().isNotFound());
  }

  @Test
  void actuatorIsNotGuarded() throws Exception {
    mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
  }
}
