package com.bank.workforce.bff.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/** E8: the identity-provider budget in application.yml is 4.5 s and environment-overridable. */
@SpringBootTest(properties = "WORKFORCE_IDP_CONNECT_TIMEOUT=800ms")
class IdentityProviderBudgetBindingTest {

  @Autowired DownstreamTimeouts timeouts;

  @Test
  void applicationYmlBindsTheBudgetAndHonoursTheEnvironment() {
    assertThat(timeouts.identityProvider().read()).isEqualTo(Duration.ofMillis(4500));
    assertThat(timeouts.identityProvider().connect()).isEqualTo(Duration.ofMillis(800));
  }
}
