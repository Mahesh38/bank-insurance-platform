package com.bank.platform.quotation.application;

import com.bank.common.error.ErrorCodes;
import com.bank.common.error.ServiceErrors;
import com.bank.common.error.ServiceException;
import com.bank.common.events.Ulid;
import com.bank.platform.quotation.domain.CreateOutcome;
import com.bank.platform.quotation.domain.IdempotencyClaim;
import com.bank.platform.quotation.domain.InsurerError;
import com.bank.platform.quotation.domain.NewQuote;
import com.bank.platform.quotation.domain.OfferState;
import com.bank.platform.quotation.domain.OutboxEvent;
import com.bank.platform.quotation.domain.QuoteOffer;
import com.bank.platform.quotation.domain.QuoteRuleException;
import com.bank.platform.quotation.domain.SelectedPremium;
import com.bank.platform.quotation.domain.StoreGuard;
import com.bank.platform.quotation.domain.WorkingQuote;
import com.bank.platform.quotation.domain.WorkingQuoteStore;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;

/**
 * Commands on the working quote before payment (DB-DEC-0005). Each is: strongly consistent read,
 * lazy expiry (quotation LLD §4.5), domain transition, one conditional write — with its outbox item
 * when an event is due. Event {@code data} follows {@code contracts/events/schemas}.
 */
@Service
public class QuoteWorkingStateService {

  /** Result of {@code payment-release}. */
  public record ReleaseResult(WorkingQuote quote, boolean released) {}

  static final String QUOTE_EXPIRED_EVENT = "QuoteExpired";
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
   * a different body is {@code 409 IDEMPOTENCY_CONFLICT}. If the journey's open-quote marker names
   * a quote that has lapsed, that quote is expired (releasing the marker) and the create retried
   * once (LLD §4.1 step 5).
   */
  public CreateOutcome create(String idempotencyKey, NewQuote request) {
    if (idempotencyKey == null || idempotencyKey.isBlank()) {
      throw errors
          .error(ErrorCodes.MISSING_IDEMPOTENCY_KEY)
          .component(COMPONENT)
          .operation("create")
          .build();
    }
    try {
      return createOnce(idempotencyKey, request);
    } catch (ServiceException e) {
      if (!ErrorCodes.CONFLICT.equals(e.getErrorResponse().getCode())
          || !expireOpenQuote(request.journeyId())) {
        throw e;
      }
      return createOnce(idempotencyKey, request);
    }
  }

  private CreateOutcome createOnce(String idempotencyKey, NewQuote request) {
    Instant now = clock.instant();
    WorkingQuote quote = WorkingQuote.request(Ulid.mint(clock), request, now);
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("quoteId", quote.quoteId());
    data.put("journeyId", request.journeyId());
    putIfPresent(data, "productClass", request.productClass());
    data.put("customerId", request.customerId());
    return store.create(
        quote,
        new IdempotencyClaim(idempotencyKey, Hashes.sha256(request.toString())),
        event(quote, "QuoteRequested", now, null, data));
  }

  private boolean expireOpenQuote(String journeyId) {
    return store
        .openQuoteId(journeyId)
        .flatMap(store::find)
        .map(open -> expire(open, null))
        .orElse(false);
  }

  /** {@code getQuote} on the working item: lazily expires a lapsed quote before answering. */
  public WorkingQuote get(String quoteId) {
    WorkingQuote quote = load(quoteId, "get");
    expire(quote, null);
    return quote;
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
    WorkingQuote quote = load(quoteId, "select");
    failIfExpired(quote, "select", null);
    return apply(
        quote,
        "select",
        StoreGuard.SELECTABLE,
        "OfferSelected",
        null,
        q -> q.select(offerId, now()));
  }

  /**
   * {@code POST …/payment-hold} (LLD §4.4): {@code SET paymentInFlight, heldByPaymentId REMOVE
   * ttl}. The same payment again returns the same body without a write; a lapsed, unheld quote is
   * expired first and answers {@code 409 QUOTE_EXPIRED}.
   */
  public SelectedPremium holdForPayment(
      String quoteId, String paymentId, String offerId, String actorId) {
    WorkingQuote quote = load(quoteId, "holdForPayment");
    failIfExpired(quote, "holdForPayment", actorId);
    boolean changed = rule("holdForPayment", () -> quote.holdForPayment(paymentId, offerId, now()));
    if (changed) {
      try {
        store.update(quote, StoreGuard.HOLDABLE, null);
      } catch (ServiceException e) {
        // A concurrent hold won the optimistic lock: answer as if it had been read first.
        if (!ErrorCodes.CONFLICT.equals(e.getErrorResponse().getCode())) {
          throw e;
        }
        WorkingQuote current = load(quoteId, "holdForPayment");
        if (rule("holdForPayment", () -> current.holdForPayment(paymentId, offerId, now()))) {
          throw e;
        }
        return current.selectedPremium().orElseThrow();
      }
    }
    return quote.selectedPremium().orElseThrow();
  }

