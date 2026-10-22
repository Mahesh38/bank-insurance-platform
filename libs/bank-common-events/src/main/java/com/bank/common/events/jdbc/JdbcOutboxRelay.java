package com.bank.common.events.jdbc;

import com.bank.common.events.EventEnvelope;
import com.bank.common.events.EventJsonCodec;
import com.bank.common.events.EventPublisher;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.sql.DataSource;

/**
 * Postgres outbox relay (ADR-025 §3, seam S-23). One {@link #relayOnce()} is one transaction:
 *
 * <ol>
 *   <li>Claim up to {@code batchSize} <em>head</em> rows — the oldest unpublished row of each
 *       aggregate — with {@code FOR UPDATE SKIP LOCKED} (PostgreSQL) or {@code FOR UPDATE}
 *       (others). Only heads are claimable, so while one relay holds an aggregate's head no other
 *       relay can take a later event of that aggregate: per-aggregate order survives several
 *       replicas.
 *   <li>Lock and read the pending rows of the claimed aggregates in {@code created_at, outbox_id}
 *       order. This re-read is also the double-publish guard where {@code SKIP LOCKED} is absent: a
 *       relay that waited on a lock sees the other relay's committed {@code published_at}.
 *   <li>Publish each through {@link EventPublisher}; mark {@code published_at}. On failure: {@code
 *       attempts + 1}, {@code last_error}, and at {@code maxAttempts} set {@code parked_at} (alert
 *       — never silently dropped). Later rows of a failed aggregate wait for the next cycle.
 * </ol>
 *
 * <p>A parked row blocks the later events of its aggregate (head-of-line) until operations
 * re-drives it ({@code UPDATE … SET parked_at = NULL, attempts = 0}) — order is never broken
 * silently. Publish-then-crash-before-commit republishes; consumers dedupe on {@code eventId}.
 */
public final class JdbcOutboxRelay {

  private static final System.Logger LOG = System.getLogger(JdbcOutboxRelay.class.getName());
  private static final int LAST_ERROR_MAX = 1000;

  private final DataSource dataSource;
  private final String table;
  private final EventPublisher publisher;
  private final EventJsonCodec codec;
  private final OutboxRelaySettings settings;
  private final Clock clock;
  private volatile SqlDialect dialect;

  public JdbcOutboxRelay(
      DataSource dataSource,
      String schema,
      EventPublisher publisher,
      EventJsonCodec codec,
      OutboxRelaySettings settings,
      Clock clock) {
    this.dataSource = Objects.requireNonNull(dataSource);
    this.table = SqlDialect.requireSchema(schema) + ".outbox_event";
    this.publisher = Objects.requireNonNull(publisher);
    this.codec = Objects.requireNonNull(codec);
    this.settings = Objects.requireNonNull(settings);
    this.clock = Objects.requireNonNull(clock);
    this.dialect = settings.dialect();
  }

  /** Relays until a cycle claims nothing or {@code maxCycles} is reached; returns the total. */
  public RelayResult drain(int maxCycles) {
    int claimed = 0;
    int published = 0;
    int failed = 0;
    int parked = 0;
    int deferred = 0;
    for (int i = 0; i < maxCycles; i++) {
      RelayResult r = relayOnce();
      claimed += r.claimed();
      published += r.published();
      failed += r.failed();
      parked += r.parked();
      deferred += r.deferred();
      if (r.claimed() == 0 || r.published() == 0) {
        break;
      }
    }
    return new RelayResult(claimed, published, failed, parked, deferred);
  }

  public RelayResult relayOnce() {
    try (Connection c = dataSource.getConnection()) {
      boolean autoCommit = c.getAutoCommit();
      c.setAutoCommit(false);
      try {
        RelayResult result = relay(c);
        c.commit();
        return result;
      } catch (SQLException | RuntimeException ex) {
        c.rollback();
        throw ex;
      } finally {
        c.setAutoCommit(autoCommit);
      }
    } catch (SQLException ex) {
      throw new OutboxException("outbox relay cycle failed on " + table, ex);
    }
  }

  public OutboxBacklog backlog() {
    String sql =
        "SELECT"
            + " SUM(CASE WHEN parked_at IS NULL THEN 1 ELSE 0 END),"
            + " SUM(CASE WHEN parked_at IS NOT NULL THEN 1 ELSE 0 END),"
            + " MIN(CASE WHEN parked_at IS NULL THEN created_at END)"
            + " FROM "
            + table
            + " WHERE published_at IS NULL";
    try (Connection c = dataSource.getConnection();
        PreparedStatement ps = c.prepareStatement(sql);
        ResultSet rs = ps.executeQuery()) {
      rs.next();
      OffsetDateTime oldest = rs.getObject(3, OffsetDateTime.class);
      return new OutboxBacklog(
          rs.getLong(1), rs.getLong(2), oldest == null ? null : oldest.toInstant());
    } catch (SQLException ex) {
      throw new OutboxException("outbox backlog query failed on " + table, ex);
    }
  }

