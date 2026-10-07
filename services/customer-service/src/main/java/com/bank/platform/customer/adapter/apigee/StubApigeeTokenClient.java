package com.bank.platform.customer.adapter.apigee;

import com.bank.common.apigee.ApigeeTokenClient;
import com.bank.common.apigee.IssuedToken;
import java.time.Clock;
import java.time.Duration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
    name = "customer.apigee.token-mode",
    havingValue = "stub",
    matchIfMissing = true)
public class StubApigeeTokenClient implements ApigeeTokenClient {

  private final Clock clock;

  public StubApigeeTokenClient(Clock clock) {
    this.clock = clock;
  }

  @Override
  public IssuedToken fetchClientCredentials() {
    return new IssuedToken("stub-apigee-token", clock.instant().plus(Duration.ofMinutes(5)));
  }
}
