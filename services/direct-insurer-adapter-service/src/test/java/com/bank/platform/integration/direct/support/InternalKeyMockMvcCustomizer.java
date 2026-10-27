package com.bank.platform.integration.direct.support;

import com.bank.common.security.internal.InternalCallerAuth;
import com.bank.platform.integration.direct.config.InternalAuthProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcBuilderCustomizer;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.ConfigurableMockMvcBuilder;

/**
 * Test-only: every auto-configured MockMvc request carries the BFF internal key.
 * InternalCallerAuthFilterTest builds its own MockMvc without this default to prove refusal.
 */
@Component
public class InternalKeyMockMvcCustomizer implements MockMvcBuilderCustomizer {

  private final InternalAuthProperties properties;

  public InternalKeyMockMvcCustomizer(InternalAuthProperties properties) {
    this.properties = properties;
  }

  @Override
  public void customize(ConfigurableMockMvcBuilder<?> builder) {
    String key = properties.callers().get("workforce-access-bff");
    builder.defaultRequest(MockMvcRequestBuilders.get("/").header(InternalCallerAuth.HEADER, key));
  }
}
