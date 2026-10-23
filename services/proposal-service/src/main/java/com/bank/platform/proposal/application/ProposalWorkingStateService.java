package com.bank.platform.proposal.application;

import com.bank.common.error.ErrorCodes;
import com.bank.common.error.ServiceErrors;
import com.bank.common.error.ServiceException;
import com.bank.platform.proposal.domain.CounterOffer;
import com.bank.platform.proposal.domain.CreateOutcome;
import com.bank.platform.proposal.domain.DocumentRef;
import com.bank.platform.proposal.domain.IdempotencyClaim;
import com.bank.platform.proposal.domain.NewProposal;
import com.bank.platform.proposal.domain.Nominee;
import com.bank.platform.proposal.domain.OutboxEvent;
import com.bank.platform.proposal.domain.ProposalAnswer;
import com.bank.platform.proposal.domain.ProposalRuleException;
import com.bank.platform.proposal.domain.ProposalState;
import com.bank.platform.proposal.domain.StoreGuard;
import com.bank.platform.proposal.domain.Ulid;
import com.bank.platform.proposal.domain.UwRequirement;
import com.bank.platform.proposal.domain.WorkingProposal;
import com.bank.platform.proposal.domain.WorkingProposalStore;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

/**
 * Commands on the working proposal before payment (DB-DEC-0005): strongly consistent read, domain
 * transition, one conditional write with its outbox item when an event is due.
 */
@Service
public class ProposalWorkingStateService {

  private static final String COMPONENT = "ProposalWorkingStateService";
  private static final Pattern UPPER_SNAKE = Pattern.compile("^[A-Z][A-Z_]*$");

  private final WorkingProposalStore store;
  private final ServiceErrors errors;
  private final Clock clock;

  public ProposalWorkingStateService(
      WorkingProposalStore store, ServiceErrors errors, Clock clock) {
    this.store = store;
    this.errors = errors;
    this.clock = clock;
  }

  /** {@code DRAFT} + IDEM + OPEN_PROPOSAL marker + {@code ProposalCreated} in one transaction. */
  public CreateOutcome create(String idempotencyKey, NewProposal request) {
    if (idempotencyKey == null || idempotencyKey.isBlank()) {
      throw errors
          .error(ErrorCodes.MISSING_IDEMPOTENCY_KEY)
          .component(COMPONENT)
          .operation("create")
          .build();
    }
    Instant now = clock.instant();
    WorkingProposal proposal = WorkingProposal.draft(Ulid.mint(clock), request, now);
    return store.create(
        proposal,
        new IdempotencyClaim(idempotencyKey, Hashes.sha256(request.toString())),
        event(proposal, "ProposalCreated"));
  }

  public WorkingProposal get(String proposalId) {
    return load(proposalId, "get");
  }

  /** Answers with the question text and schema version shown (D8, envelope v2). */
  public WorkingProposal saveAnswers(
      String proposalId, List<ProposalAnswer> answers, Nominee nominee) {
    return change(
        proposalId,
        "saveAnswers",
        StoreGuard.NONE,
        null,
        p -> p.saveAnswers(answers, nominee, clock.instant()));
  }

  public WorkingProposal submit(String proposalId) {
    return change(
        proposalId, "submit", StoreGuard.NONE, "ProposalSubmitted", p -> p.submit(clock.instant()));
  }

  /** The insurer's application number; its {@code APPNO#} marker is claimed in the same write. */
  public WorkingProposal acknowledgeApplication(String proposalId, String applicationNumber) {
    return change(
        proposalId,
        "acknowledgeApplication",
        StoreGuard.NONE,
        "ProposalAcknowledged",
        p -> p.acknowledge(applicationNumber, clock.instant()));
  }

  public WorkingProposal recordUnderwriting(
      String proposalId,
      ProposalState next,
      String underwritingState,
      List<UwRequirement> requirements,
      List<DocumentRef> documents) {
    return recordUnderwriting(proposalId, next, underwritingState, requirements, documents, null);
  }

  /** As above; {@code counterOffer} is kept for {@code UW_COUNTER_OFFER} (LLD §4.4). */
  public WorkingProposal recordUnderwriting(
      String proposalId,
      ProposalState next,
      String underwritingState,
      List<UwRequirement> requirements,
      List<DocumentRef> documents,
      CounterOffer counterOffer) {
    return change(
        proposalId,
        "recordUnderwriting",
        StoreGuard.NONE,
        "UnderwritingDecision",
        p ->
            p.recordUnderwriting(
                next, underwritingState, requirements, documents, counterOffer, clock.instant()));
  }

  /**
   * {@code decideCounterOffer}: accept → {@code UW_APPROVED}, decline → {@code CUSTOMER_DECLINED}.
   */
  public WorkingProposal decideCounterOffer(String proposalId, boolean accept) {
    return change(
        proposalId,
        "decideCounterOffer",
        StoreGuard.NONE,
        "UnderwritingDecision",
        p -> p.decideCounterOffer(accept, clock.instant()));
  }

  public WorkingProposal awaitPayment(String proposalId) {
    return change(
        proposalId, "awaitPayment", StoreGuard.NONE, null, p -> p.awaitPayment(clock.instant()));
  }

