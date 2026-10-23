package com.bank.platform.adminconfig.adapter.persistence;

import com.bank.platform.adminconfig.domain.RuleSetLifecycleException;
import com.bank.platform.adminconfig.domain.RuleSetVersion;
import com.bank.platform.adminconfig.domain.RuleSetVersionRepository;
import com.bank.platform.adminconfig.domain.RuleSetVersionStatus;
import com.bank.rules.engine.RuleSet;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * {@code administration.rule_set_version} (Flyway V2). A PUBLISHED row is never updated: every
 * UPDATE carries {@code status <> 'PUBLISHED'} as well as the expected prior status, and on
 * PostgreSQL a trigger (db/vendor/postgresql/V3) refuses it independently of this code.
 */
@Repository
public class JdbcRuleSetVersionRepository implements RuleSetVersionRepository {

  private static final String COLUMNS =
      "id, rule_set_id, version, domain, scope_lob, scope_product_class, classification, status,"
          + " document, document_sha256, effective_from, effective_to, maker, checker,"
          + " approved_at, decision_remarks, created_at";

  private final NamedParameterJdbcTemplate jdbc;

  public JdbcRuleSetVersionRepository(NamedParameterJdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public void insert(RuleSetVersion version) {
    try {
      jdbc.update(
          "INSERT INTO administration.rule_set_version ("
              + COLUMNS
              + ") VALUES (:id, :ruleSetId, :version, :domain, :scopeLob, :scopeProductClass,"
              + " :classification, :status, :document, :documentSha256, :effectiveFrom,"
              + " :effectiveTo, :maker, :checker, :approvedAt, :decisionRemarks, :createdAt)",
          params(version.snapshot()));
    } catch (DuplicateKeyException e) {
      throw new DuplicateVersionException(
          version.ruleSetId() + "@" + version.version() + " already exists");
    }
  }

  @Override
  public void update(RuleSetVersion version, RuleSetVersionStatus expectedStatus) {
    int rows =
        jdbc.update(
            "UPDATE administration.rule_set_version SET domain = :domain, scope_lob = :scopeLob,"
                + " scope_product_class = :scopeProductClass, classification = :classification,"
                + " status = :status, document = :document, document_sha256 = :documentSha256,"
                + " effective_from = :effectiveFrom, effective_to = :effectiveTo, maker = :maker,"
                + " checker = :checker, approved_at = :approvedAt,"
                + " decision_remarks = :decisionRemarks"
                + " WHERE id = :id AND status = :expected AND status <> 'PUBLISHED'",
            params(version.snapshot()).addValue("expected", expectedStatus.name()));
    if (rows != 1) {
      throw new RuleSetLifecycleException(
          RuleSetLifecycleException.Kind.ILLEGAL_TRANSITION,
          version.ruleSetId()
              + "@"
              + version.version()
              + " changed concurrently or is published (immutable)");
    }
  }

  @Override
  public Optional<RuleSetVersion> find(String ruleSetId, String version) {
    return jdbc
        .query(
            "SELECT "
                + COLUMNS
                + " FROM administration.rule_set_version"
                + " WHERE rule_set_id = :r AND version = :v",
            new MapSqlParameterSource("r", ruleSetId).addValue("v", version),
            JdbcRuleSetVersionRepository::map)
        .stream()
        .findFirst();
  }

  @Override
  public List<RuleSetVersion> listVersions(String ruleSetId) {
    return jdbc.query(
        "SELECT "
            + COLUMNS
            + " FROM administration.rule_set_version"
            + " WHERE rule_set_id = :r ORDER BY created_at, version",
        new MapSqlParameterSource("r", ruleSetId),
        JdbcRuleSetVersionRepository::map);
  }

  @Override
  public List<RuleSetVersion> listPublished(String domain) {
    return jdbc.query(
        "SELECT "
            + COLUMNS
            + " FROM administration.rule_set_version"
            + " WHERE domain = :d AND status = 'PUBLISHED'",
        new MapSqlParameterSource("d", domain),
        JdbcRuleSetVersionRepository::map);
  }

  private static MapSqlParameterSource params(RuleSetVersion.Snapshot s) {
    return new MapSqlParameterSource()
        .addValue("id", s.id())
        .addValue("ruleSetId", s.ruleSetId())
        .addValue("version", s.version())
        .addValue("domain", s.domain())
        .addValue("scopeLob", s.scopeLob())
        .addValue("scopeProductClass", s.scopeProductClass())
        .addValue("classification", s.classification().name())
        .addValue("status", s.status().name())
        .addValue("document", s.document())
        .addValue("documentSha256", s.documentSha256())
        .addValue("effectiveFrom", ts(s.effectiveFrom().toInstant()))
        .addValue("effectiveTo", s.effectiveTo() == null ? null : ts(s.effectiveTo().toInstant()))
        .addValue("maker", s.maker())
        .addValue("checker", s.checker())
        .addValue("approvedAt", s.approvedAt() == null ? null : ts(s.approvedAt()))
        .addValue("decisionRemarks", s.decisionRemarks())
        .addValue("createdAt", ts(s.createdAt()));
  }

  private static Timestamp ts(Instant i) {
    return Timestamp.from(i);
  }

  private static OffsetDateTime odt(Timestamp t) {
    return t == null ? null : t.toInstant().atOffset(ZoneOffset.UTC);
  }

  private static RuleSetVersion map(ResultSet rs, int row) throws SQLException {
    Timestamp approved = rs.getTimestamp("approved_at");
    return RuleSetVersion.restore(
        new RuleSetVersion.Snapshot(
            rs.getString("id"),
            rs.getString("rule_set_id"),
            rs.getString("version"),
            rs.getString("domain"),
            rs.getString("scope_lob"),
            rs.getString("scope_product_class"),
            RuleSet.Classification.valueOf(rs.getString("classification")),
            RuleSetVersionStatus.valueOf(rs.getString("status")),
            rs.getString("document"),
            rs.getString("document_sha256"),
            odt(rs.getTimestamp("effective_from")),
            odt(rs.getTimestamp("effective_to")),
            rs.getString("maker"),
            rs.getString("checker"),
            approved == null ? null : approved.toInstant(),
            rs.getString("decision_remarks"),
            rs.getTimestamp("created_at").toInstant()));
  }
}
