package com.bank.platform.lead.domain;

/**
 * A rule set the command needs could not be obtained or is not usable (ADR-024 §5 point 4). {@code
 * upstreamUnavailable}: the configuration service did not answer (503 {@code
 * UPSTREAM_UNAVAILABLE}); otherwise no usable version is in force (422 {@code
 * CONFIGURATION_UNRESOLVABLE}). Either way the command is refused — never a compiled-in default.
 */
public class RuleConfigurationException extends RuntimeException {

  private final boolean upstreamUnavailable;

  public RuleConfigurationException(String message, boolean upstreamUnavailable, Throwable cause) {
    super(message, cause);
    this.upstreamUnavailable = upstreamUnavailable;
  }

  public boolean upstreamUnavailable() {
    return upstreamUnavailable;
  }
}
