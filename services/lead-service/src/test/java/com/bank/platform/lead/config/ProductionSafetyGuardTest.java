package com.bank.platform.lead.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class ProductionSafetyGuardTest {

  private static final String ZERO_KEY = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=";

  /** 32 non-zero bytes built in-process — not a committed credential literal. */
  private static final String REAL_KEY =
      Base64.getEncoder()
          .encodeToString(
              new byte[] {
                1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23,
                24, 25, 26, 27, 28, 29, 30, 31, 32
              });

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
    env.setActiveProfiles("prod");

    assertThat(ProductionSafetyGuard.violations(env))
        .anyMatch(v -> v.contains("all-zero"))
        .anyMatch(v -> v.contains("lead.store=memory"))
        .anyMatch(v -> v.contains("H2"))
        .anyMatch(v -> v.contains("stub"));
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
    env.setActiveProfiles("uat");

    assertThat(ProductionSafetyGuard.violations(env)).isEmpty();
  }

  private static MockEnvironment devDefaults() {
    MockEnvironment env = new MockEnvironment();
    env.setProperty("lead.crypto.field-key", ZERO_KEY);
    env.setProperty("spring.datasource.url", "jdbc:h2:mem:lead_service;MODE=PostgreSQL");
    return env;
  }
}
