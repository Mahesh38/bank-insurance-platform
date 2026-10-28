package com.bank.platform.integration.hub.domain;

import java.util.Locale;
import java.util.Optional;

/** Maps authenticated internal service id → callerRef.context. */
public final class CallerContexts {

  private CallerContexts() {}

  public static Optional<String> contextForService(String callerServiceId) {
    if (callerServiceId == null || callerServiceId.isBlank()) {
      return Optional.empty();
    }
    return switch (callerServiceId.trim().toLowerCase(Locale.ROOT)) {
      case "quotation-service" -> Optional.of("quotation");
      case "proposal-service" -> Optional.of("proposal");
      case "policy-issuance-service" -> Optional.of("policy");
      default -> Optional.empty();
    };
  }
}
