package com.bank.platform.quotation.domain;

/** Result of a create: a new quote, or the quote an earlier identical request already created. */
public record CreateOutcome(String quoteId, boolean replayed) {}
