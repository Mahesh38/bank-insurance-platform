package com.bank.platform.proposal.domain;

import java.time.Instant;
import java.util.List;

/**
 * Port: the DynamoDB OUTBOX items of this service (ADR-025 §3, schemas/20 §1.1). A stream consumer
 * normally publishes them within seconds; the sweeper uses this port to republish what is still
 * unpublished after the configured age, so a stream outage loses nothing.
 */
public interface OutboxStore {

  /**
   * Unpublished items written before {@code cutoff}, oldest first, at most {@code limit}. Read from
   * the sparse pending index (no {@code Scan}, schemas/20 §4) and re-checked with a strongly
   * consistent read, so an item already published is never returned.
   */
  List<PendingOutboxItem> findUnpublished(Instant cutoff, int limit);

  /**
   * {@code published = true} and {@code ttl = now + outbox retention}, conditional on {@code
   * published = false}. Returns false when another publisher got there first.
   */
  boolean markPublished(PendingOutboxItem item, Instant now);
}
