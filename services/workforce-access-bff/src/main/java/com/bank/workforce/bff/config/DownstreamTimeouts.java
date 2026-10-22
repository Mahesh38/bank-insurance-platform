package com.bank.workforce.bff.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Per-downstream HTTP budgets (03 §5 seam catalogue). One shared client let a slow lead or customer
 * service hold an RM's request for the whole 5 s edge budget; each hop now has its own.
 *
 * <p>The PDP budget (S-02, 300 ms) lives with {@code PolicyDecisionClient} as {@code
 * workforce.authorization.decision-timeout}.
 */
@ConfigurationProperties("workforce.downstream.timeouts")
public record DownstreamTimeouts(
    Budget lead, Budget customer, Budget authorization, Budget identityProvider) {

  private static final Duration INTERNAL_CONNECT = Duration.ofMillis(500);

  public DownstreamTimeouts {
    // S-20 BFF → Lead: 2 s, fail closed.
    lead = Budget.orDefault(lead, INTERNAL_CONNECT, Duration.ofSeconds(2));
    // Customer search reaches CBS (S-05: 3 s, 1 retry); 4 s stays inside the 5 s edge (S-01).
    customer = Budget.orDefault(customer, INTERNAL_CONNECT, Duration.ofSeconds(4));
    // Identity resolve is a local lookup in the PDP's store.
    authorization = Budget.orDefault(authorization, INTERNAL_CONNECT, Duration.ofSeconds(2));
    // Login path. Must stay inside the 5 s edge budget (S-01) — it was 6 s (READINESS-REVIEW
    // §3.7). E8 (DECISIONS-2026-10-22): it stays above the adapter's AD-verify / Keycloak read
    // timeout (3 s; connect 2 s). Env WORKFORCE_IDP_CONNECT_TIMEOUT / WORKFORCE_IDP_READ_TIMEOUT.
    identityProvider =
        Budget.orDefault(identityProvider, Duration.ofSeconds(1), Duration.ofMillis(4500));
  }

  public record Budget(Duration connect, Duration read) {

    static Budget orDefault(Budget given, Duration connect, Duration read) {
      if (given == null) {
        return new Budget(connect, read);
      }
      return new Budget(
          given.connect() != null ? given.connect() : connect,
          given.read() != null ? given.read() : read);
    }
  }
}
