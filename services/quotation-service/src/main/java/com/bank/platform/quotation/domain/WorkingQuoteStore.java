package com.bank.platform.quotation.domain;

import java.util.Optional;

/** Port: the working quote while the sale is open (DynamoDB {@code {env}-quotation-working}). */
public interface WorkingQuoteStore {

  /**
   * Creates the quote, claims the idempotency key and the journey's open-quote marker, and writes
   * {@code event}, all in one transaction. A replay of the same request returns the original id.
   */
  CreateOutcome create(WorkingQuote quote, IdempotencyClaim claim, OutboxEvent event);

  /** Strongly consistent read. */
  Optional<WorkingQuote> find(String quoteId);

  /**
   * Writes the quote if it is still at the version it was read at and not materialised, plus {@code
   * guard}; advances its version. {@code event} may be null.
   */
  void update(WorkingQuote quote, StoreGuard guard, OutboxEvent event);

  /**
   * Freezes the item after the structured rows committed and writes {@code event} ({@code
   * QuoteConverted}) in the same transaction, so the event exists exactly when the freeze does.
   * Returns false when another consumer already did.
   */
  boolean markMaterialized(WorkingQuote quote, OutboxEvent event);

  /** The quote the journey's {@code OPEN_QUOTE} marker names, if any (strongly consistent). */
  Optional<String> openQuoteId(String journeyId);

  /** Deletes the journey's open-quote marker if it still names this quote. */
  void releaseOpenMarker(String journeyId, String quoteId);
}
