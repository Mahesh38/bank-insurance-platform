package com.bank.platform.integration.hub.domain;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Compiled equivalent of seed {@code HUB.ROUTING} v1.0.0 (FIRST hit → ONE_SB for LIFE +
 * TERM|SAVINGS|ULIP). Full rule-engine resolve lands with administration-config wiring.
 */
public final class RoutingPolicy {

  public static final String ROUTING_VERSION = "HUB.ROUTING@1.0.0";
  public static final String PROVIDER_ONE_SB = "ONE_SB";

  private static final Set<String> PRODUCT_CLASSES = Set.of("TERM", "SAVINGS", "ULIP");

  private RoutingPolicy() {}

  public static Optional<String> route(String lob, String productClass) {
    if (lob == null || productClass == null) {
      return Optional.empty();
    }
    String lobN = lob.trim().toUpperCase(Locale.ROOT);
    String classN = productClass.trim().toUpperCase(Locale.ROOT);
    if ("LIFE".equals(lobN) && PRODUCT_CLASSES.contains(classN)) {
      return Optional.of(PROVIDER_ONE_SB);
    }
    return Optional.empty();
  }

  /** Canonical productClass → adapter Lob enum name (LLD §5.1). */
  public static Optional<String> adapterLob(String lob, String productClass) {
    if (!"LIFE".equalsIgnoreCase(lob)) {
      return Optional.empty();
    }
    return switch (productClass == null ? "" : productClass.trim().toUpperCase(Locale.ROOT)) {
      case "TERM" -> Optional.of("TERM");
      case "SAVINGS" -> Optional.of("SAVING");
      case "ULIP" -> Optional.of("ULIP");
      default -> Optional.empty();
    };
  }
}
