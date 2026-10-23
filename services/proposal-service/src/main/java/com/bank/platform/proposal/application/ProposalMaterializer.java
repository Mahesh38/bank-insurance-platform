package com.bank.platform.proposal.application;

import com.bank.common.error.ErrorCodes;
import com.bank.common.error.ServiceErrors;
import com.bank.common.events.Ulid;
import com.bank.platform.proposal.domain.MaterializationReceipt;
import com.bank.platform.proposal.domain.OutboxEvent;
import com.bank.platform.proposal.domain.ProposalRuleException;
import com.bank.platform.proposal.domain.StructuredProposalRepository;
import com.bank.platform.proposal.domain.WorkingProposal;
import com.bank.platform.proposal.domain.WorkingProposalStore;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Idempotent {@code PaymentReconciled} consumer (schemas/20 §3.4, proposal LLD §4.6): Aurora {@code
 * proposal}, one {@code proposal_answer} per question, {@code uw_requirement}, {@code
 * uw_document_ref} and the first {@code proposal_status_history} row {@code PAID} commit first;
 * then the item is frozen in {@code PAID} together with OUTBOX {@code ProposalMaterialised}. A
 * crash in between is repaired by re-delivery.
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

  public MaterializeOutcome onPaymentReconciled(
      String eventId, String proposalId, String paymentId) {
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
            eventId,
            Hashes.sha256("PaymentReconciled|" + proposalId),
            now.plus(RECEIPT_RETENTION),
            paymentId);

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
    store.markMaterialized(proposal, materialised(proposal, paymentId)); // 5
    store.releaseOpenMarker(proposal.journeyId(), proposalId); // 6
    return MaterializeOutcome.MATERIALIZED;
  }

  private OutboxEvent materialised(WorkingProposal proposal, String paymentId) {
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("proposalId", proposal.proposalId());
    data.put("paymentId", paymentId);
    data.put("journeyId", proposal.journeyId());
    data.put("applicationNumber", proposal.applicationNumber());
    data.put("quoteId", proposal.quoteId());
    data.put("answerCount", proposal.envelope().answers().size());
    return new OutboxEvent(
        Ulid.mint(clock),
        "ProposalMaterialised",
        proposal.proposalId(),
        proposal.materializedAt(),
        proposal.journeyId(),
        proposal.leadId(),
        proposal.configVersionRef(),
        null,
        data);
  }
}
