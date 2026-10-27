package com.bank.platform.customer.api;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bank.common.security.internal.InternalCallerAuth;
import com.bank.common.security.internal.InternalCallerAuthFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/** Proves /internal/v1/** refuses callers without the service key (SUG-20261027-iah). */
@SpringBootTest
class InternalCallerAuthFilterTest {

  @Autowired WebApplicationContext context;

  @Autowired
  @Qualifier("internalCallerAuthFilter")
  FilterRegistrationBean<InternalCallerAuthFilter> registration;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcBuilders.webAppContextSetup(context).addFilters(registration.getFilter()).build();
  }

  @Test
  void missingKeyIsUnauthorized() throws Exception {
    mockMvc
        .perform(get("/internal/v1/customers/search"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code", is("UNAUTHORIZED")));
  }

  @Test
  void wrongKeyIsUnauthorized() throws Exception {
    mockMvc
        .perform(
            get("/internal/v1/customers/search")
                .header(InternalCallerAuth.HEADER, "not-the-right-key-xx"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code", is("UNAUTHORIZED")));
  }

  @Test
  void actuatorIsNotGuarded() throws Exception {
    mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
  }
}
