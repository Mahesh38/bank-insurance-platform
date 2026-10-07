package com.bank.platform.customer.adapter.apigee;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bank.common.apigee.ApigeeAccessTokenHolder;
import com.bank.common.apigee.ApigeeTokenClient;
import com.bank.common.apigee.IssuedToken;
import com.bank.common.error.ErrorCodes;
import com.bank.common.error.PlatformLayer;
import com.bank.common.error.ServiceErrors;
import com.bank.common.error.ServiceException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
@Tag("FUNC-031")
class HolderAccessTokenPortTest {

  private final Clock clock = Clock.fixed(Instant.parse("2026-10-07T12:00:00Z"), ZoneOffset.UTC);
  private final ServiceErrors errors = ServiceErrors.of("customer", PlatformLayer.L5);

  @Test
  void mintFailureIsUpstreamUnavailableWithoutSecretOrBody() {
    ApigeeTokenClient client =
        () -> {
          throw new IllegalStateException("Apigee /token rejected the client-credentials grant");
        };
    HolderAccessTokenPort port =
        new HolderAccessTokenPort(new ApigeeAccessTokenHolder(client, clock), errors);

    assertThatThrownBy(port::currentAccessToken)
        .isInstanceOf(ServiceException.class)
        .satisfies(
            thrown -> {
              ServiceException ex = (ServiceException) thrown;
              assertThat(ex.getErrorResponse().getCode()).isEqualTo(ErrorCodes.UPSTREAM_UNAVAILABLE);
              assertThat(ex.getMessage()).doesNotContain("super-secret");
              assertThat(ex.getMessage()).doesNotContain("access_token=");
              assertThat(ex.getDiagnostic().getReason()).isEqualTo("Apigee token mint failed");
            });
  }

  @Test
  void invalidateForcesNextCallToMintAgain() {
    AtomicInteger mints = new AtomicInteger();
    ApigeeTokenClient client =
        () -> {
          mints.incrementAndGet();
          return new IssuedToken("tok-" + mints.get(), clock.instant().plusSeconds(3600));
        };
    HolderAccessTokenPort port =
        new HolderAccessTokenPort(new ApigeeAccessTokenHolder(client, clock), errors);

    assertThat(port.currentAccessToken()).isEqualTo("tok-1");
    assertThat(port.currentAccessToken()).isEqualTo("tok-1");
    port.invalidate();
    assertThat(port.currentAccessToken()).isEqualTo("tok-2");
    assertThat(mints.get()).isEqualTo(2);
  }
}
