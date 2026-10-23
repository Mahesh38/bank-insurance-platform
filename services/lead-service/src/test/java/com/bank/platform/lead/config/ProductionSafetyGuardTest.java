package com.bank.platform.lead.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class ProductionSafetyGuardTest {

  private static final String ZERO_KEY = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=";
  private static final String REAL_KEY = "AQIDBAUGBwgJCgsMDQ4PEBESExQVFhcYGRobHB0eHyA=";

  @Test
  void developmentDefaultsAreFineOutsideProdAndUat() {
    MockEnvironment env = devDefaults();
    env.setActiveProfiles("local");

    assertThat(ProductionSafetyGuard.violations(env)).isEmpty();
  }

  @Test
  void prodRefusesEveryDevelopmentDefault() {
    MockEnvironment env = devDefaults();
    env.setProperty("lead.store", "memory");
    env.setProperty("lead.exception-rules", "stub");
    env.setActiveProfiles("prod");

    assertThat(ProductionSafetyGuard.violations(env))
        .anyMatch(v -> v.contains("all-zero"))
        .anyMatch(v -> v.contains("lead.store=memory"))
        .anyMatch(v -> v.contains("H2"))
        .anyMatch(v -> v.contains("lead.exception-rules=stub"))
        .anyMatch(v -> v.contains("lead.rule-sets.source=classpath"));
    assertThatThrownBy(() -> new ProductionSafetyGuard(env))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void uatWithRealSettingsStarts() {
    MockEnvironment env = new MockEnvironment();
    env.setProperty("lead.crypto.field-key", REAL_KEY);
    env.setProperty("lead.store", "jdbc");
    env.setProperty("spring.datasource.url", "jdbc:postgresql://db:5432/lead");
    env.setProperty("lead.exception-rules", "engine");
    env.setProperty("lead.rule-sets.source", "http");
    env.setActiveProfiles("uat");

    assertThat(ProductionSafetyGuard.violations(env)).isEmpty();
  }

  @Test
  void prodRefusesSeedRuleSetsEvenWithEverythingElseReal() {
    MockEnvironment env = new MockEnvironment();
    env.setProperty("lead.crypto.field-key", REAL_KEY);
    env.setProperty("spring.datasource.url", "jdbc:postgresql://db:5432/lead");
    env.setActiveProfiles("prod");

    assertThat(ProductionSafetyGuard.violations(env))
        .containsExactly("lead.rule-sets.source=classpath (unapproved seed rule sets)");
  }

  private static MockEnvironment devDefaults() {
    MockEnvironment env = new MockEnvironment();
    env.setProperty("lead.crypto.field-key", ZERO_KEY);
    env.setProperty("spring.datasource.url", "jdbc:h2:mem:lead_service;MODE=PostgreSQL");
    return env;
  }
}
