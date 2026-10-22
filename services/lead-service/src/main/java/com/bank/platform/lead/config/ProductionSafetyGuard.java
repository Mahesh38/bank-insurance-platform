package com.bank.platform.lead.config;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

/**
 * Refuses to start under {@code prod} / {@code uat} with a development default that would reach a
 * real environment by omission (READINESS-REVIEW §3.7, fail-closed security): the all-zero field
 * key, the in-memory lead store, an H2 datasource, or the temporary {@code LEAD.EXCEPTION} stub
 * (D2) that passes every customer.
 */
@Component
public class ProductionSafetyGuard {

  private static final Profiles PROD_LIKE = Profiles.of("prod", "uat");

  public ProductionSafetyGuard(Environment environment) {
    List<String> violations = violations(environment);
    if (!violations.isEmpty()) {
      throw new IllegalStateException(
          "lead-service refuses to start under prod/uat: " + String.join("; ", violations));
    }
  }

  static List<String> violations(Environment environment) {
    if (!environment.acceptsProfiles(PROD_LIKE)) {
      return List.of();
    }
    List<String> violations = new ArrayList<>();
    if (isZeroKey(environment.getProperty("lead.crypto.field-key"))) {
      violations.add("lead.crypto.field-key is the all-zero development key");
    }
    if ("memory".equalsIgnoreCase(environment.getProperty("lead.store", "jdbc"))) {
      violations.add("lead.store=memory");
    }
    String url = environment.getProperty("spring.datasource.url", "");
    if (url.toLowerCase(Locale.ROOT).startsWith("jdbc:h2:")) {
      violations.add("the datasource is H2");
    }
    if ("stub".equalsIgnoreCase(environment.getProperty("lead.exception-rules", "stub"))) {
      violations.add("lead.exception-rules=stub (temporary, D2)");
    }
    return violations;
  }

  // Missing or undecodable counts as unsafe too: the key must be deliberately provided.
  private static boolean isZeroKey(String encoded) {
    if (encoded == null || encoded.isBlank()) {
      return true;
    }
    try {
      for (byte b : Base64.getDecoder().decode(encoded.trim())) {
        if (b != 0) {
          return false;
        }
      }
      return true;
    } catch (IllegalArgumentException ex) {
      return true;
    }
  }
}
