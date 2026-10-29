package com.bank.identity.provider.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("identity.keycloak-password")
public record KeycloakPasswordProperties(String mode, Stub stub) {

  public KeycloakPasswordProperties {
    mode = mode == null || mode.isBlank() ? "stub" : mode;
  }

  public boolean stubMode() {
    return "stub".equalsIgnoreCase(mode);
  }

  public record Stub(List<StubUser> users) {}

  public record StubUser(String username, String password, boolean active) {}
}
