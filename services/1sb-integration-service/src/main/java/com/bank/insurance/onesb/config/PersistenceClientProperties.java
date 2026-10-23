package com.bank.insurance.onesb.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * bank-persistence-service client. {@code internalServiceKey} is sent as {@code
 * X-Internal-Service-Key} on every call: bank-persistence refuses {@code /internal/v1/**} without
 * it (its InternalServiceAuthFilter — raw payloads decrypt to PII).
 */
@ConfigurationProperties(prefix = "bank.persistence")
public record PersistenceClientProperties(String baseUrl, String internalServiceKey) {

  public static final String INTERNAL_KEY_HEADER = "X-Internal-Service-Key";

  public PersistenceClientProperties {
    if (baseUrl == null || baseUrl.isBlank()) {
      baseUrl = "http://localhost:8081";
    }
    if (internalServiceKey == null || internalServiceKey.isBlank()) {
      throw new IllegalStateException(
          "bank.persistence.internal-service-key is required (BANK_PERSISTENCE_INTERNAL_KEY)");
    }
  }

  @Override
  public String toString() {
    return "PersistenceClientProperties[baseUrl=" + baseUrl + ", internalServiceKey=redacted]";
  }
}
