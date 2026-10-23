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
  // E8 (DECISIONS-2026-10-22): connect 2 s / read 3 s; env BANK_AD_VERIFY_CONNECT_TIMEOUT /
  // BANK_AD_VERIFY_READ_TIMEOUT override them (application.yml).
  public static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(1);
  public static final Duration DEFAULT_READ_TIMEOUT = Duration.ofSeconds(3);

  public BankAdVerifyProperties {
    connectTimeout = connectTimeout != null ? connectTimeout : DEFAULT_CONNECT_TIMEOUT;
    readTimeout = readTimeout != null ? readTimeout : DEFAULT_READ_TIMEOUT;
  }

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
