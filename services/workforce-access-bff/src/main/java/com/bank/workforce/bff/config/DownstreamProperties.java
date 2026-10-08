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
    @NotNull URI customerServiceBaseUrl,
    @NotBlank String leadMode,
    @NotBlank String customerMode) {

  public static final String PROVIDER_ADAPTER_INTERNAL_KEY_HEADER = "X-Internal-Service-Key";

  @Override
  public String toString() {
    return "DownstreamProperties[providerAdapterBaseUrl="
        + providerAdapterBaseUrl
        + ", providerAdapterInternalKey=redacted, authorizationServiceBaseUrl="
        + authorizationServiceBaseUrl
        + ", leadServiceBaseUrl="
        + leadServiceBaseUrl
        + ", customerServiceBaseUrl="
        + customerServiceBaseUrl
        + ", leadMode="
        + leadMode
        + ", customerMode="
        + customerMode
        + "]";
  }
}
