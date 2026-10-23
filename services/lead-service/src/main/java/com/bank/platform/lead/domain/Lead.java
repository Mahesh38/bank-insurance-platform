package com.bank.platform.lead.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * Lead aggregate. Identity is a bank-minted ULID that never changes ({@code BR-LEAD-001}, {@code
 * BR-LEAD-002}, {@code D-020}).
 */
public final class Lead {

  private final String leadId;
  private final String customerId;
  private final String lob;
  private final ProductClass productClass;
  private final String branchId;
  private final String createdByPrincipalId;
  private final Instant createdAt;
  private LeadState state;
  private String assignedRmId;
  private String assignedSpId;
  private String accountableSpId;
  private boolean exceptionHold;
  private ExceptionOutcome exceptionOutcome;
  private String exceptionEvaluationId;
  private boolean biGenerated;
  private ReportingClass reportingClass;
  private String journeyId;
  private Instant updatedAt;
  private Instant expiresAt;
  private long version;

  public Lead(
      String leadId,
      String customerId,
      String lob,
      ProductClass productClass,
      String branchId,
      String createdByPrincipalId,
      Instant createdAt) {
    this.leadId = Objects.requireNonNull(leadId);
    this.customerId = Objects.requireNonNull(customerId);
    this.lob = Objects.requireNonNull(lob);
    this.productClass = Objects.requireNonNull(productClass);
    this.branchId = branchId;
    this.createdByPrincipalId = Objects.requireNonNull(createdByPrincipalId);
    this.createdAt = Objects.requireNonNull(createdAt);
    this.updatedAt = createdAt;
    this.state = LeadState.NEW;
    this.exceptionOutcome = ExceptionOutcome.NOT_EVALUATED;
    this.reportingClass = ReportingClass.DIARY;
    this.journeyId = leadId; // inert journey reference until Journey context owns a real id
  }

  /**
   * Rebuilds a stored lead. Only persistence adapters call this; every business transition still
   * goes through the command methods below.
   */
  public static Lead restore(Snapshot snapshot) {
    Lead lead =
        new Lead(
            snapshot.leadId(),
            snapshot.customerId(),
            snapshot.lob(),
            snapshot.productClass(),
            snapshot.branchId(),
            snapshot.createdByPrincipalId(),
            snapshot.createdAt());
    lead.state = Objects.requireNonNull(snapshot.state());
    lead.assignedRmId = snapshot.assignedRmId();
    lead.assignedSpId = snapshot.assignedSpId();
    lead.accountableSpId = snapshot.accountableSpId();
    lead.exceptionHold = snapshot.exceptionHold();
    lead.exceptionOutcome = Objects.requireNonNull(snapshot.exceptionOutcome());
    lead.exceptionEvaluationId = snapshot.exceptionEvaluationId();
    lead.biGenerated = snapshot.biGenerated();
    lead.reportingClass = Objects.requireNonNull(snapshot.reportingClass());
    lead.journeyId = snapshot.journeyId();
    lead.updatedAt = Objects.requireNonNull(snapshot.updatedAt());
    lead.expiresAt = snapshot.expiresAt();
    lead.version = snapshot.version();
    return lead;
  }

  /** Every persisted field, in one value — the shape a repository reads and writes. */
  public record Snapshot(
      String leadId,
      String customerId,
      String lob,
      ProductClass productClass,
      String branchId,
      String createdByPrincipalId,
      Instant createdAt,
      LeadState state,
      String assignedRmId,
      String assignedSpId,
      String accountableSpId,
      boolean exceptionHold,
      ExceptionOutcome exceptionOutcome,
      String exceptionEvaluationId,
      boolean biGenerated,
      ReportingClass reportingClass,
      String journeyId,
      Instant updatedAt,
      Instant expiresAt,
      long version) {}

  public Snapshot snapshot() {
    return new Snapshot(
        leadId,
        customerId,
        lob,
        productClass,
        branchId,
        createdByPrincipalId,
        createdAt,
        state,
        assignedRmId,
        assignedSpId,
        accountableSpId,
        exceptionHold,
        exceptionOutcome,
        exceptionEvaluationId,
        biGenerated,
        reportingClass,
        journeyId,
        updatedAt,
        expiresAt,
        version);
  }

  /** Version of the stored row this instance was read from; 0 for a lead never saved. */
  public long version() {
    return version;
  }

  /** Called by a repository after a successful write. */
  public void markPersisted(long newVersion) {
    this.version = newVersion;
  }

  public String leadId() {
    return leadId;
  }

  public String customerId() {
    return customerId;
  }

  public String lob() {
    return lob;
  }

  public ProductClass productClass() {
    return productClass;
  }

  public String branchId() {
    return branchId;
  }

  public String createdByPrincipalId() {
    return createdByPrincipalId;
  }

  public Instant createdAt() {
    return createdAt;
  }

  public LeadState state() {
    return state;
  }

  public String assignedRmId() {
    return assignedRmId;
  }

  public String assignedSpId() {
    return assignedSpId;
  }

  public String accountableSpId() {
    return accountableSpId;
  }

  public boolean exceptionHold() {
    return exceptionHold;
  }

  public ExceptionOutcome exceptionOutcome() {
    return exceptionOutcome;
  }

  /** The {@code lead_exception_evaluation} row that set the current outcome; null before one. */
  public String exceptionEvaluationId() {
    return exceptionEvaluationId;
  }

  public boolean biGenerated() {
    return biGenerated;
  }

  public ReportingClass reportingClass() {
    return reportingClass;
  }

  public String journeyId() {
    return journeyId;
  }

  public Instant updatedAt() {
    return updatedAt;
  }

