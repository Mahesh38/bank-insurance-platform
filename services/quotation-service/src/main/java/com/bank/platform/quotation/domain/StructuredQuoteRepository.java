package com.bank.platform.quotation.domain;

/** Port: the structured quote record in Aurora schema {@code quotation} after RECONCILED. */
public interface StructuredQuoteRepository {

  boolean exists(String quoteId);

  /**
   * Inserts {@code quotation.quote}, one {@code quotation.offer} per offer and the receipt in ONE
   * transaction; every insert is a no-op if the row already exists (schemas/20 §3.4 step 4).
   */
  void insert(WorkingQuote quote, MaterializationReceipt receipt);
}
