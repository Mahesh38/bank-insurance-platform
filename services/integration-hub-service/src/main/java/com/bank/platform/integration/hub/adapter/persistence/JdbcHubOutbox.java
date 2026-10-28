package com.bank.platform.integration.hub.adapter.persistence;

import com.bank.platform.integration.hub.domain.HubOutbox;
import com.bank.platform.integration.hub.domain.Ulid;
import java.sql.Timestamp;
import java.time.Clock;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcHubOutbox implements HubOutbox {

  private final JdbcTemplate jdbc;
  private final Clock clock;

  public JdbcHubOutbox(JdbcTemplate jdbc, Clock clock) {
    this.jdbc = jdbc;
    this.clock = clock;
  }

  @Override
  public void append(
      String aggregateType,
      String aggregateId,
      String eventType,
      String schemaVersion,
      String payloadJson) {
    jdbc.update(
        """
        INSERT INTO hub.outbox_event (
          outbox_id, aggregate_type, aggregate_id, event_type, schema_version, payload_json, created_at
        ) VALUES (?,?,?,?,?,?,?)
        """,
        Ulid.mint(clock),
        aggregateType,
        aggregateId,
        eventType,
        schemaVersion,
        payloadJson,
        Timestamp.from(clock.instant()));
  }
}
