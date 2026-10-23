package com.bank.platform.lead.api;

import com.bank.common.error.ErrorCodes;
import com.bank.common.error.ServiceErrors;
import com.bank.platform.lead.application.LeadApplicationService;
import com.bank.platform.lead.application.LeadApplicationService.AssignLeadCommand;
import com.bank.platform.lead.application.LeadApplicationService.CreateLeadCommand;
import com.bank.platform.lead.application.LeadApplicationService.ExceptionDecisionCommand;
import com.bank.platform.lead.application.LeadApplicationService.MeetingIntent;
import com.bank.platform.lead.domain.ExceptionOutcome;
import com.bank.platform.lead.domain.Lead;
import com.bank.platform.lead.domain.LeadActivityPort.MeetingType;
import com.bank.platform.lead.domain.LeadExceptionEvidencePort.DecisionKind;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/v1/leads")
public class LeadController {

  private final LeadApplicationService leads;
  private final ServiceErrors errors;

  public LeadController(LeadApplicationService leads, ServiceErrors errors) {
    this.leads = leads;
    this.errors = errors;
  }

  @PostMapping
  public ResponseEntity<LeadResponse> create(
      @RequestHeader("X-Actor-Id") String actorId,
      @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
      @Valid @RequestBody CreateLeadRequest request) {
    Lead lead =
        leads.create(
            new CreateLeadCommand(
                request.customerId(),
                request.lob(),
                request.productClass(),
                request.branchId(),
                actorId),
            idempotencyKey);
    return ResponseEntity.status(HttpStatus.CREATED).body(LeadResponse.from(lead));
  }

  @GetMapping
  public LeadPageResponse list(
      @RequestHeader("X-Actor-Id") String actorId,
      @RequestParam(name = "owner", defaultValue = "me") String owner,
      @RequestParam(required = false) String customerId,
      @RequestParam(required = false) String productClass,
      @RequestParam(defaultValue = "false") boolean unfinished) {
    if (!"me".equals(owner)) {
      throw errors
          .error(ErrorCodes.INVALID_REQUEST)
          .component("LeadController")
          .operation("list")
          .reason("owner must be me")
          .build();
    }
    List<LeadResponse> items =
        leads.listVisible(actorId, customerId, productClass, unfinished).stream()
            .map(LeadResponse::from)
            .toList();
    return new LeadPageResponse(items, null);
  }

  @GetMapping("/{leadId}")
  public LeadResponse get(
      @RequestHeader("X-Actor-Id") String actorId, @PathVariable String leadId) {
    return LeadResponse.from(leads.getVisible(leadId, actorId));
  }

  @PostMapping("/{leadId}/onboarding")
  public LeadResponse startOnboarding(
      @RequestHeader("X-Actor-Id") String actorId, @PathVariable String leadId) {
    return LeadResponse.from(leads.startOnboarding(leadId, actorId));
  }

  @PostMapping("/{leadId}/assignments")
  public LeadResponse assign(
      @RequestHeader("X-Actor-Id") String actorId,
      @PathVariable String leadId,
      @Valid @RequestBody AssignLeadRequest request) {
    return LeadResponse.from(
        leads.assign(
            new AssignLeadCommand(
                leadId,
                request.assignedRmId(),
                request.assignedSpId(),
                actorId,
                request.note(),
                request.meeting() == null ? null : request.meeting().toIntent())));
  }

  @PostMapping("/{leadId}/close")
  public LeadResponse close(
      @RequestHeader("X-Actor-Id") String actorId, @PathVariable String leadId) {
    return LeadResponse.from(leads.close(leadId, actorId));
  }

  /**
   * E6: final approval of an exception hold by a bank approver; the hold clears and assignment may
   * proceed. {@code X-Actor-Type} and {@code X-Actor-Roles} are set by the BFF from the session.
   */
  @PostMapping("/{leadId}/exception-hold:release")
  public LeadResponse releaseExceptionHold(
      @RequestHeader("X-Actor-Id") String actorId,
      @RequestHeader(name = "X-Actor-Type", required = false) String actorType,
      @RequestHeader(name = "X-Actor-Roles", required = false) String actorRoles,
      @PathVariable String leadId,
      @Valid @RequestBody ExceptionDecisionRequest request) {
    return decide(leadId, DecisionKind.APPROVED, actorId, actorType, actorRoles, request);
  }

