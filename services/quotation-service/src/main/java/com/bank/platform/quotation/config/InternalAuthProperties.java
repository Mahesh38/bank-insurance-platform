package com.bank.platform.quotation.config;

import com.bank.common.security.internal.InternalCallerCredentials;
import java.util.Map;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Inbound X-Internal-Service-Key callers for /internal/v1/** (SUG-20261027-iah). */
@Validated
@ConfigurationProperties("quotation.internal-auth")
public record InternalAuthProperties(
    Map<String, String> callers, Set<String> actorCallers, Map<String, Set<String>> privileges) {

  public InternalAuthProperties {
    callers = callers == null ? Map.of() : Map.copyOf(callers);
    actorCallers = actorCallers == null ? Set.of() : Set.copyOf(actorCallers);
    privileges = privileges == null ? Map.of() : Map.copyOf(privileges);
  }

  public InternalCallerCredentials credentials() {
    return new InternalCallerCredentials(callers, actorCallers, privileges);
  }

  @Override
  public String toString() {
    return "InternalAuthProperties[callers="
        + callers.keySet()
        + ", actorCallers="
        + actorCallers
        + ", privileges="
        + privileges.keySet()
        + ", keys=redacted]";
  }
}
