package com.bank.workforce.bff.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("workforce.downstream")
public record DownstreamProperties(
    @NotNull URI providerAdapterBaseUrl,
    @NotBlank @Size(min = 16) String providerAdapterInternalKey,
    @NotNull URI authorizationServiceBaseUrl,
    @NotNull URI leadServiceBaseUrl,
    @NotBlank @Size(min = 16) String leadServiceInternalKey,
    @NotNull URI customerServiceBaseUrl,
    @NotBlank @Size(min = 16) String customerServiceInternalKey,
    @NotBlank String leadMode,
    @NotBlank String customerMode,
    @NotBlank @Size(min = 16) String authorizationServiceInternalKey) {

  public static final String PROVIDER_ADAPTER_INTERNAL_KEY_HEADER = "X-Internal-Service-Key";

  /**
   * Same header, different key: identity-authorization-service authenticates each caller by its own
   * key, and only the BFF's key may assert {@code X-Actor-Id} (READINESS-REVIEW §3.7).
   */
  public static final String AUTHORIZATION_INTERNAL_KEY_HEADER = "X-Internal-Service-Key";

  /** Shared header name for lead-service and customer-service inbound keys (SUG-20261027-iah). */
  public static final String INTERNAL_SERVICE_KEY_HEADER = "X-Internal-Service-Key";

  @Override
  public String toString() {
    return "DownstreamProperties[providerAdapterBaseUrl="
        + providerAdapterBaseUrl
        + ", providerAdapterInternalKey=redacted, authorizationServiceBaseUrl="
        + authorizationServiceBaseUrl
        + ", leadServiceBaseUrl="
        + leadServiceBaseUrl
        + ", leadServiceInternalKey=redacted, customerServiceBaseUrl="
        + customerServiceBaseUrl
        + ", customerServiceInternalKey=redacted, leadMode="
        + leadMode
        + ", customerMode="
        + customerMode
        + ", authorizationServiceInternalKey=redacted]";
  }
}
