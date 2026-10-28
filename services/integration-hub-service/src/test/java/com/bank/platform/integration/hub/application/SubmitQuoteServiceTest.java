package com.bank.platform.integration.hub.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.common.error.ErrorCodes;
import com.bank.common.error.PlatformLayer;
import com.bank.common.error.ServiceErrors;
import com.bank.common.error.ServiceException;
import com.bank.platform.integration.hub.domain.CallerRef;
import com.bank.platform.integration.hub.domain.CanonicalState;
import com.bank.platform.integration.hub.domain.Failure;
import com.bank.platform.integration.hub.domain.HubOutbox;
import com.bank.platform.integration.hub.domain.OnesbQuotePort;
import com.bank.platform.integration.hub.domain.ProviderBreakerRegistry;
import com.bank.platform.integration.hub.domain.ProviderRequest;
import com.bank.platform.integration.hub.domain.ProviderRequestStore;
import com.bank.platform.integration.hub.domain.RoutingPolicy;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@Tag("unit")
@Tag("FUNC-032")
class SubmitQuoteServiceTest {

  @Mock private ProviderRequestStore store;
  @Mock private OnesbQuotePort onesbQuotes;
  @Mock private HubOutbox outbox;

  private SubmitQuoteService service;
  private final Clock clock = Clock.fixed(Instant.parse("2026-10-28T00:00:00Z"), ZoneOffset.UTC);

  @BeforeEach
  void setUp() {
    service =
        new SubmitQuoteService(
            store,
            onesbQuotes,
            outbox,
            new ProviderBreakerRegistry(true, 5, 30_000L, 1),
            ServiceErrors.of("integration-hub", PlatformLayer.L5),
            clock,
            new ObjectMapper());
  }

