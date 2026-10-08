package com.bank.identity.authz.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("identity.provider-adapter")
public record ProviderAdapterProperties(
    @NotNull URI baseUrl, @NotBlank @Size(min = 16) String internalKey) {
  @Override
  public String toString() {
    return "ProviderAdapterProperties[baseUrl=" + baseUrl + ", internalKey=redacted]";
  }
}
