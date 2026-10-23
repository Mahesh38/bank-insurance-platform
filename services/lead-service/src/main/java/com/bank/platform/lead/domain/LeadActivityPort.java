package com.bank.platform.lead.domain;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Everything a lead command writes besides the lead row, in the same transaction ({@code
 * 04-lead_lms.sql}): assignment history, the meeting and note captured at assignment (stored
 * encrypted), and the outbox event other services consume.
 */
public interface LeadActivityPort {

  void recordAssignment(Assignment assignment);

  void recordMeeting(Meeting meeting);

  void recordFollowUp(FollowUp followUp);

  void append(LeadEvent event);

  List<Assignment> assignmentsOf(String leadId);

  List<LeadEvent> eventsOf(String leadId);

  enum AssignmentKind {
    CREATE_ASSIGN,
    REASSIGN
  }

  enum MeetingType {
    ONLINE,
    IN_PERSON
  }

  record Assignment(
      String assignmentId,
      String leadId,
      String rmId,
      String spId,
      Instant assignedAt,
      String assignedBy,
      AssignmentKind kind) {}

  /** {@code link} is plaintext here; the store encrypts it ({@code meeting_link_enc}). */
  record Meeting(
      String meetingId,
      String leadId,
      MeetingType type,
      Instant scheduledAt,
      String link,
      String createdBy,
      Instant createdAt) {}

  /** {@code note} is plaintext here; the store encrypts it ({@code note_enc}). */
  record FollowUp(
      String followUpId, String leadId, String note, String createdBy, Instant createdAt) {}

  /** Ids and states only — never customer PII (outbox payload rule, 06 §5). */
  record LeadEvent(
      String eventId,
      String leadId,
      String eventType,
      Map<String, String> attributes,
      Instant occurredAt) {}
}
