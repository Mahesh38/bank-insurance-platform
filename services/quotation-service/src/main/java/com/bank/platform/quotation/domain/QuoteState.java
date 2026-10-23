package com.bank.platform.quotation.domain;

/** Quote lifecycle (quotation-service API reference §2; CHECK {@code ck_quote_state}). */
public enum QuoteState {
  REQUESTED,
  IN_PROGRESS,
  QUOTED,
  PARTIALLY_QUOTED,
  SELECTED,
  CONVERTED,
  EXPIRED,
  FAILED,
  TIMED_OUT,
  REJECTED;

  /** {@code PARTIALLY_QUOTED} is success ({@code INV-QUO-02}): both may be selected from. */
  public boolean isSelectable() {
    return this == QUOTED || this == PARTIALLY_QUOTED;
  }

  /** States lazy expiry may move to {@code EXPIRED} once {@code validUntil} passed (LLD §4.5). */
  public boolean isExpirable() {
    return this == QUOTED || this == PARTIALLY_QUOTED || this == SELECTED;
  }

  /**
   * States that close the quote without a sale. The journey's open-quote marker is released in the
   * same write so a re-quote can start (schemas/20 §1.1). {@code CONVERTED} releases it too, but
   * only after materialisation (schemas/20 §3.4 step 6).
   */
  public boolean endsWithoutSale() {
    return this == EXPIRED || this == FAILED || this == TIMED_OUT || this == REJECTED;
  }
}
