package com.bank.platform.lead.domain;

/**
 * RM-facing stage labels from Lead BRD §14.1. Product ({@code D-021}) confirmed these are a
 * <em>projection</em>: Lead owns pre-BI, Quote Generated, Closed and Policy Issued outcome.
 * Proposal / UW / Policy-pending labels belong to Journey, Proposal and Policy — they are never
 * Lead transitions.
 */
public enum DashboardStage {
  NEW,
  QUOTE_GENERATED,
  PROPOSAL_FORM_PENDING,
  PROPOSAL_SUBMITTED,
  POLICY_PENDING,
  REQUIREMENTS_AWAITED,
  UNDERWRITING_QUEUE,
  POLICY_ISSUED,
  POLICY_DECLINED,
  CLOSED;

  /**
   * Maps a Lead aggregate state to the labels Lead is allowed to own. Callers that need post-quote
   * insurer-status labels must ask Journey.
   */
  public static DashboardStage fromLead(LeadState state) {
    return switch (state) {
      case NEW, ASSIGNED, CONTACTED -> NEW;
      case QUALIFIED -> QUOTE_GENERATED;
      case CONVERTED, ARCHIVED -> POLICY_ISSUED;
      case DISQUALIFIED, EXPIRED -> CLOSED;
    };
  }
}
