package com.bank.workforce.bff.lead;

import java.time.Instant;
import java.util.List;

public interface LeadGateway {

  LeadRecord create(CreateLeadCommand command);

  LeadRecord get(String leadId, String actorId);

  List<LeadRecord> list(String actorId);

  LeadRecord startOnboarding(String leadId, String actorId);

  LeadRecord assign(AssignLeadCommand command);

  record CreateLeadCommand(
      String customerId, String lob, String productClass, String branchId, String actorId) {}

  record AssignLeadCommand(
      String leadId, String assignedRmId, String assignedSpId, String actorId) {}

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
