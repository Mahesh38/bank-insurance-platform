package com.bank.identity.provider.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("identity.ad-verify")
public record BankAdVerifyProperties(
    @NotBlank String mode,
    @NotNull Duration connectTimeout,
    @NotNull Duration readTimeout,
    Http http,
    Stub stub) {
  public boolean stubMode() {
    return "stub".equalsIgnoreCase(mode);
  }

  public List<StubUser> stubUsers() {
    return stub == null || stub.users() == null ? List.of() : stub.users();
  }

  public record Http(URI baseUrl, @NotBlank String path, String apiKey, String apiKeyHeader) {
    public String apiKeyHeaderName() {
      return apiKeyHeader == null || apiKeyHeader.isBlank() ? "X-API-Key" : apiKeyHeader;
    }
  }

  public record Stub(List<StubUser> users) {}

  public record StubUser(
      String employeeId, String password, boolean active, String email, String username) {}
}
