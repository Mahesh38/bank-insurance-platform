package com.bank.persistence.support;

import com.bank.persistence.config.InternalAuthProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.MockMvcBuilderCustomizer;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.ConfigurableMockMvcBuilder;

/**
 * Test-only: every auto-configured MockMvc request carries the internal service key, so the API
 * tests exercise the controllers as 1SB does. {@code InternalServiceAuthFilterTest} builds its own
 * MockMvc without this default to prove the refusal path.
 */
@Component
public class InternalKeyMockMvcCustomizer implements MockMvcBuilderCustomizer {

  private final InternalAuthProperties properties;

  public InternalKeyMockMvcCustomizer(InternalAuthProperties properties) {
    this.properties = properties;
  }

  @Override
  public void customize(ConfigurableMockMvcBuilder<?> builder) {
    builder.defaultRequest(
        MockMvcRequestBuilders.get("/")
            .header(InternalAuthProperties.HEADER, properties.sharedSecret()));
  }
}
