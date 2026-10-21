package com.bank.platform.proposal.api;

import com.bank.platform.proposal.application.MaterializeOutcome;
import com.bank.platform.proposal.application.ProposalMaterializer;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Internal delivery of {@code PaymentReconciled} until the event bus exists (no Kafka in R0 code).
 * Idempotent: re-delivery answers {@code ALREADY_MATERIALIZED}.
 */
@RestController
@RequestMapping("/internal/v1/events")
public class PaymentEventController {

  public record PaymentReconciled(
      @NotBlank String eventId, @NotBlank String proposalId, @NotBlank String paymentId) {}

  public record Ack(String eventId, String proposalId, MaterializeOutcome outcome) {}

  private final ProposalMaterializer materializer;

  public PaymentEventController(ProposalMaterializer materializer) {
    this.materializer = materializer;
  }

  @PostMapping("/payment-reconciled")
  public Ack paymentReconciled(@Valid @RequestBody PaymentReconciled event) {
    return new Ack(
        event.eventId(),
        event.proposalId(),
        materializer.onPaymentReconciled(event.eventId(), event.proposalId()));
  }
}
