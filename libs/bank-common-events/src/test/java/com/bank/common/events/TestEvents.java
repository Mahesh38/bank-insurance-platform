package com.bank.common.events;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicLong;

/** Shared fixtures. */
public final class TestEvents {

  private TestEvents() {}

  /** Advances one millisecond per read, so created_at order is deterministic. */
  public static Clock tickingClock() {
    AtomicLong millis = new AtomicLong(Instant.parse("2026-10-21T09:00:00Z").toEpochMilli());
    return new Clock() {
      @Override
      public ZoneOffset getZone() {
        return ZoneOffset.UTC;
      }

      @Override
      public Clock withZone(java.time.ZoneId zone) {
        return this;
      }

      @Override
      public Instant instant() {
        return Instant.ofEpochMilli(millis.getAndIncrement());
      }
    };
  }

  public static EventEnvelope paymentReconciled(String paymentId) {
    return EventEnvelope.builder()
        .eventType("PaymentReconciled")
        .source("payment")
        .aggregate("PAYMENT", paymentId)
        .journeyId(Ulid.mint())
        .actor(Actor.system("payment"))
        .traceId("4bf92f3577b34da6a3ce929d0e0e4736")
        .correlationId("corr-1")
        .configVersionRef(null)
        .data("paymentId", paymentId)
        .data("quoteId", Ulid.mint())
        .data("proposalId", Ulid.mint())
        .data("journeyId", Ulid.mint())
        .data("applicationNumber", "APP-1001")
        .data("settlementRef", "SETTLE-77")
        .data("method", "UPI")
        .data("paidAt", "2026-10-21T09:00:00Z")
        .data("reconciledAt", "2026-10-21T10:00:00Z")
        .data("amount", "25000.00")
        .data("currency", "INR")
        .build();
  }

  public static EventEnvelope event(String aggregateId, String eventType) {
    return EventEnvelope.builder()
        .eventType(eventType)
        .source("payment")
        .aggregate("PAYMENT", aggregateId)
        .actor(Actor.of("rm-1", "BANK_RM", "SP_ACCOUNTABLE"))
        .traceId("trace")
        .correlationId("corr")
        .data("paymentId", aggregateId)
        .build();
  }
}
