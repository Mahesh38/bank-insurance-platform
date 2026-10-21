package com.bank.platform.quotation.application;

import com.bank.common.error.ErrorCodes;
import com.bank.common.error.ServiceErrors;
import com.bank.common.error.ServiceException;
import com.bank.platform.quotation.domain.CreateOutcome;
import com.bank.platform.quotation.domain.IdempotencyClaim;
import com.bank.platform.quotation.domain.InsurerError;
import com.bank.platform.quotation.domain.NewQuote;
import com.bank.platform.quotation.domain.OutboxEvent;
import com.bank.platform.quotation.domain.QuoteOffer;
import com.bank.platform.quotation.domain.QuoteRuleException;
import com.bank.platform.quotation.domain.SelectedPremium;
import com.bank.platform.quotation.domain.StoreGuard;
import com.bank.platform.quotation.domain.Ulid;
import com.bank.platform.quotation.domain.WorkingQuote;
import com.bank.platform.quotation.domain.WorkingQuoteStore;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;

/**
 * Commands on the working quote before payment (DB-DEC-0005). Each is: strongly consistent read,
 * domain transition, one conditional write — with its outbox item when an event is due.
 */
@Service
public class QuoteWorkingStateService {

  private static final String COMPONENT = "QuoteWorkingStateService";

  private final WorkingQuoteStore store;
  private final ServiceErrors errors;
  private final Clock clock;

  public QuoteWorkingStateService(WorkingQuoteStore store, ServiceErrors errors, Clock clock) {
    this.store = store;
    this.errors = errors;
    this.clock = clock;
  }

  /**
   * schemas/20 §3.1. A replay with the same key and body returns the first quote; the same key with
   * a different body is {@code 409 IDEMPOTENCY_CONFLICT}.
   */
  public CreateOutcome create(String idempotencyKey, NewQuote request) {
    if (idempotencyKey == null || idempotencyKey.isBlank()) {
      throw errors
          .error(ErrorCodes.MISSING_IDEMPOTENCY_KEY)
          .component(COMPONENT)
          .operation("create")
          .build();
    }
    Instant now = clock.instant();
    WorkingQuote quote = WorkingQuote.request(Ulid.mint(clock), request, now);
    OutboxEvent event =
        event(quote, "QuoteRequested", now, Map.of("journeyId", request.journeyId()));
    return store.create(
        quote, new IdempotencyClaim(idempotencyKey, Hashes.sha256(request.toString())), event);
  }

  public WorkingQuote get(String quoteId) {
    return load(quoteId, "get");
  }

  public WorkingQuote startProviderJob(String quoteId, String provider, String providerJobId) {
    return change(
        quoteId,
        "startProviderJob",
        StoreGuard.NONE,
        null,
        q -> q.startProviderJob(provider, providerJobId, clock.instant()));
  }

  /** Emits {@code QuoteCompleted} (or {@code QuoteFailed} when no offer was valid). */
  public WorkingQuote recordOffers(
      String quoteId, List<QuoteOffer> offers, List<InsurerError> failures, Instant validUntil) {
    return change(
        quoteId,
        "recordOffers",
        StoreGuard.NONE,
        "QuoteCompleted",
        q -> q.recordOffers(offers, failures, validUntil, clock.instant()));
  }

  /** {@code INV-QUO-05}: selected attributes and envelope in one conditional write. */
  public WorkingQuote select(String quoteId, String offerId) {
    return change(
        quoteId,
        "select",
        StoreGuard.SELECTABLE,
        "OfferSelected",
        q -> q.select(offerId, clock.instant()));
  }

  /**
   * Payment's read of the selected premium (schemas/20 §3.3): sets {@code paymentInFlight} and
   * removes {@code ttl}. Repeated reads while in flight do not write again.
   */
  public SelectedPremium markPaymentInFlight(String quoteId) {
    WorkingQuote quote = load(quoteId, "markPaymentInFlight");
    boolean alreadyInFlight = quote.paymentInFlight();
    SelectedPremium premium =
        rule("markPaymentInFlight", () -> quote.markPaymentInFlight(clock.instant()));
    if (!alreadyInFlight) {
      store.update(quote, StoreGuard.SELECTED, null);
    }
    return premium;
  }

  /** {@code PaymentDeclined} / {@code PaymentExpired}: the quote may expire again. */
  public WorkingQuote paymentEnded(String quoteId) {
    return change(
        quoteId, "paymentEnded", StoreGuard.SELECTED, null, q -> q.paymentEnded(clock.instant()));
  }

  private WorkingQuote change(
      String quoteId,
      String operation,
      StoreGuard guard,
      String eventType,
      Consumer<WorkingQuote> transition) {
    WorkingQuote quote = load(quoteId, operation);
    rule(
        operation,
        () -> {
          transition.accept(quote);
          return null;
        });
    String type =
        "QuoteCompleted".equals(eventType) && quote.state().endsWithoutSale()
            ? "QuoteFailed"
            : eventType;
    OutboxEvent event =
        type == null
            ? null
            : event(quote, type, quote.updatedAt(), Map.of("state", quote.state().name()));
    store.update(quote, guard, event);
    return quote;
  }

  WorkingQuote load(String quoteId, String operation) {
    return store
        .find(quoteId)
        .orElseThrow(
            () ->
                errors
                    .error(ErrorCodes.RESOURCE_NOT_FOUND)
                    .component(COMPONENT)
                    .operation(operation)
                    .reason("quote " + quoteId)
                    .build());
  }

  <T> T rule(String operation, Supplier<T> action) {
    try {
      return action.get();
    } catch (QuoteRuleException e) {
      throw toServiceException(errors, COMPONENT, operation, e);
    }
  }

  static ServiceException toServiceException(
      ServiceErrors errors, String component, String operation, QuoteRuleException e) {
    return errors
        .error(e.code())
        .component(component)
        .operation(operation)
        .reason(e.getMessage())
        .cause(e)
        .build();
  }

  private OutboxEvent event(
      WorkingQuote quote, String type, Instant at, Map<String, String> attributes) {
    return new OutboxEvent(Ulid.mint(clock), type, quote.quoteId(), at, attributes);
  }
}
