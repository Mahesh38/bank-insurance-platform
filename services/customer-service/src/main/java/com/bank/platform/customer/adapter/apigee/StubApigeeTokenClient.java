package com.bank.platform.customer.adapter.apigee;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;

@Component
@ConditionalOnProperty(name = "customer.apigee.token-mode", havingValue = "stub", matchIfMissing = true)
public class StubApigeeTokenClient implements ApigeeTokenClient {

    private final Clock clock;

    public StubApigeeTokenClient(Clock clock) {
        this.clock = clock;
    }

    @Override
    public ApigeeAccessTokenHolder.IssuedToken fetchClientCredentials() {
        return new ApigeeAccessTokenHolder.IssuedToken(
            "stub-apigee-token", clock.instant().plus(Duration.ofMinutes(5)));
    }
}