  /** E6: rejection — the lead is blocked for good and closes (BRD §8.8). */
  @PostMapping("/{leadId}/exception-hold:reject")
  public LeadResponse rejectExceptionHold(
      @RequestHeader("X-Actor-Id") String actorId,
      @RequestHeader(name = "X-Actor-Type", required = false) String actorType,
      @RequestHeader(name = "X-Actor-Roles", required = false) String actorRoles,
      @PathVariable String leadId,
      @Valid @RequestBody ExceptionDecisionRequest request) {
    return decide(leadId, DecisionKind.REJECTED, actorId, actorType, actorRoles, request);
  }

  private LeadResponse decide(
      String leadId,
      DecisionKind kind,
      String actorId,
      String actorType,
      String actorRoles,
      ExceptionDecisionRequest request) {
    Set<String> roles =
        actorRoles == null
            ? Set.of()
            : Arrays.stream(actorRoles.split(","))
                .map(String::strip)
                .filter(r -> !r.isEmpty())
                .collect(Collectors.toSet());
    return LeadResponse.from(
        leads.decideException(
            new ExceptionDecisionCommand(
                leadId,
                kind,
                actorId,
                actorType,
                roles,
                request.remarks(),
                request.decisionRef())));
  }

  /** Remarks mandatory (Exception BRD §24.3 column J, §5.2). */
  public record ExceptionDecisionRequest(
      @NotBlank @Size(max = 500) String remarks, @Size(max = 64) String decisionRef) {}

  public record CreateLeadRequest(
      @NotBlank String customerId,
      @NotBlank String lob,
      @NotBlank String productClass,
      String branchId) {}

  public record AssignLeadRequest(
      @NotBlank String assignedRmId,
      String assignedSpId,
      @Size(max = 250) String note,
      @Valid MeetingRequest meeting) {}

  /** Lead BRD Screen 7 meeting capture; date {@code yyyy-MM-dd}, time {@code HH:mm} bank time. */
  public record MeetingRequest(
      @NotNull MeetingType type, @NotBlank String date, @NotBlank String time, String link) {
    MeetingIntent toIntent() {
      return new MeetingIntent(type, date, time, link);
    }
  }

  public record LeadPageResponse(List<LeadResponse> items, String nextCursor) {}

  public record LeadResponse(
      String leadId,
      String customerId,
      String lob,
      String productClass,
      String branchId,
      String state,
      String dashboardStage,
      String assignedRmId,
      String assignedSpId,
      String accountableSpId,
      boolean exceptionHold,
      String exceptionOutcome,
      boolean exceptionRequired,
      boolean biGenerated,
      String reportingClass,
      String journeyId,
      Instant expiresAt,
      Instant createdAt,
      Instant updatedAt) {
    static LeadResponse from(Lead lead) {
      boolean exceptionRequired =
          lead.exceptionOutcome() == ExceptionOutcome.APPROVAL_REQUIRED
              || lead.exceptionOutcome() == ExceptionOutcome.BLOCK
              || lead.exceptionHold();
      return new LeadResponse(
          lead.leadId(),
          lead.customerId(),
          lead.lob(),
          lead.productClass().name(),
          lead.branchId(),
          lead.state().name(),
          lead.dashboardStage().name(),
          lead.assignedRmId(),
          lead.assignedSpId(),
          lead.accountableSpId(),
          lead.exceptionHold(),
          lead.exceptionOutcome().name(),
          exceptionRequired,
          lead.biGenerated(),
          lead.reportingClass().name(),
          lead.journeyId(),
          lead.expiresAt(),
          lead.createdAt(),
          lead.updatedAt());
    }
  }
}
