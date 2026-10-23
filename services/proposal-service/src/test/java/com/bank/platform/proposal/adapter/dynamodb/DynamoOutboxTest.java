package com.bank.platform.proposal.adapter.dynamodb;

import static org.assertj.core.api.Assertions.assertThat;

import com.bank.common.events.InMemoryEventPublisher;
import com.bank.common.events.Ulid;
import com.bank.platform.proposal.application.OutboxSweeper;
import com.bank.platform.proposal.domain.OutboxEvent;
import com.bank.platform.proposal.domain.PendingOutboxItem;
import com.bank.platform.proposal.domain.WorkingRetention;
import com.bank.platform.proposal.support.EventSchemas;
import com.bank.platform.proposal.support.LocalDynamoDb;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/** ADR-025 §3 outbox on DynamoDB Local: published flag, ttl at publish, sweeper order. */
class DynamoOutboxTest {

  private static final String TABLE = "test-proposal-outbox";
  private static final Instant NOW = Instant.parse("2026-10-22T09:00:00.000Z");

  private final DynamoDbClient dynamo = LocalDynamoDb.client();
  private final ProposalTableSettings settings =
      new ProposalTableSettings(
          TABLE,
          new WorkingRetention(Duration.ofDays(90), Duration.ofDays(30)),
          Duration.ofHours(24),
          Duration.ofDays(7),
          3);
  private final DynamoOutbox outbox = new DynamoOutbox(dynamo, settings);

  DynamoOutboxTest() {
    LocalDynamoDb.createTable(dynamo, TABLE);
    // Isolated table per run: drain anything a previous test of this class left behind.
    outbox
        .findUnpublished(NOW.plus(Duration.ofDays(3650)), 1000)
        .forEach(item -> outbox.markPublished(item, NOW));
  }

  private OutboxEvent write(String proposalId, Instant at) {
    OutboxEvent event =
        new OutboxEvent(
            Ulid.mint(Clock.fixed(at, ZoneOffset.UTC)),
            "ProposalWithdrawn",
            proposalId,
            at,
            proposalId,
            null,
            "cfg-1",
            "system:payment",
            Map.of("proposalId", proposalId, "state", "WITHDRAWN", "journeyId", proposalId));
    dynamo.transactWriteItems(r -> r.transactItems(outbox.put("PROPOSAL#" + proposalId, event)));
    return event;
  }

  private Map<String, AttributeValue> item(String proposalId, String eventId) {
    return dynamo
        .getItem(
            r ->
                r.tableName(TABLE)
                    .key(
                        DynamoWorkingProposalStore.key(
                            "PROPOSAL#" + proposalId, "OUTBOX#" + eventId))
                    .consistentRead(true))
        .item();
  }

  @Test
  void sweeperRepublishesOldItemsInOrderAndHoldsBackAnAggregateAfterAFailure() {
    String a = Ulid.mint();
    String b = Ulid.mint();
    Instant old = NOW.minus(Duration.ofMinutes(10));
    OutboxEvent a1 = write(a, old);
    OutboxEvent a2 = write(a, old.plusMillis(1));
    OutboxEvent a3 = write(a, old.plusMillis(2));
    OutboxEvent b1 = write(b, old.plusMillis(1));
    OutboxEvent recent = write(b, NOW.minus(Duration.ofMinutes(1)));

    List<PendingOutboxItem> pending = outbox.findUnpublished(NOW.minus(Duration.ofMinutes(5)), 50);
    assertThat(pending)
        .extracting(PendingOutboxItem::eventId)
        .containsExactly(a1.eventId(), a2.eventId(), b1.eventId(), a3.eventId());
    pending.forEach(p -> assertThat(EventSchemas.validateEnvelope(p.envelopeJson())).isEmpty());

    InMemoryEventPublisher publisher = new InMemoryEventPublisher();
    publisher.failWhen(e -> e.eventId().equals(a2.eventId()));
    OutboxSweeper sweeper =
        new OutboxSweeper(
            outbox, publisher, Clock.fixed(NOW, ZoneOffset.UTC), Duration.ofMinutes(5), 50);

    assertThat(sweeper.sweep()).isEqualTo(new OutboxSweeper.SweepResult(2, 1, 1));
    assertThat(publisher.published())
        .extracting(e -> e.eventId())
        .containsExactly(a1.eventId(), b1.eventId());
    Map<String, AttributeValue> published = item(a, a1.eventId());
    assertThat(published.get("published").bool()).isTrue();
    assertThat(published.get("ttl").n())
        .isEqualTo(Long.toString(NOW.plus(Duration.ofDays(7)).getEpochSecond()));
    assertThat(published).doesNotContainKey(DynamoOutbox.PENDING_PK);
    assertThat(item(a, a3.eventId()).get("published").bool()).isFalse();
    assertThat(item(b, recent.eventId())).doesNotContainKey("ttl");

    publisher.failWhen(e -> false);
    assertThat(sweeper.sweep()).isEqualTo(new OutboxSweeper.SweepResult(2, 0, 0));
    assertThat(publisher.published())
        .extracting(e -> e.eventId())
        .containsExactly(a1.eventId(), b1.eventId(), a2.eventId(), a3.eventId());
    assertThat(publisher.published().get(0).actor().type()).isEqualTo("SYSTEM");
  }

  @Test
  void markPublishedIsConditional() {
    OutboxEvent event = write(Ulid.mint(), NOW.minus(Duration.ofHours(1)));
    PendingOutboxItem item =
        new PendingOutboxItem("PROPOSAL#" + event.aggregateId(), event.eventId(), NOW, "{}");

    assertThat(outbox.markPublished(item, NOW)).isTrue();
    assertThat(outbox.markPublished(item, NOW.plusSeconds(60))).isFalse();
  }

  @Test
  void actorTypes() {
    assertThat(DynamoOutbox.actor(null).id()).isEqualTo("system:proposal");
    assertThat(DynamoOutbox.actor("system:payment").type()).isEqualTo("SYSTEM");
    assertThat(DynamoOutbox.actor("RM123").type()).isEqualTo("BANK_USER");
  }
}
