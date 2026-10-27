package com.bank.common.security.internal;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Per-caller shared keys and privilege grants. Network location is not trust — every east-west
 * caller must present a key that maps to a known caller id.
 *
 * @param callers caller id → shared key (each at least 16 characters)
 * @param actorCallers caller ids allowed to assert {@code X-Actor-Id}
 * @param privileges caller id → privilege codes ({@link InternalCallerAuth#ALL_PRIVILEGES} allowed)
 */
public record InternalCallerCredentials(
    Map<String, String> callers, Set<String> actorCallers, Map<String, Set<String>> privileges) {

  static final int MIN_KEY_LENGTH = 16;

  public InternalCallerCredentials {
    callers = callers == null ? Map.of() : Map.copyOf(callers);
    actorCallers = actorCallers == null ? Set.of() : Set.copyOf(actorCallers);
    Map<String, Set<String>> normalized = new LinkedHashMap<>();
    if (privileges != null) {
      privileges.forEach(
          (caller, grants) ->
              normalized.put(
                  caller, grants == null ? Set.of() : Set.copyOf(new LinkedHashSet<>(grants))));
    }
    privileges = Map.copyOf(normalized);
    callers.forEach(
        (caller, key) -> {
          if (key == null || key.length() < MIN_KEY_LENGTH) {
            throw new IllegalArgumentException(
                "internal-auth.callers."
                    + caller
                    + " must be at least "
                    + MIN_KEY_LENGTH
                    + " characters");
          }
        });
  }

  public Optional<String> authenticate(String presented) {
    if (presented == null || presented.isBlank()) {
      return Optional.empty();
    }
    byte[] candidate = presented.getBytes(StandardCharsets.UTF_8);
    String matched = null;
    for (Map.Entry<String, String> entry : callers.entrySet()) {
      if (MessageDigest.isEqual(entry.getValue().getBytes(StandardCharsets.UTF_8), candidate)) {
        matched = entry.getKey();
      }
    }
    return Optional.ofNullable(matched);
  }

  public boolean mayAssertActor(String caller) {
    return caller != null && actorCallers.contains(caller);
  }

  public boolean hasPrivilege(String caller, String privilege) {
    if (caller == null || privilege == null || privilege.isBlank()) {
      return false;
    }
    Set<String> grants = privileges.getOrDefault(caller, Set.of());
    return grants.contains(InternalCallerAuth.ALL_PRIVILEGES) || grants.contains(privilege);
  }

  @Override
  public String toString() {
    return "InternalCallerCredentials[callers="
        + callers.keySet()
        + ", actorCallers="
        + actorCallers
        + ", privileges="
        + privileges.keySet()
        + ", keys=redacted]";
  }
}
