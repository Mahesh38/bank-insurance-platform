package com.bank.workforce.bff.lead;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;
import java.util.List;

public interface LeadGateway {

  LeadRecord create(CreateLeadCommand command);

  LeadRecord get(String leadId, String actorId);

  List<LeadRecord> list(String actorId);

  LeadRecord startOnboarding(String leadId, String actorId);

  LeadRecord assign(AssignLeadCommand command);

  /** {@code idempotencyKey} travels as the {@code Idempotency-Key} header, not in the body. */
  record CreateLeadCommand(
      String customerId,
      String lob,
      String productClass,
      String branchId,
      String actorId,
      @JsonIgnore String idempotencyKey) {

    public CreateLeadCommand(
        String customerId, String lob, String productClass, String branchId, String actorId) {
      this(customerId, lob, productClass, branchId, actorId, null);
    }
  }

  record AssignLeadCommand(
      String leadId,
      String assignedRmId,
      String assignedSpId,
      String actorId,
      String note,
      Meeting meeting) {

    public AssignLeadCommand(
        String leadId, String assignedRmId, String assignedSpId, String actorId) {
      this(leadId, assignedRmId, assignedSpId, actorId, null, null);
    }
  }

  /** Lead BRD Screen 7: {@code ONLINE | IN_PERSON}, date {@code yyyy-MM-dd}, time {@code HH:mm}. */
  record Meeting(String type, String date, String time, String link) {}

  record LeadRecord(
      String leadId,
      String customerId,
      String lob,
      String productClass,
      String branchId,
      String state,
      String dashboardStage,
      String assignedRmId,
      String assignedSpId,
      boolean exceptionHold,
      String exceptionOutcome,
      boolean exceptionRequired,
      String journeyId,
      Instant createdAt,
      Instant updatedAt) {}
}
