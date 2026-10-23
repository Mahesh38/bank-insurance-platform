package com.bank.identity.authz.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

@Tag("unit")
class ProductionDatasourceGuardTest {

  @Test
  void h2IsRefusedUnderProdAndUat() {
    for (String profile : new String[] {"prod", "uat"}) {
      MockEnvironment environment =
          new MockEnvironment().withProperty("spring.datasource.url", "jdbc:h2:mem:identityauthz");
      environment.setActiveProfiles(profile);

      assertThatThrownBy(() -> ProductionDatasourceGuard.check(environment))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("H2 datasource is forbidden");
    }
  }

  @Test
  void h2DriverIsRefusedEvenWithAnOddUrl() {
    MockEnvironment environment =
        new MockEnvironment()
            .withProperty("spring.datasource.url", "jdbc:tracing:whatever")
            .withProperty("spring.datasource.driver-class-name", "org.h2.Driver");
    environment.setActiveProfiles("prod");

    assertThatThrownBy(() -> ProductionDatasourceGuard.check(environment))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void postgresUnderProdAndH2LocallyAreAllowed() {
    MockEnvironment prod =
        new MockEnvironment()
            .withProperty(
                "spring.datasource.url", "jdbc:postgresql://db:5432/identity_authorization")
            .withProperty("spring.datasource.driver-class-name", "org.postgresql.Driver");
    prod.setActiveProfiles("prod");
    MockEnvironment local =
        new MockEnvironment().withProperty("spring.datasource.url", "jdbc:h2:mem:identityauthz");

    assertThatCode(() -> ProductionDatasourceGuard.check(prod)).doesNotThrowAnyException();
    assertThatCode(() -> ProductionDatasourceGuard.check(local)).doesNotThrowAnyException();
  }
}
