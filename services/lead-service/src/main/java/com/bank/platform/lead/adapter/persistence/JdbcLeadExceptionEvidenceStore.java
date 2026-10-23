package com.bank.platform.lead.adapter.persistence;

import com.bank.platform.lead.adapter.crypto.FieldCipher;
import com.bank.platform.lead.domain.ExceptionCheckpoint;
import com.bank.platform.lead.domain.ExceptionOutcome;
import com.bank.platform.lead.domain.ExceptionRulesPort.Evaluation;
import com.bank.platform.lead.domain.ExceptionRulesPort.FiredRule;
import com.bank.platform.lead.domain.LeadExceptionEvidencePort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * {@code lead_exception_evaluation} and {@code lead_exception_decision} (Flyway V6; design DDL
 * {@code 04-lead_lms.sql}). Insert-only: there is no update or delete statement here, and on
 * PostgreSQL a trigger (vendor V7) refuses both. JSON columns are text so the same migration runs
 * on H2, as V3 / V4 do. Decision remarks are RESTRICTED and stored AES-256-GCM encrypted.
 */
@Repository
@ConditionalOnProperty(name = "lead.store", havingValue = "jdbc", matchIfMissing = true)
public class JdbcLeadExceptionEvidenceStore implements LeadExceptionEvidencePort {

  private static final TypeReference<List<String>> STRINGS = new TypeReference<>() {};
  private static final TypeReference<List<FiredRule>> FIRED = new TypeReference<>() {};

  private final NamedParameterJdbcTemplate jdbc;
  private final FieldCipher cipher;
  private final ObjectMapper json;

  public JdbcLeadExceptionEvidenceStore(
      NamedParameterJdbcTemplate jdbc, FieldCipher cipher, ObjectMapper json) {
    this.jdbc = jdbc;
    this.cipher = cipher;
    this.json = json;
  }

  @Override
  public void recordEvaluation(StoredEvaluation stored) {
    Evaluation e = stored.evaluation();
    jdbc.update(
        "INSERT INTO lead_lms.lead_exception_evaluation (evaluation_id, lead_id, checkpoint,"
            + " rule_set_id, rule_set_version, engine_version, inputs_hash, outcome, reason_codes,"
            + " fired_rules, messages, evidence, evaluated_by, evaluated_at) VALUES (:id, :leadId,"
            + " :checkpoint, :ruleSetId, :version, :engine, :hash, :outcome, :reasons, :fired,"
            + " :messages, :evidence, :by, :at)",
        new MapSqlParameterSource()
            .addValue("id", stored.evaluationId())
            .addValue("leadId", stored.leadId())
            .addValue("checkpoint", stored.checkpoint().name())
            .addValue("ruleSetId", e.ruleSetId())
            .addValue("version", e.ruleSetVersion())
            .addValue("engine", e.engineVersion())
            .addValue("hash", e.inputsHash())
            .addValue("outcome", e.outcome().name())
            .addValue("reasons", write(e.reasonCodes()))
            .addValue("fired", write(e.fired()))
            .addValue("messages", write(e.messages()))
            .addValue("evidence", e.evidenceJson())
            .addValue("by", stored.evaluatedBy())
            .addValue("at", Timestamp.from(stored.evaluatedAt())));
  }

  @Override
  public Optional<StoredEvaluation> findEvaluation(String evaluationId) {
    return jdbc
        .query(
            "SELECT evaluation_id, lead_id, checkpoint, rule_set_id, rule_set_version,"
                + " engine_version, inputs_hash, outcome, reason_codes, fired_rules, messages,"
                + " evidence, evaluated_by, evaluated_at FROM lead_lms.lead_exception_evaluation"
                + " WHERE evaluation_id = :id",
            Map.of("id", evaluationId),
            this::mapEvaluation)
        .stream()
        .findFirst();
  }

  @Override
  public void recordDecision(Decision d) {
    jdbc.update(
        "INSERT INTO lead_lms.lead_exception_decision (decision_id, evaluation_id, lead_id,"
            + " decision, exception_ref, remarks_enc, encryption_key_id, decided_by,"
            + " decided_by_role, decided_at) VALUES (:id, :evaluationId, :leadId, :decision, :ref,"
            + " :remarks, :keyId, :by, :role, :at)",
        new MapSqlParameterSource()
            .addValue("id", d.decisionId())
            .addValue("evaluationId", d.evaluationId())
            .addValue("leadId", d.leadId())
            .addValue("decision", d.decision().name())
            .addValue("ref", d.decisionRef())
            .addValue("remarks", cipher.encrypt(d.remarks()))
            .addValue("keyId", cipher.keyId())
            .addValue("by", d.decidedBy())
            .addValue("role", d.decidedByRole())
            .addValue("at", Timestamp.from(d.decidedAt())));
  }

  @Override
  public List<Decision> decisionsOf(String leadId) {
    return jdbc.query(
        "SELECT decision_id, evaluation_id, lead_id, decision, exception_ref, remarks_enc,"
            + " decided_by, decided_by_role, decided_at FROM lead_lms.lead_exception_decision"
            + " WHERE lead_id = :leadId ORDER BY decided_at, decision_id",
        Map.of("leadId", leadId),
        (rs, n) ->
            new Decision(
                rs.getString("decision_id").trim(),
                rs.getString("evaluation_id").trim(),
                rs.getString("lead_id").trim(),
                DecisionKind.valueOf(rs.getString("decision")),
                rs.getString("exception_ref"),
                cipher.decrypt(rs.getBytes("remarks_enc")),
                rs.getString("decided_by"),
                rs.getString("decided_by_role"),
                rs.getTimestamp("decided_at").toInstant()));
  }

  private StoredEvaluation mapEvaluation(ResultSet rs, int row) throws SQLException {
    Evaluation e =
        new Evaluation(
            ExceptionOutcome.valueOf(rs.getString("outcome")),
            read(rs.getString("reason_codes"), STRINGS),
            rs.getString("rule_set_id"),
            rs.getString("rule_set_version"),
            rs.getString("engine_version"),
            rs.getString("inputs_hash"),
            read(rs.getString("fired_rules"), FIRED),
            read(rs.getString("messages"), STRINGS),
            rs.getString("evidence"));
    return new StoredEvaluation(
        rs.getString("evaluation_id").trim(),
        rs.getString("lead_id").trim(),
        ExceptionCheckpoint.valueOf(rs.getString("checkpoint")),
        e,
        rs.getString("evaluated_by"),
        rs.getTimestamp("evaluated_at").toInstant());
  }

  private String write(Object value) {
    try {
      return json.writeValueAsString(value);
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException("exception evidence is not serialisable", ex);
    }
  }

  private <T> T read(String value, TypeReference<T> type) {
    try {
      return json.readValue(value, type);
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException("exception evidence is not readable", ex);
    }
  }
}