  /** Ageing horizon from rule set {@code LEAD.POLICY} (E7); null only on rows created before it. */
  public Instant expiresAt() {
    return expiresAt;
  }

  /** Set once, at create, by the application service from configuration. */
  public void expireAt(Instant at) {
    if (version != 0) {
      throw new IllegalLeadTransitionException("expiry is set when the lead is created");
    }
    this.expiresAt = at;
  }

  /** DISQUALIFIED (the close path), CONVERTED, EXPIRED and ARCHIVED admit no further command. */
  public boolean terminal() {
    return state == LeadState.DISQUALIFIED
        || state == LeadState.CONVERTED
        || state == LeadState.EXPIRED
        || state == LeadState.ARCHIVED;
  }

  public DashboardStage dashboardStage() {
    return DashboardStage.fromLead(state);
  }

  public boolean unfinished() {
    return !biGenerated && !terminal();
  }

  /**
   * Refuses re-evaluation on a terminal lead, while an {@code APPROVAL_REQUIRED} hold awaits AUBIMA
   * (only the approval workflow releases it — Exception Handling BRD §4.2), and once the lead is
   * {@code BLOCK}ed: a later {@code PASS} must not silently clear either (READINESS-REVIEW §3.6).
   */
  public void applyExceptionOutcome(ExceptionOutcome outcome, Instant at) {
    applyExceptionOutcome(outcome, null, at);
  }

  /** As above, remembering which stored evaluation decided (the approval checks its roles). */
  public void applyExceptionOutcome(ExceptionOutcome outcome, String evaluationId, Instant at) {
    Objects.requireNonNull(outcome);
    if (outcome == ExceptionOutcome.NOT_EVALUATED) {
      throw new IllegalArgumentException("NOT_EVALUATED is not an evaluation result");
    }
    requireEvaluable();
    this.exceptionOutcome = outcome;
    this.exceptionEvaluationId = evaluationId;
    this.exceptionHold = outcome == ExceptionOutcome.APPROVAL_REQUIRED;
    this.updatedAt = at;
  }

  /**
   * Throws unless Start Onboarding may (re-)evaluate the exception rules for this lead. An approved
   * exception is not re-evaluated either: a re-run could only overwrite the bank's decision (BRD
   * §8.11 — resume from the latest valid point).
   */
  public void requireEvaluable() {
    requireNotTerminal("evaluate exception rules on");
    if (exceptionHold) {
      throw new IllegalLeadTransitionException("exception hold awaiting approval");
    }
    if (exceptionOutcome == ExceptionOutcome.BLOCK) {
      throw new IllegalLeadTransitionException("exception rules blocked onboarding");
    }
    if (exceptionOutcome == ExceptionOutcome.APPROVAL_REQUIRED) {
      throw new IllegalLeadTransitionException("exception already approved; resume the journey");
    }
  }

  /** Throws unless an {@code APPROVAL_REQUIRED} hold awaits a decision (E6, BRD §8.5–§8.8). */
  public void requireHeldForDecision() {
    requireNotTerminal("decide the exception of");
    if (!exceptionHold || exceptionOutcome != ExceptionOutcome.APPROVAL_REQUIRED) {
      throw new IllegalLeadTransitionException("no exception hold awaits a decision");
    }
  }

  /** Final approval (BRD §8.7): the hold is cleared and assignment may proceed. */
  public void releaseExceptionHold(Instant at) {
    requireHeldForDecision();
    this.exceptionHold = false;
    this.updatedAt = at;
  }

  /**
   * Rejection (BRD §8.8, §24.7 row 6): the journey is blocked for good and the lead closes, so a
   * new lead may be created for the customer (dedupe ignores terminal leads).
   */
  public void rejectException(Instant at) {
    requireHeldForDecision();
    this.exceptionHold = false;
    this.exceptionOutcome = ExceptionOutcome.BLOCK;
    this.state = LeadState.DISQUALIFIED;
    this.updatedAt = at;
  }

  /** The lead is unfinished and its {@code expires_at} has passed. */
  public boolean overdue(Instant now) {
    return unfinished() && expiresAt != null && !now.isBefore(expiresAt);
  }

  /** Ageing (E7, {@code LEAD.POLICY.expiryDays}): an overdue unfinished lead becomes EXPIRED. */
  public void expire(Instant at) {
    if (!overdue(at)) {
      throw new IllegalLeadTransitionException("lead is not overdue in state " + state);
    }
    this.state = LeadState.EXPIRED;
    this.updatedAt = at;
  }

  /** {@code BR-CLOSE-001}: a closed (or otherwise terminal) lead is never reassigned. */
  public void assign(String rmId, String spId, Instant at) {
    requireNotTerminal("assign");
    this.assignedRmId = Objects.requireNonNull(rmId);
    this.assignedSpId = spId;
    if (this.accountableSpId == null) {
      this.accountableSpId = rmId;
    }
    this.state = LeadState.ASSIGNED;
    this.updatedAt = at;
  }

  public void markFirstBi(Instant at) {
    this.biGenerated = true;
    this.reportingClass = ReportingClass.ELIGIBLE;
    this.state = LeadState.QUALIFIED;
    this.updatedAt = at;
  }

  public void close(Instant at) {
    requireNotTerminal("close");
    this.state = LeadState.DISQUALIFIED;
    this.updatedAt = at;
  }

  private void requireNotTerminal(String action) {
    if (terminal()) {
      throw new IllegalLeadTransitionException("cannot " + action + " a lead in state " + state);
    }
  }

  public boolean visibleTo(String principalId) {
    if (principalId == null) {
      return false;
    }
    return principalId.equals(createdByPrincipalId)
        || principalId.equals(assignedRmId)
        || principalId.equals(assignedSpId);
  }
}
