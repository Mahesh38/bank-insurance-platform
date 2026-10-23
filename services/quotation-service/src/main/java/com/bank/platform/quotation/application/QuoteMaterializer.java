package com.bank.platform.quotation.application;

import com.bank.common.error.ErrorCodes;
import com.bank.common.error.ServiceErrors;
import com.bank.common.events.Ulid;
import com.bank.platform.quotation.domain.MaterializationReceipt;
import com.bank.platform.quotation.domain.OutboxEvent;
import com.bank.platform.quotation.domain.QuoteRuleException;
import com.bank.platform.quotation.domain.StructuredQuoteRepository;
import com.bank.platform.quotation.domain.WorkingQuote;
import com.bank.platform.quotation.domain.WorkingQuoteStore;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Idempotent {@code PaymentReconciled} consumer (schemas/20 §3.4). Aurora commits first and is the
 * record from that moment; the DynamoDB item is frozen afterwards together with OUTBOX {@code
 * QuoteConverted} (D8). No transaction spans the two stores: a crash in between is repaired by
 * re-delivery, because every step is a no-op the second time.
 */
@Service
public class QuoteMaterializer {

  private static final String COMPONENT = "QuoteMaterializer";
  // The receipt outlives every plausible re-delivery of the event.
  private static final Duration RECEIPT_RETENTION = Duration.ofDays(30);

  private final WorkingQuoteStore store;
  private final StructuredQuoteRepository structured;
  private final ServiceErrors errors;
  private final Clock clock;

  public QuoteMaterializer(
      WorkingQuoteStore store,
      StructuredQuoteRepository structured,
      ServiceErrors errors,
      Clock clock) {
    this.store = store;
    this.structured = structured;
    this.errors = errors;
    this.clock = clock;
  }

  public MaterializeOutcome onPaymentReconciled(String eventId, String quoteId, String paymentId) {
    // 1  consistent read (and 3: the store decrypts in process)
    WorkingQuote quote =
        store
            .find(quoteId)
            .orElseThrow(
                () ->
                    errors
                        .error(ErrorCodes.RESOURCE_NOT_FOUND)
                        .component(COMPONENT)
                        .operation("onPaymentReconciled")
                        .reason("working quote " + quoteId + " is gone; repair sweep must alert")
                        .build());
    Instant now = clock.instant();
    MaterializationReceipt receipt =
        new MaterializationReceipt(
            eventId,
            Hashes.sha256("PaymentReconciled|" + quoteId),
            now.plus(RECEIPT_RETENTION),
            paymentId);

    // 2  already materialised: make sure the structured rows exist, then ack
    if (quote.isMaterialized()) {
      if (!structured.exists(quoteId)) {
        structured.insert(quote, receipt);
      }
      store.releaseOpenMarker(quote.journeyId(), quoteId);
      return MaterializeOutcome.ALREADY_MATERIALIZED;
    }

    try {
      quote.materialize(now);
    } catch (QuoteRuleException e) {
      throw QuoteWorkingStateService.toServiceException(
          errors, COMPONENT, "onPaymentReconciled", e);
    }
    // 4  one Aurora transaction, ON CONFLICT DO NOTHING
    structured.insert(quote, receipt);
    // 5  freeze the item + QuoteConverted; false means a concurrent consumer got there first
    store.markMaterialized(quote, converted(quote, paymentId));
    // 6
    store.releaseOpenMarker(quote.journeyId(), quoteId);
    return MaterializeOutcome.MATERIALIZED;
  }

  private OutboxEvent converted(WorkingQuote quote, String paymentId) {
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("quoteId", quote.quoteId());
    data.put("selectedOfferId", quote.envelope().selectedOfferId());
    data.put("paymentId", paymentId);
    data.put("journeyId", quote.journeyId());
    return new OutboxEvent(
        Ulid.mint(clock),
        "QuoteConverted",
        quote.quoteId(),
        quote.materializedAt(),
        quote.journeyId(),
        quote.leadId(),
        quote.configVersionRef(),
        null,
        data);
  }
}
