package com.bank.platform.lead.adapter.persistence;

import com.bank.platform.lead.adapter.crypto.FieldCipher;
import com.bank.platform.lead.domain.LeadActivityPort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Assignment history, meeting, note and outbox rows (Flyway {@code V3__lead_lms_activity.sql}).
 * Writes join the caller's transaction, so they commit or roll back with the lead row.
 *
 * <p>Nothing publishes the outbox yet: there is no MSK in R0 code. Rows wait with {@code
 * published_at IS NULL} for the outbox publisher (S-23).
 */
@Repository
@ConditionalOnProperty(name = "lead.store", havingValue = "jdbc", matchIfMissing = true)
public class JdbcLeadActivityStore implements LeadActivityPort {

  private static final TypeReference<Map<String, String>> ATTRIBUTES = new TypeReference<>() {};

  private final NamedParameterJdbcTemplate jdbc;
  private final FieldCipher cipher;
  private final ObjectMapper json;

  public JdbcLeadActivityStore(
      NamedParameterJdbcTemplate jdbc, FieldCipher cipher, ObjectMapper json) {
    this.jdbc = jdbc;
    this.cipher = cipher;
    this.json = json;
  }

  @Override
  public void recordAssignment(Assignment a) {
    jdbc.update(
        "INSERT INTO lead_lms.lead_assignment (assignment_id, lead_id, rm_id, assigned_sp_id,"
            + " assigned_at, assigned_by, assignment_kind) VALUES (:id, :leadId, :rmId, :spId,"
            + " :at, :by, :kind)",
        new MapSqlParameterSource()
            .addValue("id", a.assignmentId())
            .addValue("leadId", a.leadId())
            .addValue("rmId", a.rmId())
            .addValue("spId", a.spId())
            .addValue("at", Timestamp.from(a.assignedAt()))
            .addValue("by", a.assignedBy())
            .addValue("kind", a.kind().name()));
  }

  @Override
  public void recordMeeting(Meeting m) {
    boolean hasLink = m.link() != null;
    jdbc.update(
        "INSERT INTO lead_lms.lead_meeting (meeting_id, lead_id, meeting_type, scheduled_at,"
            + " meeting_link_enc, encryption_key_id, created_by, created_at) VALUES (:id, :leadId,"
            + " :type, :at, :link, :keyId, :by, :createdAt)",
        new MapSqlParameterSource()
            .addValue("id", m.meetingId())
            .addValue("leadId", m.leadId())
            .addValue("type", m.type().name())
            .addValue("at", Timestamp.from(m.scheduledAt()))
            .addValue("link", hasLink ? cipher.encrypt(m.link()) : null)
            .addValue("keyId", hasLink ? cipher.keyId() : null)
            .addValue("by", m.createdBy())
            .addValue("createdAt", Timestamp.from(m.createdAt())));
  }

  @Override
  public void recordFollowUp(FollowUp f) {
    jdbc.update(
        "INSERT INTO lead_lms.lead_follow_up (follow_up_id, lead_id, note_enc, encryption_key_id,"
            + " created_by, created_at) VALUES (:id, :leadId, :note, :keyId, :by, :at)",
        new MapSqlParameterSource()
            .addValue("id", f.followUpId())
            .addValue("leadId", f.leadId())
            .addValue("note", cipher.encrypt(f.note()))
            .addValue("keyId", cipher.keyId())
            .addValue("by", f.createdBy())
            .addValue("at", Timestamp.from(f.createdAt())));
  }

  @Override
  public void append(LeadEvent e) {
    jdbc.update(
        "INSERT INTO lead_lms.outbox_event (outbox_id, aggregate_type, aggregate_id, event_type,"
            + " payload, created_at) VALUES (:id, 'lead', :leadId, :type, :payload, :at)",
        new MapSqlParameterSource()
            .addValue("id", e.eventId())
            .addValue("leadId", e.leadId())
            .addValue("type", e.eventType())
            .addValue("payload", write(e.attributes()))
            .addValue("at", Timestamp.from(e.occurredAt())));
  }

  @Override
  public List<Assignment> assignmentsOf(String leadId) {
    return jdbc.query(
        "SELECT assignment_id, lead_id, rm_id, assigned_sp_id, assigned_at, assigned_by,"
            + " assignment_kind FROM lead_lms.lead_assignment WHERE lead_id = :leadId"
            + " ORDER BY assigned_at, assignment_id",
        Map.of("leadId", leadId),
        (rs, n) ->
            new Assignment(
                rs.getString("assignment_id"),
                rs.getString("lead_id"),
                rs.getString("rm_id"),
                rs.getString("assigned_sp_id"),
                rs.getTimestamp("assigned_at").toInstant(),
                rs.getString("assigned_by"),
                AssignmentKind.valueOf(rs.getString("assignment_kind"))));
  }

  @Override
  public List<LeadEvent> eventsOf(String leadId) {
    return jdbc.query(
        "SELECT outbox_id, aggregate_id, event_type, payload, created_at FROM"
            + " lead_lms.outbox_event WHERE aggregate_id = :leadId ORDER BY created_at, outbox_id",
        Map.of("leadId", leadId),
        (rs, n) ->
            new LeadEvent(
                rs.getString("outbox_id"),
                rs.getString("aggregate_id"),
                rs.getString("event_type"),
                read(rs.getString("payload")),
                rs.getTimestamp("created_at").toInstant()));
  }

  private String write(Map<String, String> attributes) {
    try {
      return json.writeValueAsString(attributes);
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException("outbox payload is not serialisable", ex);
    }
  }

  private Map<String, String> read(String payload) {
    try {
      return json.readValue(payload, ATTRIBUTES);
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException("outbox payload is not readable", ex);
    }
  }
}
