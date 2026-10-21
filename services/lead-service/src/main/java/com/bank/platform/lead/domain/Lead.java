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
  private boolean biGenerated;
  private ReportingClass reportingClass;
  private String journeyId;
  private Instant updatedAt;
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
    lead.biGenerated = snapshot.biGenerated();
    lead.reportingClass = Objects.requireNonNull(snapshot.reportingClass());
    lead.journeyId = snapshot.journeyId();
    lead.updatedAt = Objects.requireNonNull(snapshot.updatedAt());
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
      boolean biGenerated,
      ReportingClass reportingClass,
      String journeyId,
      Instant updatedAt,
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
        biGenerated,
        reportingClass,
        journeyId,
        updatedAt,
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

  public DashboardStage dashboardStage() {
    return DashboardStage.fromLead(state);
  }

  public boolean unfinished() {
    return !biGenerated
        && state != LeadState.DISQUALIFIED
        && state != LeadState.CONVERTED
        && state != LeadState.ARCHIVED
        && state != LeadState.EXPIRED;
  }

  public void applyExceptionOutcome(ExceptionOutcome outcome, Instant at) {
    this.exceptionOutcome = Objects.requireNonNull(outcome);
    this.exceptionHold = outcome == ExceptionOutcome.APPROVAL_REQUIRED;
    this.updatedAt = at;
  }

  public void assign(String rmId, String spId, Instant at) {
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
    this.state = LeadState.DISQUALIFIED;
    this.updatedAt = at;
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
