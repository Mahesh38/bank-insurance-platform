package com.bank.platform.lead.application;

import com.bank.common.error.ErrorCodes;
import com.bank.common.error.ServiceErrors;
import com.bank.platform.lead.domain.ExceptionOutcome;
import com.bank.platform.lead.domain.Lead;
import com.bank.platform.lead.domain.LeadRepository;
import com.bank.platform.lead.domain.LeadState;
import com.bank.platform.lead.domain.ProductClass;
import com.bank.platform.lead.domain.SpCertificationPort;
import com.bank.platform.lead.domain.Ulid;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

/**
 * Lead commands for the R0 assisted slice ({@code D-019}, {@code D-020}, {@code D-021}).
 *
 * <p>Persistence is an in-memory port implementation until Aarti's physical pack is applied ({@code
 * SUG-20260825-db1} parked at S09).
 */
@Service
public class LeadApplicationService {

  private final LeadRepository leads;
  private final ServiceErrors errors;
  private final Clock clock;
  private final SpCertificationPort certification;

  public LeadApplicationService(
      LeadRepository leads, ServiceErrors errors, Clock clock, SpCertificationPort certification) {
    this.leads = leads;
    this.errors = errors;
    this.clock = clock;
    this.certification = certification;
  }

  public Lead create(CreateLeadCommand command) {
    require(command.customerId(), "customerId");
    require(command.createdByPrincipalId(), "createdByPrincipalId");
    if (!"LIFE".equals(command.lob())) {
      throw errors
          .error(ErrorCodes.UNSUPPORTED_LOB)
          .component("LeadApplicationService")
          .operation("create")
          .reason("lob must be LIFE")
          .build();
    }
    ProductClass productClass = parseProductClass(command.productClass());
    leads
        .findUnfinished(command.createdByPrincipalId(), command.customerId(), productClass)
        .ifPresent(
            existing -> {
              throw errors
                  .error(ErrorCodes.CONFLICT)
                  .component("LeadApplicationService")
                  .operation("create")
                  .reason("unfinished lead exists for this creator, customer and product class")
                  .build();
            });
    Instant now = clock.instant();
    Lead lead =
        new Lead(
            Ulid.mint(clock),
            command.customerId(),
            command.lob(),
            productClass,
            command.branchId(),
            command.createdByPrincipalId(),
            now);
    leads.save(lead);
    return lead;
  }

  public Lead getVisible(String leadId, String principalId) {
    Lead lead = leads.findById(leadId).orElseThrow(() -> notFound());
    if (!lead.visibleTo(principalId)) {
      throw notFound();
    }
    return lead;
  }

  public List<Lead> listVisible(String principalId) {
    return listVisible(principalId, null, null, false);
  }

  public List<Lead> listVisible(
      String principalId, String customerId, String productClass, boolean unfinishedOnly) {
    return leads.findVisibleTo(principalId).stream()
        .filter(lead -> customerId == null || customerId.equals(lead.customerId()))
        .filter(lead -> productClass == null || productClass.equals(lead.productClass().name()))
        .filter(lead -> !unfinishedOnly || lead.unfinished())
        .toList();
  }

  /** Start Onboarding evaluates exception rules. Save/create never does ({@code D-019}). */
  public Lead startOnboarding(String leadId, String principalId) {
    Lead lead = getVisible(leadId, principalId);
    ExceptionOutcome outcome = evaluate(lead.customerId());
    lead.applyExceptionOutcome(outcome, clock.instant());
    leads.save(lead);
    if (outcome == ExceptionOutcome.BLOCK) {
      throw errors
          .error(ErrorCodes.ILLEGAL_TRANSITION)
          .component("LeadApplicationService")
          .operation("startOnboarding")
          .reason("exception rules blocked onboarding")
          .build();
    }
    return lead;
  }

  public Lead assign(AssignLeadCommand command) {
    Lead lead = getVisible(command.leadId(), command.actorPrincipalId());
    if (lead.biGenerated()) {
      throw errors
          .error(ErrorCodes.ILLEGAL_TRANSITION)
          .component("LeadApplicationService")
          .operation("assign")
          .reason("reassignment after BI is not permitted")
          .build();
    }
    if (lead.exceptionOutcome() == ExceptionOutcome.NOT_EVALUATED) {
      throw errors
          .error(ErrorCodes.ILLEGAL_TRANSITION)
          .component("LeadApplicationService")
          .operation("assign")
          .reason("start onboarding before assignment")
          .build();
    }
    if (lead.exceptionOutcome() == ExceptionOutcome.BLOCK || lead.exceptionHold()) {
      throw errors
          .error(ErrorCodes.CONFLICT)
          .component("LeadApplicationService")
          .operation("assign")
          .reason("exception hold is active")
          .build();
    }
    require(command.assignedRmId(), "assignedRmId");
    if (!certification.holdsValidCertification(command.assignedRmId())) {
      throw errors
          .error(ErrorCodes.SP_CERTIFICATION_REQUIRED)
          .component("LeadApplicationService")
          .operation("assign")
          .reason("assignee must hold a valid SP certification")
          .build();
    }
    lead.assign(command.assignedRmId(), command.assignedSpId(), clock.instant());
    leads.save(lead);
    return lead;
  }

  public Lead close(String leadId, String principalId) {
    Lead lead = getVisible(leadId, principalId);
    if (lead.state() == LeadState.QUALIFIED || lead.biGenerated()) {
      throw errors
          .error(ErrorCodes.ILLEGAL_TRANSITION)
          .component("LeadApplicationService")
          .operation("close")
          .reason("eligible leads are not closed from the diary path")
          .build();
    }
    lead.close(clock.instant());
    leads.save(lead);
    return lead;
  }

  /**
   * Deterministic stub until Exception Handling / DWH rules are wired. Tokens are synthetic test
   * customer ids, never logged.
   */
  static ExceptionOutcome evaluate(String customerId) {
    String token = customerId == null ? "" : customerId.toUpperCase(Locale.ROOT);
    if (token.contains("BLOCK")) {
      return ExceptionOutcome.BLOCK;
    }
    if (token.contains("HOLD")) {
      return ExceptionOutcome.APPROVAL_REQUIRED;
    }
    return ExceptionOutcome.PASS;
  }

  private ProductClass parseProductClass(String raw) {
    try {
      return ProductClass.valueOf(raw);
    } catch (RuntimeException ex) {
      throw errors
          .error(ErrorCodes.UNSUPPORTED_LOB)
          .component("LeadApplicationService")
          .operation("create")
          .reason("product class is not TERM, SAVINGS or ULIP")
          .cause(ex)
          .build();
    }
  }

  private void require(String value, String field) {
    if (value == null || value.isBlank()) {
      throw errors
          .error(ErrorCodes.MISSING_REQUIRED_FIELD)
          .component("LeadApplicationService")
          .operation("validate")
          .reason(field + " is required")
          .build();
    }
  }

  private RuntimeException notFound() {
    return errors
        .error(ErrorCodes.RESOURCE_NOT_FOUND)
        .component("LeadApplicationService")
        .operation("get")
        .reason("lead is absent from this book")
        .build();
  }

  public record CreateLeadCommand(
      String customerId,
      String lob,
      String productClass,
      String branchId,
      String createdByPrincipalId) {}

  public record AssignLeadCommand(
      String leadId, String assignedRmId, String assignedSpId, String actorPrincipalId) {}
}
