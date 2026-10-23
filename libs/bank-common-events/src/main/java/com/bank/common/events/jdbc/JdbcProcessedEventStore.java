package com.bank.common.events.jdbc;

import com.bank.common.events.ProcessedEventStore;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;

/**
 * {@code <schema>.processed_event} (ADR-025 §4), written through the caller's transaction. On
 * PostgreSQL the claim is {@code INSERT … ON CONFLICT DO NOTHING} — a plain duplicate insert would
 * abort the whole transaction there. Elsewhere it is select-then-insert; a racing duplicate then
 * fails the insert, the transaction rolls back and the redelivery is recognised as a duplicate.
 */
public final class JdbcProcessedEventStore implements ProcessedEventStore {

  private final String table;
  private final ConnectionProvider connections;
  private final Clock clock;
  private volatile SqlDialect dialect;

  public JdbcProcessedEventStore(
      String schema, ConnectionProvider connections, Clock clock, SqlDialect dialect) {
    this.table = SqlDialect.requireSchema(schema) + ".processed_event";
    this.connections = Objects.requireNonNull(connections);
    this.clock = Objects.requireNonNull(clock);
    this.dialect = dialect;
  }

  @Override
  public boolean claim(String eventId, String consumer) {
    Connection c = null;
    try {
      c = connections.acquire();
      SqlDialect d = dialect;
      if (d == null) {
        d = SqlDialect.detect(c);
        dialect = d;
      }
      return d.onConflictDoNothing()
          ? insertIgnore(c, eventId, consumer)
          : selectThenInsert(c, eventId, consumer);
    } catch (SQLException ex) {
      throw new OutboxException("processed_event claim failed for " + eventId, ex);
    } finally {
      if (c != null) {
        connections.release(c);
      }
    }
  }

  private boolean insertIgnore(Connection c, String eventId, String consumer) throws SQLException {
    try (PreparedStatement ps =
        c.prepareStatement(
            "INSERT INTO "
                + table
                + " (event_id, consumer, processed_at) VALUES (?, ?, ?) ON CONFLICT DO NOTHING")) {
      bind(ps, eventId, consumer);
      return ps.executeUpdate() == 1;
    }
  }

  private boolean selectThenInsert(Connection c, String eventId, String consumer)
      throws SQLException {
    try (PreparedStatement ps =
        c.prepareStatement("SELECT 1 FROM " + table + " WHERE event_id = ? AND consumer = ?")) {
      ps.setString(1, eventId);
      ps.setString(2, consumer);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return false;
        }
      }
    }
    try (PreparedStatement ps =
        c.prepareStatement(
            "INSERT INTO " + table + " (event_id, consumer, processed_at) VALUES (?, ?, ?)")) {
      bind(ps, eventId, consumer);
      ps.executeUpdate();
      return true;
    }
  }

  private void bind(PreparedStatement ps, String eventId, String consumer) throws SQLException {
    ps.setString(1, eventId);
    ps.setString(2, consumer);
    ps.setObject(3, OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.UTC));
  }
}
