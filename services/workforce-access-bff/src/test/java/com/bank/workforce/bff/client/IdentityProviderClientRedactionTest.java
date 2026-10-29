package com.bank.workforce.bff.client;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class IdentityProviderClientRedactionTest {

  @Test
  void outboundCredentialRequestsDoNotPrintTheSecret() throws Exception {
    assertThat(request("KeycloakPasswordRequest", "Horse#99").toString())
        .isEqualTo("KeycloakPasswordRequest[redacted]")
        .doesNotContain("Horse#99");
    assertThat(request("AdVerifyRequest", "local-stub-password").toString())
        .isEqualTo("AdVerifyRequest[redacted]")
        .doesNotContain("local-stub-password");
    assertThat(new IdentityProviderClient.AdVerifyResult(true, true, null, null, null).accepted())
        .isTrue();
    assertThat(new IdentityProviderClient.AdVerifyResult(true, false, null, null, null).accepted())
        .isFalse();
    assertThat(new IdentityProviderClient.AdVerifyResult(false, true, null, null, null).accepted())
        .isFalse();
  }

  private static Object request(String simpleName, String secret) throws Exception {
    for (Class<?> type : IdentityProviderClient.class.getDeclaredClasses()) {
      if (type.getSimpleName().equals(simpleName)) {
        var constructor = type.getDeclaredConstructor(String.class, String.class);
        constructor.setAccessible(true);
        return constructor.newInstance("user", secret);
      }
    }
    throw new AssertionError(simpleName);
  }
}
