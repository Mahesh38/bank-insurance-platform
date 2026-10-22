package com.bank.common.events.jdbc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bank.common.events.EventEnvelope;
import com.bank.common.events.IdempotentEventHandler;
import com.bank.common.events.IdempotentEventHandler.Outcome;
import com.bank.common.events.TestEvents;
import com.bank.common.events.Ulid;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class IdempotentEventHandlerTest {

  private DataSource ds;

  @BeforeEach
  void setUp() throws SQLException {
    ds = H2Outbox.create();
  }

  @Test
  void duplicateDeliveryIsSkipped() throws Exception {
    List<String> handled = new ArrayList<>();
    EventEnvelope event = TestEvents.event(Ulid.mint(), "PaymentReconciled");
    try (Connection c = ds.getConnection()) {
      IdempotentEventHandler handler =
          handler(c, "proposal.materialiser", e -> handled.add(e.eventId()));

      assertThat(handler.process(event)).isEqualTo(Outcome.PROCESSED);
      assertThat(handler.process(event)).isEqualTo(Outcome.DUPLICATE);
      handler.handle(event);
    }
    assertThat(handled).containsExactly(event.eventId());
  }

  @Test
  void anotherConsumerProcessesTheSameEvent() throws Exception {
    EventEnvelope event = TestEvents.event(Ulid.mint(), "PaymentReconciled");
    try (Connection c = ds.getConnection()) {
      assertThat(handler(c, "proposal.materialiser", e -> {}).process(event))
          .isEqualTo(Outcome.PROCESSED);
      assertThat(handler(c, "quotation.materialiser", e -> {}).process(event))
          .isEqualTo(Outcome.PROCESSED);
    }
  }

  @Test
  void failedHandlerRollsBackTheClaimSoRedeliveryIsProcessed() throws Exception {
    EventEnvelope event = TestEvents.event(Ulid.mint(), "PaymentReconciled");
    try (Connection c = ds.getConnection()) {
      c.setAutoCommit(false);
      IdempotentEventHandler failing =
          handler(
              c,
              "policy.tracker",
              e -> {
                throw new IllegalStateException("downstream down");
              });
      assertThatThrownBy(() -> failing.process(event)).isInstanceOf(IllegalStateException.class);
      c.rollback();

      assertThat(handler(c, "policy.tracker", e -> {}).process(event)).isEqualTo(Outcome.PROCESSED);
      c.commit();
      try (ResultSet rs =
          c.createStatement().executeQuery("SELECT COUNT(*) FROM evt.processed_event")) {
        rs.next();
        assertThat(rs.getInt(1)).isEqualTo(1);
      }
    }
  }

  @Test
  void validatesArgumentsAndSurfacesSqlFailures() throws Exception {
    try (Connection c = ds.getConnection()) {
      JdbcProcessedEventStore store = store(c);
      assertThatThrownBy(() -> new IdempotentEventHandler(store, " ", e -> {}))
          .isInstanceOf(IllegalArgumentException.class);
      JdbcProcessedEventStore missing =
          new JdbcProcessedEventStore(
              "nope",
              new FixedConnectionProvider(c),
              TestEvents.tickingClock(),
              SqlDialect.GENERIC);
      assertThatThrownBy(() -> missing.claim(Ulid.mint(), "x")).isInstanceOf(OutboxException.class);
      // PostgreSQL path (ON CONFLICT DO NOTHING) is not H2 syntax in regular mode: the store
      // reports it.
      JdbcProcessedEventStore pg =
          new JdbcProcessedEventStore(
              "evt",
              new FixedConnectionProvider(c),
              TestEvents.tickingClock(),
              SqlDialect.POSTGRES);
      assertThatThrownBy(() -> pg.claim(Ulid.mint(), "x")).isInstanceOf(OutboxException.class);
    }
  }

  private IdempotentEventHandler handler(
      Connection c, String consumer, com.bank.common.events.EventHandler delegate) {
    return new IdempotentEventHandler(store(c), consumer, delegate);
  }

  private static JdbcProcessedEventStore store(Connection c) {
    return new JdbcProcessedEventStore(
        "evt", new FixedConnectionProvider(c), TestEvents.tickingClock(), null);
  }
}
