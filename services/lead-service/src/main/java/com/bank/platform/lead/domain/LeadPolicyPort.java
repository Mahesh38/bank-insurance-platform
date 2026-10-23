package com.bank.platform.lead.domain;

import java.time.Duration;
import java.time.Instant;

/**
 * Lead periods from rule set {@code LEAD.POLICY} (E7): configuration per line of business, never a
 * constant in code. Throws {@link RuleConfigurationException} when no usable value is in force.
 */
public interface LeadPolicyPort {

  /** How long a lead of {@code lob} created at {@code asOf} stays open before it expires. */
  Duration expiryFor(String lob, Instant asOf);
}
