package com.bank.common.events;

/**
 * Consumer dedupe record (ADR-025 §4). {@link #claim} must run in the same transaction as the
 * consumer's own writes, so "processed" and "the work" commit or roll back together.
 */
public interface ProcessedEventStore {

  /**
   * Records {@code eventId} as processed by {@code consumer}.
   *
   * @return true if this is the first claim; false if it was already processed (a duplicate)
   */
  boolean claim(String eventId, String consumer);
}
