package com.bank.platform.integration.hub.domain;

import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Rejects caller-supplied attribution anywhere in the JSON tree ({@code INV-DIS-01}). Keys are
 * matched case-insensitively.
 */
public final class AttributionGuard {

  private static final Set<String> FORBIDDEN =
      Set.of("distributorid", "agentid", "splicenceid", "distribution", "rmemployeeid");

  private AttributionGuard() {}

  public static boolean containsAttribution(Object node) {
    if (node == null) {
      return false;
    }
    if (node instanceof Map<?, ?> map) {
      for (Map.Entry<?, ?> entry : map.entrySet()) {
        Object key = entry.getKey();
        if (key != null && FORBIDDEN.contains(key.toString().toLowerCase(Locale.ROOT))) {
          return true;
        }
        if (containsAttribution(entry.getValue())) {
          return true;
        }
      }
      return false;
    }
    if (node instanceof Collection<?> collection) {
      for (Object item : collection) {
        if (containsAttribution(item)) {
          return true;
        }
      }
    }
    return false;
  }
}
