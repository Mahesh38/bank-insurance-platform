package com.bank.platform.proposal.domain;

import java.util.Optional;

/** Port: the working proposal before payment (DynamoDB {@code {env}-proposal-working}). */
public interface WorkingProposalStore {

  /** META + IDEM + OPEN_PROPOSAL marker + OUTBOX in one transaction; a replay returns the id. */
  CreateOutcome create(WorkingProposal proposal, IdempotencyClaim claim, OutboxEvent event);

  /** Strongly consistent read. */
  Optional<WorkingProposal> find(String proposalId);

  /**
   * Conditional on version and not materialised, plus {@code guard}. A newly received application
   * number claims its {@code APPNO#} marker in the same transaction. {@code event} may be null.
   */
  void update(WorkingProposal proposal, StoreGuard guard, OutboxEvent event);

  /** Freezes the item; false when another consumer already did. */
  boolean markMaterialized(WorkingProposal proposal);

  void releaseOpenMarker(String journeyId, String proposalId);
}
