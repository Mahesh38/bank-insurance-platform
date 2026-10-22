package com.bank.common.events.jdbc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bank.common.events.EventEnvelope;
import com.bank.common.events.EventJsonCodec;
import com.bank.common.events.EventPublisher;
import com.bank.common.events.InMemoryEventPublisher;
import com.bank.common.events.TestEvents;
import com.bank.common.events.Ulid;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JdbcOutboxRelayTest {

  private final EventJsonCodec codec = new EventJsonCodec();
  private final Clock clock = TestEvents.tickingClock();
  private DataSource ds;
  private OutboxWriter writer;
  private InMemoryEventPublisher publisher;

  @BeforeEach
  void setUp() throws SQLException {
    ds = H2Outbox.create();
    writer = new OutboxWriter("evt", codec, clock);
    publisher = new InMemoryEventPublisher();
  }

  @Test
  void publishesInOrderPerAggregateAndMarksPublished() throws Exception {
    String a = Ulid.mint();
    String b = Ulid.mint();
    List<EventEnvelope> written =
        write(
            TestEvents.event(a, "PaymentLinkIssued"),
            TestEvents.event(b, "PaymentLinkIssued"),
            TestEvents.event(a, "PaymentCaptured"),
            TestEvents.event(a, "PaymentReconciled"),
            TestEvents.event(b, "PaymentCaptured"));

    RelayResult result = relay(10).relayOnce();

    assertThat(result).isEqualTo(new RelayResult(5, 5, 0, 0, 0));
    assertThat(publisher.published()).containsExactlyElementsOf(written);
    assertThat(H2Outbox.count(ds, "published_at IS NOT NULL")).isEqualTo(5);
    assertThat(relay(10).relayOnce()).isEqualTo(RelayResult.EMPTY);
    assertThat(relay(10).backlog()).isEqualTo(new OutboxBacklog(0, 0, null));
  }

  @Test
  void batchSizeBoundsTheClaimAndDrainContinues() throws Exception {
    for (int i = 0; i < 7; i++) {
      write(TestEvents.event(Ulid.mint(), "PaymentCaptured"));
    }
    JdbcOutboxRelay relay = relay(3);

    assertThat(relay.relayOnce().published()).isEqualTo(3);
    assertThat(relay.drain(10).published()).isEqualTo(4);
    assertThat(publisher.published()).hasSize(7);
  }

  @Test
  void failureCountsAnAttemptDefersTheAggregateAndRetries() throws Exception {
    String a = Ulid.mint();
    String b = Ulid.mint();
    EventEnvelope a1 = TestEvents.event(a, "PaymentCaptured");
    EventEnvelope a2 = TestEvents.event(a, "PaymentReconciled");
    EventEnvelope b1 = TestEvents.event(b, "PaymentCaptured");
    write(a1, a2, b1);
    publisher.failWhen(e -> e.eventId().equals(a1.eventId()));

    RelayResult first = relay(10).relayOnce();

    assertThat(first).isEqualTo(new RelayResult(3, 1, 1, 0, 1));
    assertThat(publisher.published()).containsExactly(b1);
    assertThat(H2Outbox.count(ds, "attempts = 1 AND last_error LIKE '%refused%'")).isEqualTo(1);
    OutboxBacklog backlog = relay(10).backlog();
    assertThat(backlog.pending()).isEqualTo(2);
    assertThat(backlog.oldestPendingCreatedAt()).isNotNull();

    publisher.failWhen(e -> false);
    assertThat(relay(10).relayOnce().published()).isEqualTo(2);
    assertThat(publisher.published()).containsExactly(b1, a1, a2);
    assertThat(H2Outbox.count(ds, "last_error IS NULL AND published_at IS NOT NULL")).isEqualTo(3);
  }

  @Test
  void parksAfterMaxAttemptsAndBlocksTheAggregateUntilRedriven() throws Exception {
    String a = Ulid.mint();
    EventEnvelope a1 = TestEvents.event(a, "PaymentCaptured");
    EventEnvelope a2 = TestEvents.event(a, "PaymentReconciled");
    write(a1, a2);
    publisher.failWhen(e -> e.eventId().equals(a1.eventId()));
    JdbcOutboxRelay relay = relayWith(new OutboxRelaySettings(10, 3, null));

    assertThat(relay.relayOnce().failed()).isEqualTo(1);
    assertThat(relay.relayOnce().failed()).isEqualTo(1);
    assertThat(relay.relayOnce().parked()).isEqualTo(1);
    assertThat(H2Outbox.count(ds, "parked_at IS NOT NULL AND attempts = 3")).isEqualTo(1);

    // Parked head blocks a2: order is never broken silently.
    assertThat(relay.relayOnce()).isEqualTo(RelayResult.EMPTY);
    assertThat(publisher.published()).isEmpty();
    assertThat(relay.backlog())
        .extracting(OutboxBacklog::pending, OutboxBacklog::parked)
        .containsExactly(1L, 1L);

    // Operations re-drive.
    publisher.failWhen(e -> false);
    exec(
        "UPDATE evt.outbox_event SET parked_at = NULL, attempts = 0 WHERE outbox_id = '"
            + a1.eventId()
            + "'");
    assertThat(relay.relayOnce().published()).isEqualTo(2);
    assertThat(publisher.published()).containsExactly(a1, a2);
  }

  @Test
  void defaultParkThresholdIsTen() throws Exception {
    EventEnvelope e = TestEvents.event(Ulid.mint(), "PaymentCaptured");
    write(e);
    publisher.failWhen(x -> true);
    JdbcOutboxRelay relay = relayWith(OutboxRelaySettings.defaults());
    for (int i = 0; i < 9; i++) {
      assertThat(relay.relayOnce().failed()).isEqualTo(1);
    }
    assertThat(relay.relayOnce().parked()).isEqualTo(1);
    assertThat(relay.drain(5)).isEqualTo(RelayResult.EMPTY);
  }

  @Test
  void unreadablePayloadIsAFailureNotACrash() throws Exception {
    exec(
        "INSERT INTO evt.outbox_event (outbox_id, aggregate_type, aggregate_id, event_type,"
            + " payload, created_at) VALUES ('"
            + Ulid.mint()
            + "', 'PAYMENT', 'x', 'PaymentCaptured',"
            + " '{broken', CURRENT_TIMESTAMP)");

    RelayResult result = relay(10).relayOnce();

    assertThat(result.failed()).isEqualTo(1);
    assertThat(H2Outbox.count(ds, "last_error LIKE 'EventCodecException%'")).isEqualTo(1);
  }

  @Test
  void twoRelaysConcurrentlyNeverDoublePublishAndKeepAggregateOrder() throws Exception {
    // H2 has no SKIP LOCKED: the second relay waits on FOR UPDATE, then its step-2 re-read sees
    // the first relay's published_at. SKIP LOCKED itself is proven only on PostgreSQL.
    List<EventEnvelope> written = new ArrayList<>();
    List<String> aggregates = new ArrayList<>();
    for (int i = 0; i < 20; i++) {
      aggregates.add(Ulid.mint());
    }
    for (int round = 0; round < 4; round++) {
      for (String a : aggregates) {
        written.addAll(write(TestEvents.event(a, "PaymentCaptured")));
      }
    }
    EventPublisher slow =
        e -> {
          try {
            Thread.sleep(1);
          } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
          }
          publisher.publish(e);
        };
    CountDownLatch start = new CountDownLatch(1);
    ExecutorService pool = Executors.newFixedThreadPool(2);
    try {
      List<Future<RelayResult>> futures = new ArrayList<>();
      for (int r = 0; r < 2; r++) {
        JdbcOutboxRelay relay =
            new JdbcOutboxRelay(
                ds, "evt", slow, codec, new OutboxRelaySettings(7, 10, null), clock);
        Callable<RelayResult> task =
            () -> {
              start.await();
              return relay.drain(100);
            };
        futures.add(pool.submit(task));
      }
      start.countDown();
      int total = 0;
      for (Future<RelayResult> f : futures) {
        total += f.get(60, TimeUnit.SECONDS).published();
      }
      assertThat(total).isEqualTo(written.size());
    } finally {
      pool.shutdownNow();
    }
    List<EventEnvelope> published = publisher.published();
    assertThat(published).hasSize(written.size()).doesNotHaveDuplicates();
    Map<String, List<String>> perAggregate = new HashMap<>();
    for (EventEnvelope e : published) {
      perAggregate.computeIfAbsent(e.aggregateId(), k -> new ArrayList<>()).add(e.eventId());
    }
    for (List<String> ids : perAggregate.values()) {
      assertThat(ids).isSorted();
    }
    assertThat(H2Outbox.count(ds, "published_at IS NULL")).isZero();
  }

  @Test
  void writerNeedsAProviderForTheImplicitConnectionOverload() {
    assertThatThrownBy(() -> writer.write(TestEvents.event(Ulid.mint(), "PaymentCaptured")))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void writerWithProviderAndExplicitDialect() throws Exception {
    try (Connection c = ds.getConnection()) {
      OutboxWriter w =
          new OutboxWriter("evt", codec, clock, new FixedConnectionProvider(c), SqlDialect.GENERIC);
      w.write(TestEvents.event(Ulid.mint(), "PaymentCaptured"));
    }
    assertThat(H2Outbox.count(ds, "1 = 1")).isEqualTo(1);
  }

  @Test
  void writerSurfacesSqlFailures() throws Exception {
    EventEnvelope e = TestEvents.event(Ulid.mint(), "PaymentCaptured");
    write(e);
    assertThatThrownBy(() -> write(e)).isInstanceOf(OutboxException.class);
  }

  @Test
  void rejectsUnsafeSchemaNamesAndSettings() {
    assertThatThrownBy(() -> new OutboxWriter("evt; DROP TABLE x", codec, clock))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new OutboxRelaySettings(0, 10, null))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new OutboxRelaySettings(10, 0, null))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void relaySurfacesDatabaseFailures() throws Exception {
    JdbcOutboxRelay relay =
        new JdbcOutboxRelay(ds, "missing", publisher, codec, OutboxRelaySettings.defaults(), clock);
    assertThatThrownBy(relay::relayOnce).isInstanceOf(OutboxException.class);
    assertThatThrownBy(relay::backlog).isInstanceOf(OutboxException.class);
  }

  @Test
  void dialectDetection() throws Exception {
    try (Connection c = ds.getConnection()) {
      assertThat(SqlDialect.detect(c)).isEqualTo(SqlDialect.GENERIC);
    }
    assertThat(SqlDialect.POSTGRES.claimLock()).contains("SKIP LOCKED");
    assertThat(SqlDialect.POSTGRES.payloadPlaceholder()).contains("jsonb");
    assertThat(SqlDialect.GENERIC.claimLock()).doesNotContain("SKIP");
  }

  private JdbcOutboxRelay relay(int batch) {
    return relayWith(new OutboxRelaySettings(batch, 10, null));
  }

  private JdbcOutboxRelay relayWith(OutboxRelaySettings settings) {
    return new JdbcOutboxRelay(ds, "evt", publisher, codec, settings, clock);
  }

  private List<EventEnvelope> write(EventEnvelope... events) throws SQLException {
    try (Connection c = ds.getConnection()) {
      c.setAutoCommit(false);
      for (EventEnvelope e : events) {
        writer.write(c, e);
      }
      c.commit();
    }
    return List.of(events);
  }

  private void exec(String sql) throws SQLException {
    try (Connection c = ds.getConnection();
        Statement s = c.createStatement()) {
      s.execute(sql);
    }
  }
}
