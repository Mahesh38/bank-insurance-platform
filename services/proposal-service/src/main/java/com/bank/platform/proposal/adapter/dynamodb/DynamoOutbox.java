package com.bank.platform.proposal.adapter.dynamodb;

import com.bank.common.events.Actor;
import com.bank.common.events.EventEnvelope;
import com.bank.common.events.EventJsonCodec;
import com.bank.platform.proposal.domain.OutboxEvent;
import com.bank.platform.proposal.domain.OutboxStore;
import com.bank.platform.proposal.domain.PendingOutboxItem;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.Put;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem;

/**
 * OUTBOX items of {@code {env}-proposal-working} (ADR-025 §3, CATALOGUE §6.2).
 *
 * <p>Written with the full {@code event-envelope/v1} JSON and {@code published = false} and
 * <strong>no {@code ttl}</strong>: an item expires only 7 days after it was published, so a
 * publisher outage of any length loses nothing. While unpublished the item carries the sparse index
 * keys {@code outboxPendingPK} / {@code outboxPendingSK}; marking it published removes them, so the
 * sweeper's query never sees published items and no {@code Scan} is needed (schemas/20 §4).
 */
@Component
public class DynamoOutbox implements OutboxStore {

  public static final String PENDING_INDEX = "GSI_OUTBOX_PENDING";
  public static final String PENDING_PK = "outboxPendingPK";
  public static final String PENDING_SK = "outboxPendingSK";
  static final String SOURCE = "proposal";
  static final String AGGREGATE_TYPE = "PROPOSAL";
  private static final String SYSTEM_PREFIX = "system:";

  private final DynamoDbClient dynamo;
  private final ProposalTableSettings settings;
  private final EventJsonCodec codec = new EventJsonCodec();

  public DynamoOutbox(DynamoDbClient dynamo, ProposalTableSettings settings) {
    this.dynamo = dynamo;
    this.settings = settings;
  }

  /** The Put for one event, to go in the caller's {@code TransactWriteItems}. */
  TransactWriteItem put(String partitionKey, OutboxEvent event) {
    Map<String, AttributeValue> item = DynamoWorkingProposalStore.key(partitionKey, sortKey(event));
    String occurredAt = DynamoTimes.format(event.occurredAt());
    item.put("eventId", AttributeValue.fromS(event.eventId()));
    item.put("eventType", AttributeValue.fromS(event.eventType()));
    item.put("aggregateId", AttributeValue.fromS(event.aggregateId()));
    item.put("occurredAt", AttributeValue.fromS(occurredAt));
    item.put("envelope", AttributeValue.fromS(codec.encode(envelope(event))));
    item.put("published", AttributeValue.fromBool(false));
    item.put(PENDING_PK, AttributeValue.fromS(pendingPartition(event.aggregateId())));
    item.put(PENDING_SK, AttributeValue.fromS(occurredAt + "#" + event.eventId()));
    return TransactWriteItem.builder()
        .put(Put.builder().tableName(settings.tableName()).item(item).build())
        .build();
  }

  @Override
  public List<PendingOutboxItem> findUnpublished(Instant cutoff, int limit) {
    String before = DynamoTimes.format(cutoff);
    List<PendingOutboxItem> found = new ArrayList<>();
    for (int shard = 0; shard < settings.outboxPendingShards(); shard++) {
      String partition = "OUTBOX_PENDING#" + shard;
      Map<String, AttributeValue> start = null;
      do {
        Map<String, AttributeValue> exclusiveStart = start;
        QueryResponse page =
            dynamo.query(
                r ->
                    r.tableName(settings.tableName())
                        .indexName(PENDING_INDEX)
                        .keyConditionExpression("#pk = :pk AND #sk < :before")
                        .expressionAttributeNames(Map.of("#pk", PENDING_PK, "#sk", PENDING_SK))
                        .expressionAttributeValues(
                            Map.of(
                                ":pk", AttributeValue.fromS(partition),
                                ":before", AttributeValue.fromS(before)))
                        .limit(limit)
                        .exclusiveStartKey(exclusiveStart));
        for (Map<String, AttributeValue> keys : page.items()) {
          // The index is eventually consistent: confirm on the item itself.
          Map<String, AttributeValue> item = read(keys.get("PK").s(), keys.get("SK").s());
          if (item != null && item.containsKey("published") && !item.get("published").bool()) {
            found.add(
                new PendingOutboxItem(
                    item.get("PK").s(),
                    item.get("eventId").s(),
                    DynamoTimes.parse(item.get("occurredAt").s()),
                    item.get("envelope").s()));
          }
        }
        start = page.hasLastEvaluatedKey() ? page.lastEvaluatedKey() : null;
      } while (start != null && !start.isEmpty() && found.size() < limit);
    }
    found.sort(
        Comparator.comparing(PendingOutboxItem::occurredAt)
            .thenComparing(PendingOutboxItem::eventId));
    return found.size() > limit ? List.copyOf(found.subList(0, limit)) : found;
  }

