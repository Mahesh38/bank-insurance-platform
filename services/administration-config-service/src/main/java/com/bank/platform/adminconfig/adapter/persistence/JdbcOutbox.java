package com.bank.platform.adminconfig.adapter.persistence;

import com.bank.platform.adminconfig.domain.OutboxPort;
import java.sql.Timestamp;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * {@code administration.outbox_event} (ADR-025 point 3). Relayed by the shared outbox relay; this
 * service only appends, inside the publishing transaction.
 */
@Repository
public class JdbcOutbox implements OutboxPort {

  private final NamedParameterJdbcTemplate jdbc;

  public JdbcOutbox(NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public void append(OutboxEvent e) {
    jdbc.update(
        "INSERT INTO administration.outbox_event (outbox_id, aggregate_type, aggregate_id,"
            + " event_type, payload, created_at, attempts) VALUES (:id, :aggregateType,"
            + " :aggregateId, :eventType, :payload, :createdAt, 0)",
        new MapSqlParameterSource()
            .addValue("id", e.outboxId())
            .addValue("aggregateType", e.aggregateType())
            .addValue("aggregateId", e.aggregateId())
            .addValue("eventType", e.eventType())
            .addValue("payload", e.payload())
            .addValue("createdAt", Timestamp.from(e.createdAt())));
  }
}
