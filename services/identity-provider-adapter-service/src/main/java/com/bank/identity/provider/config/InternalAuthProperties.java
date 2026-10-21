package com.bank.identity.provider.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("identity.internal-auth")
public record InternalAuthProperties(@NotBlank @Size(min = 16) String sharedSecret) {

  public static final String HEADER = "X-Internal-Service-Key";

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