  @Test
  void failedRow_sameKey_replaysError_not202_M1() {
    ProviderRequest prior =
        ProviderRequest.rehydrate(
            "01JTQ7Z9X4N2B8C5D6E7F8G9HA",
            "key-1",
            "deadbeef",
            "QUOTE",
            "LIFE",
            "TERM",
            "MULTI",
            new CallerRef("quotation", "01JTQ7Z9X4N2B8C5D6E7F8G9HB"),
            "01JQX4K8A1B2C3D4E5F6G7H8J9",
            RoutingPolicy.PROVIDER_ONE_SB,
            RoutingPolicy.ROUTING_VERSION,
            clock.instant(),
            clock.instant(),
            CanonicalState.FAILED,
            null,
            new Failure(ErrorCodes.UPSTREAM_BAD_RESPONSE, "adapter refused"));
    when(store.findByIdempotencyKey("key-1")).thenReturn(Optional.of(prior));

    // Hash will not match deadbeef → conflict first. Seed hash by using real command then stub.
    // Instead force match: spy hash via saving a real row path is heavy; call with empty and
    // temporarily set request hash by computing through a successful first submit mock.

    SubmitQuoteCommand command = command("key-1");
    // Compute expected hash by first successful path would be complex; instead patch prior hash
    // by submitting once then failing — use ArgumentCaptor approach: make find return empty first,
    // then we don't test hash. For M1 we need same hash: use reflection-free approach —
    // call submit once to create, then mark failed in store mock.

    when(store.findByIdempotencyKey("key-1")).thenReturn(Optional.empty());
    when(onesbQuotes.submitQuote(any()))
        .thenThrow(
            ServiceErrors.of("integration-hub", PlatformLayer.L5)
                .error(ErrorCodes.UPSTREAM_BAD_RESPONSE)
                .component("test")
                .operation("submit")
                .reason("boom")
                .build());

    assertThatThrownBy(() -> service.submit(command))
        .isInstanceOf(ServiceException.class)
        .extracting(ex -> ((ServiceException) ex).getErrorResponse().getCode())
        .isEqualTo(ErrorCodes.UPSTREAM_BAD_RESPONSE);

    // Capture saved row and replay with same hash
    org.mockito.ArgumentCaptor<ProviderRequest> captor =
        org.mockito.ArgumentCaptor.forClass(ProviderRequest.class);
    verify(store, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
    ProviderRequest failed = captor.getValue();
    assertThat(failed.state()).isEqualTo(CanonicalState.FAILED);

    when(store.findByIdempotencyKey("key-1")).thenReturn(Optional.of(failed));
    assertThatThrownBy(() -> service.submit(command))
        .isInstanceOf(ServiceException.class)
        .extracting(ex -> ((ServiceException) ex).getErrorResponse().getCode())
        .isEqualTo(ErrorCodes.UPSTREAM_BAD_RESPONSE);
    verify(onesbQuotes, org.mockito.Mockito.times(1)).submitQuote(any());
  }

  @Test
  void dispatchingRow_requestInProgress_AT_HUB_02() {
    ProviderRequest prior =
        ProviderRequest.rehydrate(
            "01JTQ7Z9X4N2B8C5D6E7F8G9HC",
            "key-d",
            "abc",
            "QUOTE",
            "LIFE",
            "TERM",
            "MULTI",
            new CallerRef("quotation", "01JTQ7Z9X4N2B8C5D6E7F8G9HD"),
            "01JQX4K8A1B2C3D4E5F6G7H8J9",
            RoutingPolicy.PROVIDER_ONE_SB,
            RoutingPolicy.ROUTING_VERSION,
            clock.instant(),
            clock.instant(),
            CanonicalState.DISPATCHING,
            null,
            null);

    // Align hash: first create empty then we can't easily. Use Mockito Answers to return prior
    // with hash matching by submitting and interrupting — simpler to compute hash via service
    // private method isn't accessible. Use ArgumentCaptor after one successful save of DISPATCHING
    // by stubbing submit to hang... Alternative: stub find to return empty, capture first save
    // (DISPATCHING), then on second call return that with same hash before adapter returns.

    when(store.findByIdempotencyKey("key-d"))
        .thenReturn(Optional.empty())
        .thenAnswer(
            inv -> {
              // After first save, subsequent find returns DISPATCHING with real hash
              return Optional.of(prior);
            });
    // Fix: update prior hash after first save
    org.mockito.Mockito.doAnswer(
            inv -> {
              ProviderRequest row = inv.getArgument(0);
              ProviderRequest dispatching =
                  ProviderRequest.rehydrate(
                      row.providerRequestId(),
                      row.idempotencyKey(),
                      row.requestHash(),
                      row.operation(),
                      row.lob(),
                      row.productClass(),
                      row.mode(),
                      row.callerRef(),
                      row.journeyId(),
                      row.provider(),
                      row.routingVersion(),
                      row.createdAt(),
                      row.updatedAt(),
                      CanonicalState.DISPATCHING,
                      null,
                      null);
              when(store.findByIdempotencyKey("key-d")).thenReturn(Optional.of(dispatching));
              return null;
            })
        .when(store)
        .save(any());

    when(onesbQuotes.submitQuote(any()))
        .thenReturn(new OnesbQuotePort.SubmitOutcome.Accepted("job-1"));

    // First call will save DISPATCHING then overwrite find — but same call continues to adapter.
    // Second call hits DISPATCHING.
    service.submit(command("key-d"));
    assertThatThrownBy(() -> service.submit(command("key-d")))
        .isInstanceOf(ServiceException.class)
        .extracting(ex -> ((ServiceException) ex).getErrorResponse().getCode())
        .isEqualTo(ErrorCodes.REQUEST_IN_PROGRESS);
    verify(onesbQuotes, org.mockito.Mockito.times(1)).submitQuote(any());
  }

  private static SubmitQuoteCommand command(String key) {
    return new SubmitQuoteCommand(
        key,
        "RM-1",
        "33333333-3333-3333-3333-333333333333",
        "quotation-service",
        "QUOTE",
        "LIFE",
        "TERM",
        new SubmitQuoteCommand.CallerRefView("quotation", "01JTQ7Z9X4N2B8C5D6E7F8G9HE"),
        "01JQX4K8A1B2C3D4E5F6G7H8J9",
        "MULTI",
        List.of(),
        "SUM_ASSURED",
        BigDecimal.valueOf(1_000_000),
        null,
        List.of(
            new SubmitQuoteCommand.Member(
                "LIFE_ASSURED", 1, "1990-01-01", "MALE", false, null, "400001", null)),
        null,
        null,
        Map.of("operation", "QUOTE"));
  }
}