  private RelayResult relay(Connection c) throws SQLException {
    SqlDialect d = dialect(c);
    Set<String> aggregates = claimHeads(c, d);
    if (aggregates.isEmpty()) {
      return RelayResult.EMPTY;
    }
    List<Row> rows = lockPending(c, aggregates);
    Set<String> blocked = new HashSet<>();
    int published = 0;
    int failed = 0;
    int parked = 0;
    int deferred = 0;
    for (Row row : rows) {
      if (blocked.contains(row.aggregateKey())) {
        deferred++;
        continue;
      }
      try {
        EventEnvelope event = codec.decode(row.payload());
        publisher.publish(event);
        markPublished(c, row);
        published++;
      } catch (RuntimeException ex) {
        blocked.add(row.aggregateKey());
        if (markFailed(c, row, ex)) {
          parked++;
        } else {
          failed++;
        }
      }
    }
    return new RelayResult(rows.size(), published, failed, parked, deferred);
  }

  /** Step 1: lock the head row of up to batchSize aggregates; returns their aggregate keys. */
  private Set<String> claimHeads(Connection c, SqlDialect d) throws SQLException {
    String sql =
        "SELECT o.aggregate_type, o.aggregate_id FROM "
            + table
            + " o WHERE o.published_at IS NULL AND o.parked_at IS NULL AND NOT EXISTS ("
            + "SELECT 1 FROM "
            + table
            + " p WHERE p.aggregate_type = o.aggregate_type AND p.aggregate_id = o.aggregate_id"
            + " AND p.published_at IS NULL AND (p.created_at < o.created_at"
            + " OR (p.created_at = o.created_at AND p.outbox_id < o.outbox_id)))"
            + " ORDER BY o.created_at, o.outbox_id LIMIT ?"
            + d.claimLock();
    Set<String> keys = new LinkedHashSet<>();
    try (PreparedStatement ps = c.prepareStatement(sql)) {
      ps.setInt(1, settings.batchSize());
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          keys.add(key(rs.getString(1), rs.getString(2)));
        }
      }
    }
    return keys;
  }

  /** Step 2: lock and read the claimed aggregates' pending rows, oldest first. */
  private List<Row> lockPending(Connection c, Set<String> aggregateKeys) throws SQLException {
    Set<String> ids = new LinkedHashSet<>();
    for (String k : aggregateKeys) {
      ids.add(k.substring(k.indexOf('|') + 1));
    }
    String placeholders = String.join(",", java.util.Collections.nCopies(ids.size(), "?"));
    String sql =
        "SELECT outbox_id, aggregate_type, aggregate_id, payload, attempts FROM "
            + table
            + " WHERE published_at IS NULL AND parked_at IS NULL AND aggregate_id IN ("
            + placeholders
            + ") ORDER BY created_at, outbox_id LIMIT ? FOR UPDATE";
    List<Row> rows = new ArrayList<>();
    try (PreparedStatement ps = c.prepareStatement(sql)) {
      int i = 1;
      for (String id : ids) {
        ps.setString(i++, id);
      }
      ps.setInt(i, settings.batchSize());
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          Row row =
              new Row(
                  rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getInt(5));
          if (aggregateKeys.contains(row.aggregateKey())) {
            rows.add(row);
          }
        }
      }
    }
    return rows;
  }

  private void markPublished(Connection c, Row row) throws SQLException {
    try (PreparedStatement ps =
        c.prepareStatement(
            "UPDATE " + table + " SET published_at = ?, last_error = NULL WHERE outbox_id = ?")) {
      ps.setObject(1, now());
      ps.setString(2, row.outboxId());
      ps.executeUpdate();
    }
  }

  /** Returns true when this failure parked the row. */
  private boolean markFailed(Connection c, Row row, RuntimeException cause) throws SQLException {
    int attempts = row.attempts() + 1;
    boolean park = attempts >= settings.maxAttempts();
    String error = describe(cause);
    try (PreparedStatement ps =
        c.prepareStatement(
            "UPDATE "
                + table
                + " SET attempts = ?, last_error = ?, parked_at = ? WHERE outbox_id = ?")) {
      ps.setInt(1, attempts);
      ps.setString(2, error);
      ps.setObject(3, park ? now() : null);
      ps.setString(4, row.outboxId());
      ps.executeUpdate();
    }
    if (park) {
      // The alert hook: a parked row is an operations task (ADR-025 §3), never a silent drop.
      LOG.log(
          System.Logger.Level.ERROR,
          "outbox row parked after {0} attempts: table={1} outboxId={2} aggregateId={3} error={4}",
          attempts,
          table,
          row.outboxId(),
          row.aggregateId(),
          error);
    } else {
      LOG.log(
          System.Logger.Level.WARNING,
          "outbox publish failed (attempt {0}): table={1} outboxId={2} error={3}",
          attempts,
          table,
          row.outboxId(),
          error);
    }
    return park;
  }

  private static String describe(RuntimeException ex) {
    String text = ex.getClass().getSimpleName() + ": " + ex.getMessage();
    return text.length() > LAST_ERROR_MAX ? text.substring(0, LAST_ERROR_MAX) : text;
  }

  private OffsetDateTime now() {
    Instant instant = clock.instant();
    return OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
  }

  private SqlDialect dialect(Connection c) throws SQLException {
    SqlDialect d = dialect;
    if (d == null) {
      d = SqlDialect.detect(c);
      dialect = d;
    }
    return d;
  }

  private static String key(String aggregateType, String aggregateId) {
    return aggregateType + "|" + aggregateId;
  }

  private record Row(
      String outboxId, String aggregateType, String aggregateId, String payload, int attempts) {
    String aggregateKey() {
      return key(aggregateType, aggregateId);
    }
  }
}
