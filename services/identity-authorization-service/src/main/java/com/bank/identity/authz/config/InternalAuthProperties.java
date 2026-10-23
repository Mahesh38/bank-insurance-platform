package com.bank.identity.authz.config;

import jakarta.validation.constraints.NotEmpty;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Caller authentication for {@code /internal/v1/**} (READINESS-REVIEW §3.7: this service had none).
 *
 * <p>Same {@code X-Internal-Service-Key} scheme as identity-provider-adapter-service, but one key
 * per caller so the service knows <em>which</em> caller it is talking to. That is what lets it
 * refuse {@code X-Actor-Id} from anyone but the BFF: only callers listed in {@code actorCallers}
 * may assert a human maker or checker.
 *
 * @param callers caller id → shared key (each at least 16 characters)
 * @param actorCallers caller ids allowed to set {@code X-Actor-Id}
 */
@Validated
@ConfigurationProperties("identity.internal-auth")
public record InternalAuthProperties(
    @NotEmpty Map<String, String> callers, Set<String> actorCallers) {

  public static final String HEADER = "X-Internal-Service-Key";
  static final int MIN_KEY_LENGTH = 16;

  public InternalAuthProperties {
    callers = callers == null ? Map.of() : Map.copyOf(callers);
    actorCallers = actorCallers == null ? Set.of() : Set.copyOf(actorCallers);
    callers.forEach(
        (caller, key) -> {
          if (key == null || key.length() < MIN_KEY_LENGTH) {
            throw new IllegalArgumentException(
                "identity.internal-auth.callers."
                    + caller
                    + " must be at least "
                    + MIN_KEY_LENGTH
                    + " characters");
          }
        });
  }

  /** The caller whose key matches, compared in constant time against every configured key. */
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

  @Override
  public String toString() {
    return "InternalAuthProperties[callers=" + callers.keySet() + ", keys=redacted]";
  }
}
