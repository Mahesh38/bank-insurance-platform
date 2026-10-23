package com.bank.workforce.bff.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.time.Duration;
import java.util.Base64;
import java.util.Set;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

/** READINESS-REVIEW §3.7: no prod-profile guard against development-only settings. */
@Tag("unit")
class ProductionSafetyGuardTest {

  private static final String ZERO_KEY = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=";
  private static final String REAL_KEY =
      Base64.getEncoder().encodeToString("0123456789abcdef0123456789abcdef".getBytes());

  @Test
  void developmentDefaultsAreRefusedUnderProdAndUatWithAClearMessage() {
    for (String profile : new String[] {"prod", "uat"}) {
      ProductionSafetyGuard guard =
          guard(profile, session("memory", ZERO_KEY), downstream("stub", "stub"));

      assertThatThrownBy(guard::refuseDevelopmentSettingsInDeployedProfiles)
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("refuses to start under the prod/uat profile")
          .hasMessageContaining("workforce.session.store=memory")
          .hasMessageContaining("all-zero development key")
          .hasMessageContaining("lead-mode=stub")
          .hasMessageContaining("customer-mode=stub");
    }
  }

  @Test
  void eachSettingIsCheckedOnItsOwn() {
    assertThat(
            ProductionSafetyGuard.violations(
                session("redis", ZERO_KEY), downstream("http", "http")))
        .singleElement()
        .asString()
        .contains("encryption-key");
    assertThat(
            ProductionSafetyGuard.violations(
                session("redis", REAL_KEY), downstream("stub", "http")))
        .singleElement()
        .asString()
        .contains("lead-mode");
    assertThat(
            ProductionSafetyGuard.violations(
                session("memory", REAL_KEY), downstream("http", "http")))
        .singleElement()
        .asString()
        .contains("store=memory");
  }

  @Test
  void aFixedOtpCodeIsRefusedUnderProd() {
    MockEnvironment environment = new MockEnvironment();
    environment.setActiveProfiles("prod");
    environment.setProperty("workforce.login.otp-code", "482913");
    ProductionSafetyGuard guard =
        new ProductionSafetyGuard(
            environment, session("redis", REAL_KEY), downstream("http", "http"));

    assertThatThrownBy(guard::refuseDevelopmentSettingsInDeployedProfiles)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("workforce.login.otp-code is set");
  }

  @Test
  void productionSettingsStartAndLocalDefaultsAreAllowedOutsideProd() {
    assertThatCode(
            guard("prod", session("redis", REAL_KEY), downstream("http", "http"))
                ::refuseDevelopmentSettingsInDeployedProfiles)
        .doesNotThrowAnyException();
    assertThatCode(
            guard("default", session("memory", ZERO_KEY), downstream("stub", "stub"))
                ::refuseDevelopmentSettingsInDeployedProfiles)
        .doesNotThrowAnyException();
  }

  private static ProductionSafetyGuard guard(
      String profile, WorkforceSessionProperties session, DownstreamProperties downstream) {
    MockEnvironment environment = new MockEnvironment();
    environment.setActiveProfiles(profile);
    return new ProductionSafetyGuard(environment, session, downstream);
  }

  private static WorkforceSessionProperties session(String store, String key) {
    return new WorkforceSessionProperties(
        store,
        "WORKFORCE_SESSION",
        key,
        Duration.ofMinutes(5),
        Duration.ofMinutes(1),
        Duration.ofHours(8),
        Set.of("http://localhost"));
  }

  private static DownstreamProperties downstream(String leadMode, String customerMode) {
    URI uri = URI.create("http://downstream.example.test");
    return new DownstreamProperties(
        uri,
        "local-idp-adapter-internal-key",
        uri,
        uri,
        uri,
        leadMode,
        customerMode,
        "local-authz-bff-internal-key");
  }
}
