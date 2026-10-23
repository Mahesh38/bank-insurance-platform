package com.bank.platform.lead.adapter.persistence;

import com.bank.platform.lead.domain.ConcurrentLeadUpdateException;
import com.bank.platform.lead.domain.LeadIdempotencyPort;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/** {@code lead_lms.idempotency_record}. An expired record is treated as absent and replaced. */
@Repository
@ConditionalOnProperty(name = "lead.store", havingValue = "jdbc", matchIfMissing = true)
public class JdbcLeadIdempotencyStore implements LeadIdempotencyPort {

  private final NamedParameterJdbcTemplate jdbc;

  public JdbcLeadIdempotencyStore(NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public Optional<IdempotencyRecord> find(String idempotencyKey, Instant now) {
    return jdbc
        .query(
            "SELECT idempotency_key, request_hash, response_ref, created_at, expires_at FROM"
                + " lead_lms.idempotency_record WHERE idempotency_key = :key AND expires_at > :now",
            Map.of("key", idempotencyKey, "now", Timestamp.from(now)),
            (rs, n) ->
                new IdempotencyRecord(
                    rs.getString("idempotency_key"),
                    rs.getString("request_hash"),
                    rs.getString("response_ref"),
                    rs.getTimestamp("created_at").toInstant(),
                    rs.getTimestamp("expires_at").toInstant()))
        .stream()
        .findFirst();
  }

  @Override
  public void save(IdempotencyRecord record) {
    MapSqlParameterSource params =
        new MapSqlParameterSource()
            .addValue("key", record.idempotencyKey())
            .addValue("hash", record.requestHash())
            .addValue("ref", record.responseRef())
            .addValue("createdAt", Timestamp.from(record.createdAt()))
            .addValue("expiresAt", Timestamp.from(record.expiresAt()));
    // An expired row for the same key may still be there until the purge runs.
    jdbc.update(
        "DELETE FROM lead_lms.idempotency_record WHERE idempotency_key = :key"
            + " AND expires_at <= :createdAt",
        params);
    try {
      jdbc.update(
          "INSERT INTO lead_lms.idempotency_record (idempotency_key, request_hash, response_ref,"
              + " created_at, expires_at) VALUES (:key, :hash, :ref, :createdAt, :expiresAt)",
          params);
    } catch (DuplicateKeyException ex) {
      throw new ConcurrentLeadUpdateException(record.responseRef());
    }
  }
}
