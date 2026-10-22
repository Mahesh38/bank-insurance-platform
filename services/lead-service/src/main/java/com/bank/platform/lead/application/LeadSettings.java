package com.bank.platform.lead.application;

import java.time.Duration;

/**
 * Configuration the lead commands read. {@code expiry} is the ageing horizon behind {@code
 * expires_at}; null leaves it unset because neither the Lead BRD nor the information model states a
 * value (BRD-P0-CAPABILITIES "expiry = configurable — confirm TTL"; 02-information-model {@code
 * expiresAt} "value is configuration (OPEN-D4)").
 */
public record LeadSettings(Duration expiry) {

  public static LeadSettings defaults() {
    return new LeadSettings(null);
  }
}
