package com.bank.platform.lead.adapter.rules;

import com.bank.platform.lead.domain.RuleConfigurationException;
import com.bank.rules.client.RuleSetQuery;
import com.bank.rules.client.RuleSetResolver;
import com.bank.rules.client.RuleSetUnresolvableException;
import com.bank.rules.engine.RuleSet;
import java.time.Instant;
import java.time.ZoneId;
import org.springframework.web.client.ResourceAccessException;

/**
 * Resolves a rule set and translates the library's failure into the domain's (ADR-024 §5 point 4):
 * an I/O failure or timeout towards administration-config-service is {@code upstreamUnavailable}
 * (503 {@code UPSTREAM_UNAVAILABLE}); everything else — no version in force, not published, invalid
 * document, hash mismatch — is {@code CONFIGURATION_UNRESOLVABLE}.
 */
final class RuleSetLookup {

  /** Rule-set effective dates are bank-time instants (seeds: {@code +05:30}). */
  static final ZoneId BANK_ZONE = ZoneId.of("Asia/Kolkata");

  private RuleSetLookup() {}

  static RuleSet resolve(
      RuleSetResolver resolver, String domain, String lob, String productClass, Instant asOf) {
    try {
      return resolver.resolve(
          new RuleSetQuery(domain, lob, productClass, asOf.atZone(BANK_ZONE).toOffsetDateTime()));
    } catch (RuleSetUnresolvableException ex) {
      throw new RuleConfigurationException(
          ex.getMessage(), ex.getCause() instanceof ResourceAccessException, ex);
    }
  }
}
