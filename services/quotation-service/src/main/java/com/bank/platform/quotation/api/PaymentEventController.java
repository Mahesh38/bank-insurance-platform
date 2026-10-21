package com.bank.platform.quotation.api;

import com.bank.platform.quotation.application.MaterializeOutcome;
import com.bank.platform.quotation.application.QuoteMaterializer;
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
      @NotBlank String eventId, @NotBlank String quoteId, @NotBlank String paymentId) {}

  public record Ack(String eventId, String quoteId, MaterializeOutcome outcome) {}

  private final QuoteMaterializer materializer;

  public PaymentEventController(QuoteMaterializer materializer) {
    this.materializer = materializer;
  }

  @PostMapping("/payment-reconciled")
  public Ack paymentReconciled(@Valid @RequestBody PaymentReconciled event) {
    return new Ack(
        event.eventId(),
        event.quoteId(),
        materializer.onPaymentReconciled(event.eventId(), event.quoteId()));
  }
}
