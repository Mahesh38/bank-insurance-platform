package com.bank.platform.proposal.domain;

/** Port: structured proposal in Aurora schema {@code proposal} after RECONCILED. */
public interface StructuredProposalRepository {

  boolean exists(String proposalId);

  /**
   * {@code proposal.proposal}, {@code uw_requirement}, {@code uw_document_ref} and the receipt in
   * ONE transaction; each insert is a no-op when the row exists (schemas/20 §3.4).
   */
  void insert(WorkingProposal proposal, MaterializationReceipt receipt);
}
