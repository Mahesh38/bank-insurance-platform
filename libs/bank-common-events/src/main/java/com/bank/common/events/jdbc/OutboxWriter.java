package com.bank.common.events.jdbc;

import com.bank.common.events.EventEnvelope;
import com.bank.common.events.EventJsonCodec;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;

/**
 * Inserts one {@code <schema>.outbox_event} row in the <b>caller's</b> transaction (ADR-025 §3:
 * transactional outbox only — never a direct send in a request). {@code outbox_id} = {@code
 * eventId}; {@code payload} = the whole {@code event-envelope/v1} JSON, so the relay publishes
 * exactly what was committed. Expected DDL: see CATALOGUE.md "Postgres outbox".
 */
public final class OutboxWriter {

  private final String table;
  private final EventJsonCodec codec;
  private final Clock clock;
  private final ConnectionProvider connections;
  private volatile SqlDialect dialect;

  public OutboxWriter(String schema, EventJsonCodec codec, Clock clock) {
    this(schema, codec, clock, null, null);
  }

  /**
   * @param connections supplies the transaction's connection for {@link #write(EventEnvelope)}; may
   *     be null when only {@link #write(Connection, EventEnvelope)} is used
   * @param dialect null to detect from the first connection
   */
  public OutboxWriter(
      String schema,
      EventJsonCodec codec,
      Clock clock,
      ConnectionProvider connections,
      SqlDialect dialect) {
    this.table = SqlDialect.requireSchema(schema) + ".outbox_event";
    this.codec = Objects.requireNonNull(codec);
    this.clock = Objects.requireNonNull(clock);
    this.connections = connections;
    this.dialect = dialect;
  }

  public void write(EventEnvelope event) {
    if (connections == null) {
      throw new IllegalStateException("no ConnectionProvider configured; pass the Connection");
    }
    Connection connection = null;
    try {
      connection = connections.acquire();
      write(connection, event);
    } catch (SQLException ex) {
      throw new OutboxException("outbox insert failed for " + event.eventId(), ex);
    } finally {
      if (connection != null) {
        connections.release(connection);
      }
    }
  }

  public void write(Connection connection, EventEnvelope event) {
    try {
      SqlDialect d = dialect(connection);
      String sql =
          "INSERT INTO "
              + table
              + " (outbox_id, aggregate_type, aggregate_id, event_type, payload, created_at,"
              + " attempts) VALUES (?, ?, ?, ?, "
              + d.payloadPlaceholder()
              + ", ?, 0)";
      try (PreparedStatement ps = connection.prepareStatement(sql)) {
        ps.setString(1, event.eventId());
        ps.setString(2, event.aggregateType());
        ps.setString(3, event.aggregateId());
        ps.setString(4, event.eventType());
        ps.setString(5, codec.encode(event));
        ps.setObject(6, OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.UTC));
        ps.executeUpdate();
      }
    } catch (SQLException ex) {
      throw new OutboxException("outbox insert failed for " + event.eventId(), ex);
    }
  }

  private SqlDialect dialect(Connection connection) throws SQLException {
    SqlDialect d = dialect;
    if (d == null) {
      d = SqlDialect.detect(connection);
      dialect = d;
    }
    return d;
  }
}
