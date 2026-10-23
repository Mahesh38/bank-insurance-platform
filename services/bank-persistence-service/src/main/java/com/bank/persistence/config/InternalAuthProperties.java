package com.bank.persistence.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Shared internal service key for {@code /internal/v1/**} — same pattern as
 * identity-provider-adapter-service ({@code InternalAuthProperties}). The key is a stop-gap until
 * workload identity (mTLS / service JWT) lands; network location alone is not trust.
 */
@Validated
@ConfigurationProperties("persistence.internal-auth")
public record InternalAuthProperties(@NotBlank @Size(min = 16) String sharedSecret) {

  public static final String HEADER = "X-Internal-Service-Key";

  /** Constant-time comparison so the key cannot be guessed byte by byte. */
  public boolean matches(String presented) {
    if (presented == null
        || presented.isBlank()
        || sharedSecret == null
        || sharedSecret.isBlank()) {
      return false;
    }
    return MessageDigest.isEqual(
        sharedSecret.getBytes(StandardCharsets.UTF_8), presented.getBytes(StandardCharsets.UTF_8));
  }

  @Override
  public String toString() {
    return "InternalAuthProperties[sharedSecret=redacted]";
  }
}
