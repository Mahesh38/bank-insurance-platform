package com.bank.platform.lead.adapter.persistence;

import com.bank.platform.lead.domain.ConcurrentLeadUpdateException;
import com.bank.platform.lead.domain.ExceptionOutcome;
import com.bank.platform.lead.domain.Lead;
import com.bank.platform.lead.domain.LeadRepository;
import com.bank.platform.lead.domain.LeadState;
import com.bank.platform.lead.domain.ProductClass;
import com.bank.platform.lead.domain.ReportingClass;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Lead store on schema {@code lead_lms} (Flyway {@code V2__lead_lms_lead.sql}). The default store;
 * {@code lead.store=memory} selects the in-memory one for isolated tests.
 *
 * <p>Updates are optimistic: the row's {@code version} must still be the version the lead was read
 * at, otherwise {@link ConcurrentLeadUpdateException}. That is what stops two assignment requests
 * from both writing {@code accountable_sp_id} (INV-ACT-03).
 */
@Repository
@ConditionalOnProperty(name = "lead.store", havingValue = "jdbc", matchIfMissing = true)
public class JdbcLeadRepository implements LeadRepository {

  private static final String COLUMNS =
      "lead_id, customer_id, lob, product_class, branch_id, created_by_principal_id, state,"
          + " assigned_rm_id, assigned_sp_id, accountable_sp_id, exception_hold, exception_outcome,"
          + " bi_generated, reporting_class, journey_id, created_at, updated_at, expires_at, version";

  private final NamedParameterJdbcTemplate jdbc;

  public JdbcLeadRepository(NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public void save(Lead lead) {
    Lead.Snapshot s = lead.snapshot();
    MapSqlParameterSource params = params(s);
    if (s.version() == 0) {
      try {
        jdbc.update(
            "INSERT INTO lead_lms.lead ("
                + COLUMNS
                + ") VALUES (:leadId, :customerId, :lob,"
                + " :productClass, :branchId, :createdBy, :state, :assignedRmId, :assignedSpId,"
                + " :accountableSpId, :exceptionHold, :exceptionOutcome, :biGenerated,"
                + " :reportingClass, :journeyId, :createdAt, :updatedAt, :expiresAt, 1)",
            params);
      } catch (DuplicateKeyException ex) {
        // ux_lead_dedupe_open (vendor V5): a concurrent create won the dedupe race.
        throw new ConcurrentLeadUpdateException(s.leadId());
      }
      lead.markPersisted(1);
      return;
    }
    int updated =
        jdbc.update(
            "UPDATE lead_lms.lead SET state = :state, assigned_rm_id = :assignedRmId,"
                + " assigned_sp_id = :assignedSpId, accountable_sp_id = :accountableSpId,"
                + " exception_hold = :exceptionHold, exception_outcome = :exceptionOutcome,"
                + " bi_generated = :biGenerated, reporting_class = :reportingClass,"
                + " journey_id = :journeyId, updated_at = :updatedAt, version = version + 1"
                + " WHERE lead_id = :leadId AND version = :version",
            params.addValue("version", s.version()));
    if (updated != 1) {
      throw new ConcurrentLeadUpdateException(s.leadId());
    }
    lead.markPersisted(s.version() + 1);
  }

  @Override
  public Optional<Lead> findById(String leadId) {
    return jdbc
        .query(
            "SELECT " + COLUMNS + " FROM lead_lms.lead WHERE lead_id = :leadId",
            new MapSqlParameterSource("leadId", leadId),
            JdbcLeadRepository::map)
        .stream()
        .findFirst();
  }

  @Override
  public List<Lead> findVisibleTo(String principalId) {
    return jdbc.query(
        "SELECT "
            + COLUMNS
            + " FROM lead_lms.lead WHERE created_by_principal_id = :p"
            + " OR assigned_rm_id = :p OR assigned_sp_id = :p ORDER BY updated_at DESC",
        new MapSqlParameterSource("p", principalId),
        JdbcLeadRepository::map);
  }

  @Override
  public Optional<Lead> findUnfinished(
      String creatorPrincipalId, String customerId, ProductClass productClass) {
    return jdbc
        .query(
            "SELECT "
                + COLUMNS
                + " FROM lead_lms.lead WHERE created_by_principal_id = :creator"
                + " AND customer_id = :customerId AND product_class = :productClass"
                + " AND bi_generated = FALSE"
                + " AND state NOT IN ('DISQUALIFIED', 'CONVERTED', 'ARCHIVED', 'EXPIRED')"
                + " ORDER BY created_at",
            new MapSqlParameterSource("creator", creatorPrincipalId)
                .addValue("customerId", customerId)
                .addValue("productClass", productClass.name()),
            JdbcLeadRepository::map)
        .stream()
        .findFirst();
  }

  private static MapSqlParameterSource params(Lead.Snapshot s) {
    return new MapSqlParameterSource()
        .addValue("leadId", s.leadId())
        .addValue("customerId", s.customerId())
        .addValue("lob", s.lob())
        .addValue("productClass", s.productClass().name())
        .addValue("branchId", s.branchId())
        .addValue("createdBy", s.createdByPrincipalId())
        .addValue("state", s.state().name())
        .addValue("assignedRmId", s.assignedRmId())
        .addValue("assignedSpId", s.assignedSpId())
        .addValue("accountableSpId", s.accountableSpId())
        .addValue("exceptionHold", s.exceptionHold())
        .addValue("exceptionOutcome", s.exceptionOutcome().name())
        .addValue("biGenerated", s.biGenerated())
        .addValue("reportingClass", s.reportingClass().name())
        .addValue("journeyId", s.journeyId())
        .addValue("createdAt", Timestamp.from(s.createdAt()))
        .addValue("updatedAt", Timestamp.from(s.updatedAt()))
        .addValue(
            "expiresAt",
            s.expiresAt() == null ? null : Timestamp.from(s.expiresAt()),
            Types.TIMESTAMP);
  }

  private static Lead map(ResultSet rs, int row) throws SQLException {
    return Lead.restore(
        new Lead.Snapshot(
            rs.getString("lead_id").trim(),
            rs.getString("customer_id"),
            rs.getString("lob"),
            ProductClass.valueOf(rs.getString("product_class")),
            rs.getString("branch_id"),
            rs.getString("created_by_principal_id"),
            instant(rs, "created_at"),
            LeadState.valueOf(rs.getString("state")),
            rs.getString("assigned_rm_id"),
            rs.getString("assigned_sp_id"),
            rs.getString("accountable_sp_id"),
            rs.getBoolean("exception_hold"),
            ExceptionOutcome.valueOf(rs.getString("exception_outcome")),
            rs.getBoolean("bi_generated"),
            ReportingClass.valueOf(rs.getString("reporting_class")),
            trimmed(rs.getString("journey_id")),
            instant(rs, "updated_at"),
            nullableInstant(rs, "expires_at"),
            rs.getLong("version")));
  }

  private static Instant instant(ResultSet rs, String column) throws SQLException {
    return rs.getTimestamp(column).toInstant();
  }

  private static Instant nullableInstant(ResultSet rs, String column) throws SQLException {
    Timestamp value = rs.getTimestamp(column);
    return value == null ? null : value.toInstant();
  }

  private static String trimmed(String value) {
    return value == null ? null : value.trim();
  }
}
