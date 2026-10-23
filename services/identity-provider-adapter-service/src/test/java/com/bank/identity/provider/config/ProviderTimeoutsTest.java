package com.bank.identity.provider.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * E8 (DECISIONS-2026-10-22): AD verify and Keycloak default to connect 1 s / read 3 s, and each is
 * overridable from the environment through the placeholders in application.yml.
 */
class ProviderTimeoutsTest {

  /** The BFF's identity-provider read budget (workforce.downstream.timeouts.identity-provider). */
  private static final Duration BFF_IDENTITY_PROVIDER_READ = Duration.ofMillis(4500);

  @Nested
  @SpringBootTest
  class Defaults {

    @Autowired BankAdVerifyProperties adVerify;
    @Autowired KeycloakProperties keycloak;

    @Test
    void adVerifyAndKeycloakDefaultToTwoSecondConnectAndThreeSecondRead() {
      assertThat(adVerify.connectTimeout()).isEqualTo(Duration.ofSeconds(1));
      assertThat(adVerify.readTimeout()).isEqualTo(Duration.ofSeconds(3));
      assertThat(keycloak.connectTimeout()).isEqualTo(Duration.ofSeconds(1));
      assertThat(keycloak.readTimeout()).isEqualTo(Duration.ofSeconds(3));
    }

    @Test
    void theBffBudgetStaysAboveTheProviderReadTimeouts() {
      assertThat(BFF_IDENTITY_PROVIDER_READ)
          .isGreaterThan(adVerify.readTimeout())
          .isGreaterThan(keycloak.readTimeout());
    }
  }

  @Nested
  @SpringBootTest(
      properties = {
        "BANK_AD_VERIFY_CONNECT_TIMEOUT=750ms",
        "BANK_AD_VERIFY_READ_TIMEOUT=2500ms",
        "KEYCLOAK_CONNECT_TIMEOUT=1500ms",
        "KEYCLOAK_READ_TIMEOUT=2s"
      })
  class EnvironmentOverrides {

    @Autowired BankAdVerifyProperties adVerify;
    @Autowired KeycloakProperties keycloak;

    @Test
    void environmentVariablesOverrideEveryTimeout() {
      assertThat(adVerify.connectTimeout()).isEqualTo(Duration.ofMillis(750));
      assertThat(adVerify.readTimeout()).isEqualTo(Duration.ofMillis(2500));
      assertThat(keycloak.connectTimeout()).isEqualTo(Duration.ofMillis(1500));
      assertThat(keycloak.readTimeout()).isEqualTo(Duration.ofSeconds(2));
    }
  }

  @Test
  void codeDefaultsMatchTheConfiguredDefaults() {
    assertThat(BankAdVerifyProperties.DEFAULT_CONNECT_TIMEOUT).isEqualTo(Duration.ofSeconds(1));
    assertThat(BankAdVerifyProperties.DEFAULT_READ_TIMEOUT).isEqualTo(Duration.ofSeconds(3));
    assertThat(KeycloakProperties.DEFAULT_CONNECT_TIMEOUT).isEqualTo(Duration.ofSeconds(1));
    assertThat(KeycloakProperties.DEFAULT_READ_TIMEOUT).isEqualTo(Duration.ofSeconds(3));
  }
}