  /**
   * {@code POST …/payment-release}: only the holding payment releases; anything else is a no-op
   * {@code released = false}. {@code ttl} is restored to now + RET-WORKING.
   */
  public ReleaseResult releasePaymentHold(String quoteId, String paymentId) {
    WorkingQuote quote = load(quoteId, "releasePaymentHold");
    boolean released = rule("releasePaymentHold", () -> quote.releasePaymentHold(paymentId, now()));
    if (released) {
      store.update(quote, StoreGuard.SELECTED, null);
    }
    return new ReleaseResult(quote, released);
  }

  /**
   * Deprecated {@code GET …/selected} (schemas/20 §3.3 as first built): sets {@code
   * paymentInFlight} and removes {@code ttl}. Repeated reads while in flight do not write again.
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

  /** Clears a hold without a payment id (legacy {@code GET …/selected} hold). */
  public WorkingQuote paymentEnded(String quoteId) {
    return change(
        quoteId, "paymentEnded", StoreGuard.SELECTED, null, q -> q.paymentEnded(clock.instant()));
  }

  private void failIfExpired(WorkingQuote quote, String operation, String actorId) {
    if (expire(quote, actorId)) {
      throw errors
          .error(ErrorCodes.QUOTE_EXPIRED)
          .component(COMPONENT)
          .operation(operation)
          .reason("quote " + quote.quoteId() + " was valid until " + quote.validUntil())
          .build();
    }
  }

  /**
   * LLD §4.5 step 1: {@code state = EXPIRED}, OPEN_QUOTE marker deleted and OUTBOX {@code
   * QuoteExpired}, in one transaction. A concurrent change loses quietly: the caller re-reads.
   */
  private boolean expire(WorkingQuote quote, String actorId) {
    if (!quote.expireIfDue(now())) {
      return false;
    }
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("quoteId", quote.quoteId());
    data.put("journeyId", quote.journeyId());
    data.put("state", quote.state().name());
    data.put("expiredAt", quote.updatedAt().toString());
    store.update(
        quote,
        StoreGuard.NONE,
        event(quote, QUOTE_EXPIRED_EVENT, quote.updatedAt(), actorId, data));
    return true;
  }

  private WorkingQuote change(
      String quoteId,
      String operation,
      StoreGuard guard,
      String eventType,
      Consumer<WorkingQuote> transition) {
    return apply(load(quoteId, operation), operation, guard, eventType, null, transition);
  }

  private WorkingQuote apply(
      WorkingQuote quote,
      String operation,
      StoreGuard guard,
      String eventType,
      String actorId,
      Consumer<WorkingQuote> transition) {
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
        type == null ? null : event(quote, type, quote.updatedAt(), actorId, data(quote, type));
    store.update(quote, guard, event);
    return quote;
  }

  /** {@code data} per event schema (closed schemas: only declared fields). */
  static Map<String, Object> data(WorkingQuote quote, String type) {
    Map<String, Object> data = new LinkedHashMap<>();
    data.put("quoteId", quote.quoteId());
    data.put("state", quote.state().name());
    data.put("journeyId", quote.journeyId());
    switch (type) {
      case "QuoteCompleted" -> {
        data.put(
            "offerCount",
            (int)
                quote.envelope().offers().stream()
                    .filter(o -> o.offerState() != OfferState.INVALID)
                    .count());
        data.put("partialErrorCount", quote.envelope().partialErrors().size());
      }
      case "QuoteFailed" -> data.put("partialErrorCount", quote.envelope().partialErrors().size());
      case "OfferSelected" ->
          quote
              .selectedOffer()
              .ifPresent(
                  o -> {
                    data.put("offerId", o.offerId());
                    data.put("insurerCode", o.insurerCode());
                  });
      default -> {
        // ids and state only
      }
    }
    return data;
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

  private Instant now() {
    return clock.instant();
  }

  private OutboxEvent event(
      WorkingQuote quote, String type, Instant at, String actorId, Map<String, Object> data) {
    // Monotonic ULID: two events of one quote in the same millisecond keep their order (ADR-025).
    return new OutboxEvent(
        Ulid.mint(clock),
        type,
        quote.quoteId(),
        at,
        quote.journeyId(),
        quote.leadId(),
        quote.configVersionRef(),
        actorId,
        data);
  }

  private static void putIfPresent(Map<String, Object> data, String key, Object value) {
    if (value != null) {
      data.put(key, value);
    }
  }
}
