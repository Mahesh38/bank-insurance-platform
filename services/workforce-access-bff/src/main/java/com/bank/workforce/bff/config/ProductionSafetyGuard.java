package com.bank.workforce.bff.config;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Refuses to start under {@code prod} or {@code uat} with a development-only setting
 * (READINESS-REVIEW §3.7: "No prod-profile guard against the zero key, memory session store, stub
 * downstreams"). Each of these works locally and silently breaks a deployed BFF: an in-memory store
 * loses sessions across pods, the all-zero key makes every session readable by anyone with the
 * repository, a stub downstream serves fabricated leads or customers, a non-secure session cookie
 * is stored on plain HTTP, and a CORS allow-list lets a browser on another origin call the BFF.
 */
@Component
public class ProductionSafetyGuard {

  private final Environment environment;
  private final WorkforceSessionProperties session;
  private final DownstreamProperties downstream;
  private final WorkforceCorsProperties cors;

  public ProductionSafetyGuard(
      Environment environment,
      WorkforceSessionProperties session,
      DownstreamProperties downstream,
      WorkforceCorsProperties cors) {
    this.environment = environment;
    this.session = session;
    this.downstream = downstream;
    this.cors = cors;
  }

  @PostConstruct
  void refuseDevelopmentSettingsInDeployedProfiles() {
    if (!environment.matchesProfiles("prod", "uat")) {
      return;
    }
    List<String> violations = new ArrayList<>(violations(session, downstream, cors));
    String fixedOtp = environment.getProperty("workforce.login.otp-code");
    if (fixedOtp != null && !fixedOtp.isBlank()) {
      violations.add("workforce.login.otp-code is set (a fixed OTP must not be deployed)");
    }
    if (!violations.isEmpty()) {
      throw new IllegalStateException(
          "workforce-access-bff refuses to start under the prod/uat profile: "
              + String.join("; ", violations));
    }
  }

  static List<String> violations(
      WorkforceSessionProperties session,
      DownstreamProperties downstream,
      WorkforceCorsProperties cors) {
    List<String> violations = new ArrayList<>();
    if ("memory".equalsIgnoreCase(session.store())) {
      violations.add("workforce.session.store=memory (set WORKFORCE_SESSION_STORE=redis)");
    }
    if (isAllZeroKey(session.encryptionKey())) {
      violations.add(
          "workforce.session.encryption-key is the all-zero development key"
              + " (inject WORKFORCE_SESSION_ENCRYPTION_KEY from KMS)");
    }
    if (!session.cookieSecure()) {
      violations.add(
          "workforce.session.cookie-secure=false"
              + " (session cookies must stay Secure under prod/uat)");
    }
    if (!cors.allowedOrigins().isEmpty()) {
      violations.add(
          "workforce.cors.allowed-origins is set (browser CORS is limited to the dev profile)");
    }
    if ("stub".equalsIgnoreCase(downstream.leadMode())) {
      violations.add("workforce.downstream.lead-mode=stub (set LEAD_DOWNSTREAM_MODE=http)");
    }
    if ("stub".equalsIgnoreCase(downstream.customerMode())) {
      violations.add("workforce.downstream.customer-mode=stub (set CUSTOMER_DOWNSTREAM_MODE=http)");
    }
    return violations;
  }

  private static boolean isAllZeroKey(String encoded) {
    byte[] decoded;
    try {
      decoded = Base64.getDecoder().decode(encoded);
    } catch (IllegalArgumentException malformed) {
      return false; // TokenVaultCipher rejects a malformed key on its own.
    }
    for (byte b : decoded) {
      if (b != 0) {
        return false;
      }
    }
    return true;
  }
}
