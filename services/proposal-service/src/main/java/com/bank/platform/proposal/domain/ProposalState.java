package com.bank.platform.proposal.domain;

/** Proposal lifecycle (proposal-service API reference §2; CHECK {@code ck_proposal_state}). */
public enum ProposalState {
  DRAFT,
  SUBMITTED,
  SUBMISSION_FAILED,
  UNDER_WRITING,
  REQUIREMENTS_PENDING,
  UW_APPROVED,
  UW_COUNTER_OFFER,
  UW_DECLINED,
  CUSTOMER_DECLINED,
  AWAITING_PAYMENT,
  PAID,
  CONVERTED,
  ISSUANCE_FAILED,
  REFUND_REQUIRED,
  WITHDRAWN,
  EXPIRED,
  ABANDONED;

  /** Closed without a sale: the journey's open-proposal marker is released in the same write. */
  public boolean endsWithoutSale() {
    return this == UW_DECLINED
        || this == CUSTOMER_DECLINED
        || this == WITHDRAWN
        || this == EXPIRED
        || this == ABANDONED;
  }

  /** States an insurer underwriting update may move the proposal to (TI-03: insurer decides). */
  public boolean isUnderwritingOutcome() {
    return this == UNDER_WRITING
        || this == REQUIREMENTS_PENDING
        || this == UW_APPROVED
        || this == UW_COUNTER_OFFER
        || this == UW_DECLINED;
  }
}
