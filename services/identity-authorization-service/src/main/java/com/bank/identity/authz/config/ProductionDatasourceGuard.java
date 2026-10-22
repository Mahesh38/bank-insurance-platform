package com.bank.identity.authz.config;

import jakarta.annotation.PostConstruct;
import java.util.Locale;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Refuses to start under {@code prod} or {@code uat} with an in-memory H2 datasource
 * (READINESS-REVIEW §3.7: "no prod-profile guard against ... H2"). The authorization facts — roles,
 * grants, maker/checker evidence — would vanish on restart and differ per pod.
 */
@Component
public class ProductionDatasourceGuard {

  private final Environment environment;

  public ProductionDatasourceGuard(Environment environment) {
    this.environment = environment;
  }

  @PostConstruct
  void refuseH2InDeployedProfiles() {
    check(environment);
  }

  static void check(Environment environment) {
    if (!environment.matchesProfiles("prod", "uat")) {
      return;
    }
    String url = environment.getProperty("spring.datasource.url", "");
    String driver = environment.getProperty("spring.datasource.driver-class-name", "");
    if (url.toLowerCase(Locale.ROOT).startsWith("jdbc:h2:")
        || driver.toLowerCase(Locale.ROOT).contains("h2")) {
      throw new IllegalStateException(
          "identity-authorization-service refuses to start: an H2 datasource is forbidden under the"
              + " prod/uat profiles. Set AUTHZ_DATASOURCE_URL to the PostgreSQL authorization store.");
    }
  }
}