  @Override
  public boolean markPublished(PendingOutboxItem pending, Instant now) {
    Map<String, String> names = new HashMap<>();
    names.put("#published", "published");
    names.put("#publishedAt", "publishedAt");
    names.put("#ttl", "ttl");
    names.put("#pk", PENDING_PK);
    names.put("#sk", PENDING_SK);
    try {
      dynamo.updateItem(
          r ->
              r.tableName(settings.tableName())
                  .key(
                      DynamoWorkingProposalStore.key(
                          pending.partitionKey(), "OUTBOX#" + pending.eventId()))
                  .updateExpression(
                      "SET #published = :true, #publishedAt = :now, #ttl = :ttl REMOVE #pk, #sk")
                  .conditionExpression("#published = :false")
                  .expressionAttributeNames(names)
                  .expressionAttributeValues(
                      Map.of(
                          ":true", AttributeValue.fromBool(true),
                          ":false", AttributeValue.fromBool(false),
                          ":now", AttributeValue.fromS(DynamoTimes.format(now)),
                          // ADR-025 §3: retention counts from publication, not from the write.
                          ":ttl",
                              AttributeValue.fromN(
                                  Long.toString(
                                      now.plus(settings.outboxTtl()).getEpochSecond())))));
      return true;
    } catch (ConditionalCheckFailedException e) {
      return false;
    }
  }

  EventEnvelope envelope(OutboxEvent event) {
    String correlationId = mdcOr("correlationId", event.eventId());
    return EventEnvelope.builder()
        .eventId(event.eventId())
        .eventType(event.eventType())
        .schemaVersion(1)
        .source(SOURCE)
        .aggregate(AGGREGATE_TYPE, event.aggregateId())
        .occurredAt(event.occurredAt())
        .journeyId(event.journeyId())
        .leadId(event.leadId())
        .actor(actor(event.actorId()))
        .traceId(mdcOr("traceId", correlationId))
        .correlationId(correlationId)
        .configVersionRef(event.configVersionRef())
        .data(event.data())
        .build();
  }

  /**
   * {@code system:<service>} (internal hops) and background work are {@code SYSTEM}. Any other
   * {@code X-Actor-Id} is a bank user; its precise principal type is not on these internal hops yet
   * (open item in the LLD).
   */
  static Actor actor(String actorId) {
    if (actorId == null || actorId.isBlank()) {
      return Actor.system(SYSTEM_PREFIX + SOURCE);
    }
    if (actorId.startsWith(SYSTEM_PREFIX)) {
      return Actor.system(actorId);
    }
    return Actor.of(actorId, "BANK_USER", null);
  }

  private String pendingPartition(String aggregateId) {
    // One aggregate always lands on one shard, so the sweeper sees its events together.
    return "OUTBOX_PENDING#"
        + Math.floorMod(aggregateId.hashCode(), settings.outboxPendingShards());
  }

  private Map<String, AttributeValue> read(String pk, String sk) {
    Map<String, AttributeValue> item =
        dynamo
            .getItem(
                r ->
                    r.tableName(settings.tableName())
                        .key(DynamoWorkingProposalStore.key(pk, sk))
                        .consistentRead(true))
            .item();
    return item == null || item.isEmpty() ? null : item;
  }

  private static String sortKey(OutboxEvent event) {
    return "OUTBOX#" + event.eventId();
  }

  private static String mdcOr(String key, String fallback) {
    String value = MDC.get(key);
    return value == null || value.isBlank() ? fallback : value;
  }
}
