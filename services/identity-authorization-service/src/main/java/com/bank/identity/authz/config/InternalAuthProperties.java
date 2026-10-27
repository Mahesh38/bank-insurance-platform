package com.bank.identity.authz.config;

import com.bank.common.security.internal.InternalCallerCredentials;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Caller authentication and privilege grants for {@code /internal/v1/**} (SUG-20261027-iah /
 * READINESS-REVIEW §3.7).
 *
 * @param callers caller id → shared key (each at least 16 characters)
 * @param actorCallers caller ids allowed to set {@code X-Actor-Id}
 * @param privileges caller id → privilege codes ({@code *} allowed)
 */
@Validated
@ConfigurationProperties("identity.internal-auth")
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

  /** The caller whose key matches, compared in constant time against every configured key. */
  public Optional<String> authenticate(String presented) {
    return credentials().authenticate(presented);
  }

  public boolean mayAssertActor(String caller) {
    return credentials().mayAssertActor(caller);
  }

  public boolean hasPrivilege(String caller, String privilege) {
    return credentials().hasPrivilege(caller, privilege);
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