  /**
   * {@code POST …/payment-hold} (LLD §4.5): {@code SET paymentInFlight, heldByPaymentId REMOVE
   * ttl}, only in {@code AWAITING_PAYMENT} (E1: reachable only from {@code UW_APPROVED}). The same
   * payment again returns the same proposal without a write.
   */
  public WorkingProposal holdForPayment(String proposalId, String paymentId) {
    WorkingProposal proposal = load(proposalId, "holdForPayment");
    boolean[] changed = new boolean[1];
    rule("holdForPayment", () -> changed[0] = proposal.holdForPayment(paymentId, clock.instant()));
    if (changed[0]) {
      try {
        store.update(proposal, StoreGuard.AWAITING_PAYMENT, null);
      } catch (ServiceException e) {
        // A concurrent hold won the optimistic lock: answer as if it had been read first.
        if (!ErrorCodes.CONFLICT.equals(e.getErrorResponse().getCode())) {
          throw e;
        }
        WorkingProposal current = load(proposalId, "holdForPayment");
        rule(
            "holdForPayment",
            () -> changed[0] = current.holdForPayment(paymentId, clock.instant()));
        if (changed[0]) {
          throw e;
        }
        return current;
      }
    }
    return proposal;
  }

  /** Result of {@code payment-release}. */
  public record ReleaseResult(WorkingProposal proposal, boolean released) {}

  /** {@code POST …/payment-release}: only the holding payment releases; otherwise a no-op. */
  public ReleaseResult releasePaymentHold(String proposalId, String paymentId) {
    WorkingProposal proposal = load(proposalId, "releasePaymentHold");
    boolean[] released = new boolean[1];
    rule(
        "releasePaymentHold",
        () -> released[0] = proposal.releasePaymentHold(paymentId, clock.instant()));
    if (released[0]) {
      store.update(proposal, StoreGuard.AWAITING_PAYMENT, null);
    }
    return new ReleaseResult(proposal, released[0]);
  }

  /** Payment started: {@code paymentInFlight = true}, {@code ttl} removed (schemas/20 §3.3). */
  public WorkingProposal markPaymentInFlight(String proposalId) {
    WorkingProposal proposal = load(proposalId, "markPaymentInFlight");
    if (proposal.paymentInFlight()) {
      return proposal;
    }
    rule("markPaymentInFlight", () -> proposal.markPaymentInFlight(clock.instant()));
    store.update(proposal, StoreGuard.AWAITING_PAYMENT, null);
    return proposal;
  }

  public WorkingProposal paymentEnded(String proposalId) {
    return change(
        proposalId,
        "paymentEnded",
        StoreGuard.AWAITING_PAYMENT,
        null,
        p -> p.paymentEnded(clock.instant()));
  }

  public WorkingProposal withdraw(String proposalId) {
    return change(
        proposalId,
        "withdraw",
        StoreGuard.NONE,
        "ProposalWithdrawn",
        p -> p.withdraw(clock.instant()));
  }

  private WorkingProposal change(
      String proposalId,
      String operation,
      StoreGuard guard,
      String eventType,
      Consumer<WorkingProposal> transition) {
    WorkingProposal proposal = load(proposalId, operation);
    rule(operation, () -> transition.accept(proposal));
    store.update(proposal, guard, eventType == null ? null : event(proposal, eventType));
    return proposal;
  }

  private WorkingProposal load(String proposalId, String operation) {
    return store
        .find(proposalId)
        .orElseThrow(
            () ->
                errors
                    .error(ErrorCodes.RESOURCE_NOT_FOUND)
                    .component(COMPONENT)
                    .operation(operation)
                    .reason("proposal " + proposalId)
                    .build());
  }

  private void rule(String operation, Runnable action) {
    try {
      action.run();
    } catch (ProposalRuleException e) {
      throw toServiceException(errors, COMPONENT, operation, e);
    }
  }

  static ServiceException toServiceException(
      ServiceErrors errors, String component, String operation, ProposalRuleException e) {
    return errors
        .error(e.code())
        .component(component)
        .operation(operation)
        .reason(e.getMessage())
        .cause(e)
        .build();
  }

  private OutboxEvent event(WorkingProposal proposal, String type) {
    // Monotonic ULID keeps two events of one proposal in write order (ADR-025 §6).
    return new OutboxEvent(
        com.bank.common.events.Ulid.mint(clock),
        type,
        proposal.proposalId(),
        proposal.updatedAt(),
        proposal.journeyId(),
        proposal.leadId(),
        proposal.configVersionRef(),
        null,
        data(proposal, type));
  }

  /** {@code data} per contracts/events/schemas (closed schemas: only declared fields). */
  static Map<String, Object> data(WorkingProposal proposal, String type) {
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("proposalId", proposal.proposalId());
    data.put("state", proposal.state().name());
    data.put("journeyId", proposal.journeyId());
    switch (type) {
      case "ProposalCreated", "ProposalSubmitted" -> data.put("quoteId", proposal.quoteId());
      case "ProposalAcknowledged" -> data.put("applicationNumber", proposal.applicationNumber());
      case "UnderwritingDecision" -> {
        String uw = proposal.underwritingState();
        // The schema takes a normalised UPPER_SNAKE status only; a raw insurer text stays out.
        if (uw != null && UPPER_SNAKE.matcher(uw).matches()) {
          data.put("underwritingState", uw);
        }
        data.put("requirementCount", proposal.envelope().requirements().size());
      }
      default -> {
        // ids and state only
      }
    }
    return data;
  }
}
