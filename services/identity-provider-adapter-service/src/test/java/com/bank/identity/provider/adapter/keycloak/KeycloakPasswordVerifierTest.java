package com.bank.identity.provider.adapter.keycloak;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.identity.provider.config.KeycloakPasswordProperties;
import com.bank.identity.provider.config.KeycloakPasswordProperties.Stub;
import com.bank.identity.provider.config.KeycloakPasswordProperties.StubUser;
import com.bank.identity.provider.domain.IdentityProviderPort;
import com.bank.identity.provider.domain.InsuranceCredentialVerifier.Command;
import com.bank.identity.provider.domain.InsuranceCredentialVerifier.ReplaceCommand;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;

class KeycloakPasswordVerifierTest {

  @Test
  void stubRejectsAWrongPasswordAnUnknownUserAndAnInactiveAccount() {
    KeycloakPasswordVerifier verifier = stubVerifier();

    var wrong = verifier.verify(new Command("fls.rm@example.test", "nope"));
    assertThat(wrong.authenticated()).isFalse();
    assertThat(wrong.accountActive()).isTrue();

    var unknown = verifier.verify(new Command("missing@example.test", "Horse#99"));
    assertThat(unknown.authenticated()).isFalse();
    assertThat(unknown.accountActive()).isTrue();

    var inactive = verifier.verify(new Command("idle.rm@example.test", "Horse#99"));
    assertThat(inactive.authenticated()).isFalse();
    assertThat(inactive.accountActive()).isFalse();

    var missingPassword = verifier.verify(new Command("fls.rm@example.test", null));
    assertThat(missingPassword.authenticated()).isFalse();

    var blankStoredPassword = verifier.verify(new Command("blank.rm@example.test", "Horse#99"));
    assertThat(blankStoredPassword.authenticated()).isFalse();
    assertThat(blankStoredPassword.accountActive()).isTrue();
  }

  @Test
  void stubReplaceKeepsAnInactiveAccountInactiveAndAcceptsANewUser() {
    KeycloakPasswordVerifier verifier = stubVerifier();

    verifier.replacePassword(new ReplaceCommand("fls.rm@example.test", "Zebra#99"));
    var replaced = verifier.verify(new Command("fls.rm@example.test", "Zebra#99"));
    assertThat(replaced.authenticated()).isTrue();
    assertThat(verifier.verify(new Command("fls.rm@example.test", "Horse#99")).authenticated())
        .isFalse();

    verifier.replacePassword(new ReplaceCommand("idle.rm@example.test", "Zebra#99"));
    var stillInactive = verifier.verify(new Command("idle.rm@example.test", "Zebra#99"));
    assertThat(stillInactive.authenticated()).isFalse();
    assertThat(stillInactive.accountActive()).isFalse();

    verifier.replacePassword(new ReplaceCommand("new.rm@example.test", "Zebra#99"));
    var created = verifier.verify(new Command("new.rm@example.test", "Zebra#99"));
    assertThat(created.authenticated()).isTrue();
    assertThat(created.accountActive()).isTrue();

    assertThat(new Command("fls.rm@example.test", "Horse#99").toString())
        .doesNotContain("Horse#99")
        .contains("username-present=true");
    assertThat(new Command(null, "Horse#99").toString()).contains("username-present=false");
    assertThat(new Command(" ", "Horse#99").toString()).contains("username-present=false");
    assertThat(new ReplaceCommand("fls.rm@example.test", "Horse#99").toString())
        .doesNotContain("Horse#99")
        .contains("username-present=true");
    assertThat(new ReplaceCommand(null, "Horse#99").toString())
        .doesNotContain("Horse#99")
        .contains("username-present=false");
    assertThat(new ReplaceCommand(" ", "Horse#99").toString()).contains("username-present=false");
  }

  @Test
  void httpModeDelegatesBothChecksToKeycloak() {
    IdentityProviderPort keycloak = mock(IdentityProviderPort.class);
    when(keycloak.verifyPassword("fls.rm@example.test", "Horse#99"))
        .thenReturn(new IdentityProviderPort.PasswordCheck(true, false));
    KeycloakPasswordVerifier verifier = verifier("http", null, keycloak, false);

    var result = verifier.verify(new Command("fls.rm@example.test", "Horse#99"));
    verifier.replacePassword(new ReplaceCommand("fls.rm@example.test", "Zebra#99"));

    assertThat(result.authenticated()).isTrue();
    assertThat(result.accountActive()).isFalse();
    verify(keycloak).replacePassword("fls.rm@example.test", "Zebra#99");
  }

  @Test
  void productionRefusesTheStubAndAllowsHttp() {
    IdentityProviderPort keycloak = mock(IdentityProviderPort.class);
    KeycloakPasswordVerifier stubbed = verifier("stub", stubUsers(), keycloak, true);
    assertThatThrownBy(stubbed::prepare)
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("forbidden");

    KeycloakPasswordVerifier http = verifier("http", null, keycloak, true);
    http.prepare();
    when(keycloak.verifyPassword("fls.rm@example.test", "Horse#99"))
        .thenReturn(new IdentityProviderPort.PasswordCheck(false, true));
    assertThat(http.verify(new Command("fls.rm@example.test", "Horse#99")).authenticated())
        .isFalse();
  }

  @Test
  void emptyStubListRejectsEveryPassword() {
    KeycloakPasswordVerifier noStub =
        verifier("stub", null, mock(IdentityProviderPort.class), false);
    var withoutUsers = verifier("stub", new Stub(null), mock(IdentityProviderPort.class), false);

    var missing = noStub.verify(new Command("fls.rm@example.test", "Horse#99"));
    var alsoMissing = withoutUsers.verify(new Command("fls.rm@example.test", "Horse#99"));

    assertThat(missing.authenticated()).isFalse();
    assertThat(missing.accountActive()).isTrue();
    assertThat(alsoMissing.authenticated()).isFalse();
  }

  private static KeycloakPasswordVerifier stubVerifier() {
    return verifier("stub", stubUsers(), mock(IdentityProviderPort.class), false);
  }

  private static Stub stubUsers() {
    return new Stub(
        List.of(
            new StubUser("fls.rm@example.test", "Horse#99", true),
            new StubUser("idle.rm@example.test", "Horse#99", false),
            new StubUser("blank.rm@example.test", null, true)));
  }

  private static KeycloakPasswordVerifier verifier(
      String mode, Stub stub, IdentityProviderPort keycloak, boolean prod) {
    Environment environment = mock(Environment.class);
    when(environment.matchesProfiles("prod")).thenReturn(prod);
    var verifier =
        new KeycloakPasswordVerifier(
            new KeycloakPasswordProperties(mode, stub), keycloak, environment);
    if (!prod || !"stub".equalsIgnoreCase(mode)) {
      verifier.prepare();
    }
    return verifier;
  }
}
