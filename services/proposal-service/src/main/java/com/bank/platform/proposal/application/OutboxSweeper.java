package com.bank.platform.proposal.application;

import com.bank.common.events.EventJsonCodec;
import com.bank.common.events.EventPublisher;
import com.bank.platform.proposal.domain.OutboxStore;
import com.bank.platform.proposal.domain.PendingOutboxItem;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ADR-025 §3 / CATALOGUE §6.2: republishes OUTBOX items still {@code published = false} longer than
 * {@code minAge} after they were written, so an outage of the stream publisher (or of MSK) longer
 * than the 24 h stream retention loses nothing. Each aggregate's items are published in {@code
 * occurredAt, eventId} order; after a failure the rest of that aggregate waits for the next run, so
 * order is never broken. Several replicas may run it: a duplicate publish is possible and consumers
 * dedupe on {@code eventId} (ADR-025 §4).
 *
 * <p>Built and scheduled by configuration ({@code proposal.outbox.*}); not a Spring bean on its
 * own so it exists only when a publisher is configured.
 */
public class OutboxSweeper {

  /** What one run did. */
  public record SweepResult(int published, int failed, int skipped) {}

  private static final Logger LOG = LoggerFactory.getLogger(OutboxSweeper.class);

  private final OutboxStore outbox;
  private final EventPublisher publisher;
  private final Clock clock;
  private final Duration minAge;
  private final int batchSize;
  private final EventJsonCodec codec = new EventJsonCodec();

  public OutboxSweeper(
      OutboxStore outbox, EventPublisher publisher, Clock clock, Duration minAge, int batchSize) {
    this.outbox = outbox;
    this.publisher = publisher;
    this.clock = clock;
    this.minAge = minAge;
    this.batchSize = batchSize;
  }

  public SweepResult sweep() {
    Instant now = clock.instant();
    List<PendingOutboxItem> pending = outbox.findUnpublished(now.minus(minAge), batchSize);
    Set<String> blocked = new HashSet<>();
    int published = 0;
    int failed = 0;
    int skipped = 0;
    for (PendingOutboxItem item : pending) {
      if (blocked.contains(item.partitionKey())) {
        skipped++;
        continue;
      }
      try {
        publisher.publish(codec.decode(item.envelopeJson()));
        outbox.markPublished(item, clock.instant());
        published++;
      } catch (RuntimeException e) {
        // Publisher outage or an unreadable item: keep the aggregate's order, retry next run.
        blocked.add(item.partitionKey());
        failed++;
        LOG.warn(
            "outbox sweep could not publish event {} of {}: {}",
            item.eventId(),
            item.partitionKey(),
            e.getMessage());
      }
    }
    if (failed > 0) {
      LOG.error(
          "outbox sweep left {} events unpublished ({} held back behind them)", failed, skipped);
    }
    return new SweepResult(published, failed, skipped);
  }
}
