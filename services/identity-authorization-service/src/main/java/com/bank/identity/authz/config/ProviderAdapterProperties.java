package com.bank.identity.authz.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Provisioning client to identity-provider-adapter-service. Timeouts were absent (READINESS-REVIEW
 * §3.7); defaults are connect 1 s / read 3 s so one slow provider call cannot stall the dispatcher.
 */
@Validated
@ConfigurationProperties("identity.provider-adapter")
public record ProviderAdapterProperties(
    @NotNull URI baseUrl,
    @NotBlank @Size(min = 16) String internalKey,
    Duration connectTimeout,
    Duration readTimeout) {

  public static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(1);
  public static final Duration DEFAULT_READ_TIMEOUT = Duration.ofSeconds(3);

  public ProviderAdapterProperties {
    connectTimeout = connectTimeout != null ? connectTimeout : DEFAULT_CONNECT_TIMEOUT;
    readTimeout = readTimeout != null ? readTimeout : DEFAULT_READ_TIMEOUT;
  }

  @Override
  public String toString() {
    return "ProviderAdapterProperties[baseUrl=" + baseUrl + ", internalKey=redacted]";
  }
}
