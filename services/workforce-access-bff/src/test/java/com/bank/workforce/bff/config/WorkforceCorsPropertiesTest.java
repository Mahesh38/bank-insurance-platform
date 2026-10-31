package com.bank.workforce.bff.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;

@Tag("unit")
class WorkforceCorsPropertiesTest {

  @Test
  void emptyAndBlankOriginsLeaveCorsOff() {
    assertThat(new WorkforceCorsProperties(null).allowedOrigins()).isEmpty();
    assertThat(new WorkforceCorsProperties(List.of()).allowedOrigins()).isEmpty();
    assertThat(new WorkforceCorsProperties(List.of("  ")).allowedOrigins()).isEmpty();
    assertThat(
            BffSecurityConfig.corsSource(new WorkforceCorsProperties(List.of()))
                .getCorsConfiguration(new MockHttpServletRequest("GET", "/api/v1/auth/csrf")))
        .isNull();
  }

  @Test
  void anExplicitOriginIsAllowedWithCredentialsAndAForeignOriginIsNot() {
    WorkforceCorsProperties properties =
        new WorkforceCorsProperties(List.of("http://localhost:3000"));
    CorsConfiguration configuration =
        BffSecurityConfig.corsSource(properties)
            .getCorsConfiguration(new MockHttpServletRequest("POST", "/api/v1/leads"));

    assertThat(configuration.checkOrigin("http://localhost:3000"))
        .isEqualTo("http://localhost:3000");
    assertThat(configuration.checkOrigin("http://evil.example")).isNull();
    assertThat(configuration.getAllowCredentials()).isTrue();
    assertThat(configuration.getAllowedHeaders()).contains("X-XSRF-TOKEN", "Content-Type");
    assertThat(configuration.getAllowedMethods()).contains("POST", "OPTIONS");
  }

  @Test
  void wildcardsAndOriginsWithAPathAreRejected() {
    assertThatThrownBy(() -> new WorkforceCorsProperties(List.of("*")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("wildcards");
    assertThatThrownBy(() -> new WorkforceCorsProperties(List.of("http://localhost:*")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("wildcards");
    assertThatThrownBy(
            () -> new WorkforceCorsProperties(List.of("http://localhost:3000/auth/complete")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("no path");
  }
}
