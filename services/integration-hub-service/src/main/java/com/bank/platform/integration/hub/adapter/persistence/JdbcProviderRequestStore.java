package com.bank.platform.integration.hub.adapter.persistence;

import com.bank.platform.integration.hub.domain.CallerRef;
import com.bank.platform.integration.hub.domain.CanonicalState;
import com.bank.platform.integration.hub.domain.Failure;
import com.bank.platform.integration.hub.domain.ProviderRequest;
import com.bank.platform.integration.hub.domain.ProviderRequestStore;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcProviderRequestStore implements ProviderRequestStore {

  private final JdbcTemplate jdbc;

  public JdbcProviderRequestStore(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public Optional<ProviderRequest> findByIdempotencyKey(String idempotencyKey) {
    try {
      return Optional.ofNullable(
          jdbc.queryForObject(
              """
              SELECT provider_request_id, idempotency_key, request_hash, operation, lob,
                     product_class, mode, caller_context, caller_aggregate_id, journey_id,
                     provider, routing_version, adapter_job_id, state, failure_code,
                     failure_reason, created_at, updated_at
              FROM hub.provider_request WHERE idempotency_key = ?
              """,
              (rs, rowNum) -> mapRow(rs),
              idempotencyKey));
    } catch (EmptyResultDataAccessException ex) {
      return Optional.empty();
    }
  }

  @Override
  public Optional<ProviderRequest> findById(String providerRequestId) {
    try {
      return Optional.ofNullable(
          jdbc.queryForObject(
              """
              SELECT provider_request_id, idempotency_key, request_hash, operation, lob,
                     product_class, mode, caller_context, caller_aggregate_id, journey_id,
                     provider, routing_version, adapter_job_id, state, failure_code,
                     failure_reason, created_at, updated_at
              FROM hub.provider_request WHERE provider_request_id = ?
              """,
              (rs, rowNum) -> mapRow(rs),
              providerRequestId));
    } catch (EmptyResultDataAccessException ex) {
      return Optional.empty();
    }
  }

  @Override
  public void save(ProviderRequest request) {
    int updated =
        jdbc.update(
            """
            UPDATE hub.provider_request SET
              request_hash = ?, adapter_job_id = ?, state = ?, failure_code = ?,
              failure_reason = ?, updated_at = ?
            WHERE provider_request_id = ?
            """,
            request.requestHash(),
            request.adapterJobId(),
            request.state().name(),
            request.failure() == null ? null : request.failure().code(),
            request.failure() == null ? null : request.failure().reason(),
            Timestamp.from(request.updatedAt()),
            request.providerRequestId());
    if (updated == 0) {
      jdbc.update(
          """
          INSERT INTO hub.provider_request (
            provider_request_id, idempotency_key, request_hash, operation, lob, product_class,
            mode, caller_context, caller_aggregate_id, journey_id, provider, routing_version,
            adapter_job_id, state, failure_code, failure_reason, created_at, updated_at
          ) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
          """,
          request.providerRequestId(),
          request.idempotencyKey(),
          request.requestHash(),
          request.operation(),
          request.lob(),
          request.productClass(),
          request.mode(),
          request.callerRef().context(),
          request.callerRef().aggregateId(),
          request.journeyId(),
          request.provider(),
          request.routingVersion(),
          request.adapterJobId(),
          request.state().name(),
          request.failure() == null ? null : request.failure().code(),
          request.failure() == null ? null : request.failure().reason(),
          Timestamp.from(request.createdAt()),
          Timestamp.from(request.updatedAt()));
    }
  }

  private static ProviderRequest mapRow(java.sql.ResultSet rs) throws java.sql.SQLException {
    String failureCode = rs.getString("failure_code");
    Failure failure =
        failureCode == null ? null : new Failure(failureCode, rs.getString("failure_reason"));
    Instant created = rs.getTimestamp("created_at").toInstant();
    Instant updated = rs.getTimestamp("updated_at").toInstant();
    return ProviderRequest.rehydrate(
        rs.getString("provider_request_id"),
        rs.getString("idempotency_key"),
        rs.getString("request_hash"),
        rs.getString("operation"),
        rs.getString("lob"),
        rs.getString("product_class"),
        rs.getString("mode"),
        new CallerRef(rs.getString("caller_context"), rs.getString("caller_aggregate_id")),
        rs.getString("journey_id"),
        rs.getString("provider"),
        rs.getString("routing_version"),
        created,
        updated,
        CanonicalState.valueOf(rs.getString("state")),
        rs.getString("adapter_job_id"),
        failure);
  }
}
