package com.bank.identity.provider.adapter.keycloak;

import com.bank.identity.provider.config.KeycloakPasswordProperties;
import com.bank.identity.provider.config.KeycloakPasswordProperties.StubUser;
import com.bank.identity.provider.domain.IdentityProviderPort;
import com.bank.identity.provider.domain.InsuranceCredentialVerifier;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Insurance-user passwords are checked at Keycloak. Local and test use a stub so a missing Keycloak
 * does not block the login journey. Production refuses the stub.
 */
@Component
public class KeycloakPasswordVerifier implements InsuranceCredentialVerifier {

  private final KeycloakPasswordProperties properties;
  private final IdentityProviderPort keycloak;
  private final Environment environment;
  private final Map<String, StubUser> stubUsers = new ConcurrentHashMap<>();

  public KeycloakPasswordVerifier(
      KeycloakPasswordProperties properties,
      IdentityProviderPort keycloak,
      Environment environment) {
    this.properties = properties;
    this.keycloak = keycloak;
    this.environment = environment;
  }

  @PostConstruct
  void prepare() {
    if (environment.matchesProfiles("prod") && properties.stubMode()) {
      throw new IllegalStateException(
          "identity.keycloak-password.mode=stub is forbidden under the prod profile");
    }
    if (properties.stub() != null && properties.stub().users() != null) {
      for (StubUser user : properties.stub().users()) {
        stubUsers.put(user.username(), user);
      }
    }
  }

  @Override
  public Result verify(Command command) {
    if (properties.stubMode()) {
      return verifyStub(command);
    }
    IdentityProviderPort.PasswordCheck check =
        keycloak.verifyPassword(command.username(), command.password());
    return new Result(check.authenticated(), check.accountActive());
  }

  @Override
  public void replacePassword(ReplaceCommand command) {
    if (properties.stubMode()) {
      StubUser current = stubUsers.get(command.username());
      boolean active = current == null || current.active();
      stubUsers.put(
          command.username(), new StubUser(command.username(), command.password(), active));
      return;
    }
    keycloak.replacePassword(command.username(), command.password());
  }

  private Result verifyStub(Command command) {
    StubUser user = stubUsers.get(command.username());
    if (user == null || !constantEquals(user.password(), command.password())) {
      return new Result(false, true);
    }
    if (!user.active()) {
      return new Result(false, false);
    }
    return new Result(true, true);
  }

  private static boolean constantEquals(String expected, String actual) {
    if (expected == null || actual == null) {
      return false;
    }
    return MessageDigest.isEqual(
        expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8));
  }
}
