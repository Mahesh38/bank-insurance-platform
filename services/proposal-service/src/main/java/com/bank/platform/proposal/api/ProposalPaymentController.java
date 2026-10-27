package com.bank.platform.proposal.api;

import com.bank.common.error.ErrorCodes;
import com.bank.common.error.ServiceErrors;
import com.bank.platform.proposal.application.ProposalWorkingStateService;
import com.bank.platform.proposal.application.ProposalWorkingStateService.ReleaseResult;
import com.bank.platform.proposal.domain.ProposalState;
import com.bank.platform.proposal.domain.WorkingProposal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Payment's hold on the proposal (contract {@code proposal-service/openapi.yaml}, operations {@code
 * holdProposalForPayment}, {@code releaseProposalPaymentHold}; LLD §4.5). Ids only — the envelope
 * (answers, nominee) never leaves the service (INV-PRP-05).
 */
@RestController
@RequestMapping("/internal/v1/proposals")
public class ProposalPaymentController {

  static final String ULID = "^[0-9A-HJKMNP-TV-Z]{26}$";
  private static final String COMPONENT = "ProposalPaymentController";

  public record PaymentHoldRequest(@NotBlank @Pattern(regexp = ULID) String paymentId) {}

  public record PaymentReleaseRequest(
      @NotBlank @Pattern(regexp = ULID) String paymentId,
      @NotNull @Pattern(regexp = "^(DECLINED|EXPIRED|REJECTED|REFUNDED)$") String paymentState) {}

  /** {@code ProposalPaymentHold}. */
  public record ProposalPaymentHold(
      String proposalId,
      String quoteId,
      String offerId,
      String journeyId,
      String leadId,
      String customerId,
      String lob,
      String applicationNumber,
      String issuanceMode) {

    static ProposalPaymentHold of(WorkingProposal p) {
      return new ProposalPaymentHold(
          p.proposalId(),
          p.quoteId(),
          p.offerId(),
          p.journeyId(),
          p.leadId(),
          p.customerId(),
          p.lob(),
          p.applicationNumber(),
          p.issuanceMode());
    }
  }

  public record ReleaseResponse(String proposalId, ProposalState state, boolean released) {}

  private final ProposalWorkingStateService proposals;
  private final ActorGuard actors;
  private final ServiceErrors errors;

  public ProposalPaymentController(
      ProposalWorkingStateService proposals, ActorGuard actors, ServiceErrors errors) {
    this.proposals = proposals;
    this.actors = actors;
    this.errors = errors;
  }

  @PostMapping("/{proposalId}/payment-hold")
  public ProposalPaymentHold holdForPayment(
      HttpServletRequest request,
      @PathVariable String proposalId,
      @RequestHeader(value = "X-Actor-Id", required = false) String actorId,
      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
      @Valid @RequestBody PaymentHoldRequest requestBody) {
    requireHeaders(request, "holdProposalForPayment", proposalId, actorId, idempotencyKey);
    return ProposalPaymentHold.of(proposals.holdForPayment(proposalId, requestBody.paymentId()));
  }

  @PostMapping("/{proposalId}/payment-release")
  public ReleaseResponse releasePaymentHold(
      HttpServletRequest request,
      @PathVariable String proposalId,
      @RequestHeader(value = "X-Actor-Id", required = false) String actorId,
      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
      @Valid @RequestBody PaymentReleaseRequest requestBody) {
    requireHeaders(request, "releaseProposalPaymentHold", proposalId, actorId, idempotencyKey);
    ReleaseResult result = proposals.releasePaymentHold(proposalId, requestBody.paymentId());
    return new ReleaseResponse(proposalId, result.proposal().state(), result.released());
  }

  private void requireHeaders(
      HttpServletRequest request,
      String operation,
      String proposalId,
      String actorId,
      String idempotencyKey) {
    if (idempotencyKey == null || idempotencyKey.isBlank()) {
      throw errors
          .error(ErrorCodes.MISSING_IDEMPOTENCY_KEY)
          .component(COMPONENT)
          .operation(operation)
          .build();
    }
    String actor = actors.requireActorId(request, actorId, operation);
    if (actor.length() > 64) {
      throw invalid(operation, "X-Actor-Id is required (<= 64 chars)");
    }
    if (!proposalId.matches(ULID)) {
      throw invalid(operation, "proposalId must be a ULID");
    }
  }

  private RuntimeException invalid(String operation, String reason) {
    return errors
        .error(ErrorCodes.VALIDATION_ERROR)
        .component(COMPONENT)
        .operation(operation)
        .reason(reason)
        .build();
  }
}
