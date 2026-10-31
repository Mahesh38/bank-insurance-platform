package com.bank.workforce.bff.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
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
                session("redis", ZERO_KEY), downstream("http", "http"), cors()))
        .singleElement()
        .asString()
        .contains("encryption-key");
    assertThat(
            ProductionSafetyGuard.violations(
                session("redis", REAL_KEY), downstream("stub", "http"), cors()))
        .singleElement()
        .asString()
        .contains("lead-mode");
    assertThat(
            ProductionSafetyGuard.violations(
                session("memory", REAL_KEY), downstream("http", "http"), cors()))
        .singleElement()
        .asString()
        .contains("store=memory");
    assertThat(
            ProductionSafetyGuard.violations(
                session("redis", REAL_KEY, false), downstream("http", "http"), cors()))
        .singleElement()
        .asString()
        .contains("cookie-secure=false");
    assertThat(
            ProductionSafetyGuard.violations(
                session("redis", REAL_KEY),
                downstream("http", "http"),
                new WorkforceCorsProperties(List.of("http://localhost:3000"))))
        .singleElement()
        .asString()
        .contains("workforce.cors.allowed-origins is set");
  }

  @Test
  void aFixedOtpCodeIsRefusedUnderProd() {
    MockEnvironment environment = new MockEnvironment();
    environment.setActiveProfiles("prod");
    environment.setProperty("workforce.login.otp-code", "482913");
    ProductionSafetyGuard guard =
        new ProductionSafetyGuard(
            environment, session("redis", REAL_KEY), downstream("http", "http"), cors());

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
    assertThatCode(
            guard(
                    "dev",
                    session("memory", ZERO_KEY, false),
                    downstream("stub", "stub"),
                    new WorkforceCorsProperties(List.of("http://localhost:3000")))
                ::refuseDevelopmentSettingsInDeployedProfiles)
        .doesNotThrowAnyException();
  }

  @Test
  void insecureCookiesAndCorsAreRefusedUnderProdAndUat() {
    for (String profile : new String[] {"prod", "uat"}) {
      ProductionSafetyGuard guard =
          guard(
              profile,
              session("redis", REAL_KEY, false),
              downstream("http", "http"),
              new WorkforceCorsProperties(List.of("http://localhost:3000")));

      assertThatThrownBy(guard::refuseDevelopmentSettingsInDeployedProfiles)
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("cookie-secure=false")
          .hasMessageContaining("workforce.cors.allowed-origins is set");
    }
  }

  private static ProductionSafetyGuard guard(
      String profile, WorkforceSessionProperties session, DownstreamProperties downstream) {
    return guard(profile, session, downstream, cors());
  }

  private static ProductionSafetyGuard guard(
      String profile,
      WorkforceSessionProperties session,
      DownstreamProperties downstream,
      WorkforceCorsProperties cors) {
    MockEnvironment environment = new MockEnvironment();
    environment.setActiveProfiles(profile);
    return new ProductionSafetyGuard(environment, session, downstream, cors);
  }

  private static WorkforceCorsProperties cors() {
    return new WorkforceCorsProperties(List.of());
  }

  private static WorkforceSessionProperties session(String store, String key) {
    return session(store, key, true);
  }

  private static WorkforceSessionProperties session(
      String store, String key, boolean cookieSecure) {
    return new WorkforceSessionProperties(
        store,
        "WORKFORCE_SESSION",
        key,
        Duration.ofMinutes(5),
        Duration.ofMinutes(1),
        Duration.ofHours(8),
        Set.of("http://localhost"),
        cookieSecure);
  }

  private static DownstreamProperties downstream(String leadMode, String customerMode) {
    URI uri = URI.create("http://downstream.example.test");
    return new DownstreamProperties(
        uri,
        "local-idp-adapter-internal-key",
        uri,
        uri,
        "local-lead-bff-internal-key",
        uri,
        "local-customer-bff-internal-key",
        leadMode,
        customerMode,
        "local-authz-bff-internal-key");
  }
}
