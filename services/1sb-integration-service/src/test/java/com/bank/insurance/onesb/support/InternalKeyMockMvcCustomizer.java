package com.bank.insurance.onesb.support;

import com.bank.common.security.internal.InternalCallerAuth;
import com.bank.insurance.onesb.config.InternalAuthProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcBuilderCustomizer;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.ConfigurableMockMvcBuilder;

/**
 * Test-only: every auto-configured MockMvc request carries the local-test internal key so existing
 * ITs keep working under inbound auth (SUG-20261027-iah).
 */
@Component
public class InternalKeyMockMvcCustomizer implements MockMvcBuilderCustomizer {

  private final InternalAuthProperties properties;

  public InternalKeyMockMvcCustomizer(InternalAuthProperties properties) {
    this.properties = properties;
  }

  @Override
  public void customize(ConfigurableMockMvcBuilder<?> builder) {
    String key =
        properties.callers().containsKey("local-test")
            ? properties.callers().get("local-test")
            : properties.callers().values().iterator().next();
    builder.defaultRequest(MockMvcRequestBuilders.get("/").header(InternalCallerAuth.HEADER, key));
  }
}
