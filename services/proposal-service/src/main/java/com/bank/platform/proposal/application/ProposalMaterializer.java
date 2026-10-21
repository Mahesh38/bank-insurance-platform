package com.bank.platform.proposal.application;

import com.bank.common.error.ErrorCodes;
import com.bank.common.error.ServiceErrors;
import com.bank.platform.proposal.domain.MaterializationReceipt;
import com.bank.platform.proposal.domain.ProposalRuleException;
import com.bank.platform.proposal.domain.StructuredProposalRepository;
import com.bank.platform.proposal.domain.WorkingProposal;
import com.bank.platform.proposal.domain.WorkingProposalStore;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Service;

/**
 * Idempotent {@code PaymentReconciled} consumer (schemas/20 §3.4, proposal API §3.5): Aurora {@code
 * proposal}, {@code uw_requirement}, {@code uw_document_ref} commit first, then the item is frozen
 * in {@code PAID}. A crash in between is repaired by re-delivery.
 */
@Service
public class ProposalMaterializer {

  private static final String COMPONENT = "ProposalMaterializer";
  private static final Duration RECEIPT_RETENTION = Duration.ofDays(30);

  private final WorkingProposalStore store;
  private final StructuredProposalRepository structured;
  private final ServiceErrors errors;
  private final Clock clock;

  public ProposalMaterializer(
      WorkingProposalStore store,
      StructuredProposalRepository structured,
      ServiceErrors errors,
      Clock clock) {
    this.store = store;
    this.structured = structured;
    this.errors = errors;
    this.clock = clock;
  }

  public MaterializeOutcome onPaymentReconciled(String eventId, String proposalId) {
    // 1 + 3: consistent read, decrypted in process
    WorkingProposal proposal =
        store
            .find(proposalId)
            .orElseThrow(
                () ->
                    errors
                        .error(ErrorCodes.RESOURCE_NOT_FOUND)
                        .component(COMPONENT)
                        .operation("onPaymentReconciled")
                        .reason(
                            "working proposal " + proposalId + " is gone; repair sweep must alert")
                        .build());
    Instant now = clock.instant();
    MaterializationReceipt receipt =
        new MaterializationReceipt(
            eventId, Hashes.sha256("PaymentReconciled|" + proposalId), now.plus(RECEIPT_RETENTION));

    // 2
    if (proposal.isMaterialized()) {
      if (!structured.exists(proposalId)) {
        structured.insert(proposal, receipt);
      }
      store.releaseOpenMarker(proposal.journeyId(), proposalId);
      return MaterializeOutcome.ALREADY_MATERIALIZED;
    }

    try {
      proposal.materialize(now);
    } catch (ProposalRuleException e) {
      throw ProposalWorkingStateService.toServiceException(
          errors, COMPONENT, "onPaymentReconciled", e);
    }
    structured.insert(proposal, receipt); // 4
    store.markMaterialized(proposal); // 5
    store.releaseOpenMarker(proposal.journeyId(), proposalId); // 6
    return MaterializeOutcome.MATERIALIZED;
  }
}
