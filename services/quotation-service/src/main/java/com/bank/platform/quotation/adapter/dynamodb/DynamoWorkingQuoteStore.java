package com.bank.platform.quotation.adapter.dynamodb;

import com.bank.common.error.ErrorCodes;
import com.bank.common.error.ServiceErrors;
import com.bank.common.error.ServiceException;
import com.bank.platform.quotation.adapter.envelope.QuoteEnvelopeCodec;
import com.bank.platform.quotation.domain.CreateOutcome;
import com.bank.platform.quotation.domain.EnvelopeCipher;
import com.bank.platform.quotation.domain.IdempotencyClaim;
import com.bank.platform.quotation.domain.OutboxEvent;
import com.bank.platform.quotation.domain.QuoteEnvelope;
import com.bank.platform.quotation.domain.QuoteOffer;
import com.bank.platform.quotation.domain.QuoteState;
import com.bank.platform.quotation.domain.SealedPayload;
import com.bank.platform.quotation.domain.StoreGuard;
import com.bank.platform.quotation.domain.WorkingQuote;
import com.bank.platform.quotation.domain.WorkingQuoteStore;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.CancellationReason;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.Delete;
import software.amazon.awssdk.services.dynamodb.model.Put;
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem;
import software.amazon.awssdk.services.dynamodb.model.TransactionCanceledException;
import software.amazon.awssdk.services.dynamodb.model.Update;

/**
 * The working quote in {@code {env}-quotation-working}, exactly the item shapes of schemas/20 §1.
 *
 * <p>Every update carries {@code version = :expected AND attribute_not_exists(materializedAt)}
 * (§3.2), so a lost update is a {@code 409 CONFLICT} and a materialised item is frozen. Reads are
 * strongly consistent: gates, selection and materialise must not act on a stale replica.
 */
@Component
public class DynamoWorkingQuoteStore implements WorkingQuoteStore {

  static final String PK = "PK";
  static final String SK = "SK";
  static final String META = "META";
  static final String OPEN_QUOTE = "OPEN_QUOTE";
  static final String IDEM = "IDEM";
  private static final String CONDITION_FAILED = "ConditionalCheckFailed";
  private static final String COMPONENT = "DynamoWorkingQuoteStore";

  // Attributes an update may need to clear; anything else absent from the item is never written.
  private static final List<String> REMOVABLE =
      List.of(
          "validUntil",
          "provider",
          "providerJobId",
          "selectedOfferId",
          "selectedPremiumAmount",
          "selectedTaxAmount",
          "selectedSumAssured",
          "selectedInsurerCode",
          "selectedProductCode",
          "ttl");

  private final DynamoDbClient dynamo;
  private final QuoteTableSettings settings;
  private final EnvelopeCipher cipher;
  private final QuoteEnvelopeCodec codec;
  private final ServiceErrors errors;
  private final Clock clock;

  public DynamoWorkingQuoteStore(
      DynamoDbClient dynamo,
      QuoteTableSettings settings,
      EnvelopeCipher cipher,
      QuoteEnvelopeCodec codec,
      ServiceErrors errors,
      Clock clock) {
    this.dynamo = dynamo;
    this.settings = settings;
    this.cipher = cipher;
    this.codec = codec;
    this.errors = errors;
    this.clock = clock;
  }

  /** schemas/20 §3.1: IDEM, OPEN_QUOTE marker, META and OUTBOX in one TransactWriteItems. */
  @Override
  public CreateOutcome create(WorkingQuote quote, IdempotencyClaim claim, OutboxEvent event) {
    WorkingQuote.Snapshot s = quote.snapshot();
    SealedPayload sealed = seal(s);
    Map<String, AttributeValue> meta = metaItem(s, sealed, 1);

    Map<String, AttributeValue> idem = key("IDEM#" + claim.key(), IDEM);
    idem.put("requestHash", str(claim.requestHash()));
    idem.put("responseRef", str(s.quoteId()));
    idem.put("createdAt", str(DynamoTimes.format(s.createdAt())));
    idem.put("ttl", epoch(s.createdAt().plus(settings.idempotencyTtl())));

    Map<String, AttributeValue> marker = key("JOURNEY#" + s.journeyId(), OPEN_QUOTE);
    marker.put("quoteId", str(s.quoteId()));
    marker.put("createdAt", str(DynamoTimes.format(s.createdAt())));

    List<TransactWriteItem> items =
        List.of(
            putIfAbsent(idem),
            putIfAbsent(marker),
            putIfAbsent(meta),
            outboxPut(s.quoteId(), event));
    try {
      dynamo.transactWriteItems(r -> r.transactItems(items));
    } catch (TransactionCanceledException e) {
      List<CancellationReason> reasons =
          e.hasCancellationReasons() ? e.cancellationReasons() : null;
      if (failed(reasons, 0)) {
        return replay(claim, e);
      }
      String reason =
          failed(reasons, 1)
              ? "journey " + s.journeyId() + " already has an open quote"
              : "quote " + s.quoteId() + " could not be created: " + reasons;
      throw conflict(ErrorCodes.CONFLICT, "create", reason, e);
    }
    quote.markPersisted(1, sealed.encryptionKeyId());
    return new CreateOutcome(s.quoteId(), false);
  }

  @Override
  public Optional<WorkingQuote> find(String quoteId) {
    Map<String, AttributeValue> item = readMeta(quoteId);
    return item == null ? Optional.empty() : Optional.of(fromItem(item));
  }

  /** schemas/20 §3.2 (+ §3.3 guards). A quote ending without a sale releases its marker. */
  @Override
  public void update(WorkingQuote quote, StoreGuard guard, OutboxEvent event) {
    WorkingQuote.Snapshot s = quote.snapshot();
    long expected = s.version();
    SealedPayload sealed = seal(s);
    Map<String, AttributeValue> item = metaItem(s, sealed, expected);
    item.remove(PK);
    item.remove(SK);
    item.remove("version");

    Map<String, String> names = new HashMap<>();
    Map<String, AttributeValue> values = new HashMap<>();
    List<String> sets = new ArrayList<>();
    List<String> removes = new ArrayList<>();
    item.forEach(
        (name, value) -> {
          names.put("#" + name, name);
          values.put(":" + name, value);
          sets.add("#" + name + " = :" + name);
        });
    for (String name : REMOVABLE) {
      if (!item.containsKey(name)) {
        names.put("#" + name, name);
        removes.add("#" + name);
      }
    }
    names.put("#version", "version");
    names.put("#materializedAt", "materializedAt");
    values.put(":expected", num(expected));
    values.put(":one", num(1));
    sets.add("#version = #version + :one");

    StringBuilder condition =
        new StringBuilder("#version = :expected AND attribute_not_exists(#materializedAt)");
    switch (guard) {
      case SELECTABLE -> {
        names.put("#state", "state");
        names.put("#validUntil", "validUntil");
        values.put(":quoted", str(QuoteState.QUOTED.name()));
        values.put(":partial", str(QuoteState.PARTIALLY_QUOTED.name()));
        values.put(":now", str(DynamoTimes.format(clock.instant())));
        condition.append(" AND #state IN (:quoted, :partial) AND #validUntil > :now");
      }
      case SELECTED -> {
        names.put("#state", "state");
        values.put(":selected", str(QuoteState.SELECTED.name()));
        condition.append(" AND #state = :selected");
      }
      default -> {
        // optimistic lock and freeze only
      }
    }
    String expression =
        "SET "
            + String.join(", ", sets)
            + (removes.isEmpty() ? "" : " REMOVE " + String.join(", ", removes));

    List<TransactWriteItem> items = new ArrayList<>();
    items.add(
        TransactWriteItem.builder()
            .update(
                Update.builder()
                    .tableName(settings.tableName())
                    .key(key("QUOTE#" + s.quoteId(), META))
                    .updateExpression(expression)
                    .conditionExpression(condition.toString())
                    .expressionAttributeNames(names)
                    .expressionAttributeValues(values)
                    .build())
            .build());
    if (event != null) {
      items.add(outboxPut(s.quoteId(), event));
    }
    if (s.state().endsWithoutSale()) {
      items.add(markerDelete(s.journeyId(), s.quoteId()));
    }
    try {
      dynamo.transactWriteItems(r -> r.transactItems(items));
    } catch (TransactionCanceledException e) {
      throw classifyUpdateFailure(s, guard, e);
    }
    quote.markPersisted(expected + 1, sealed.encryptionKeyId());
  }

  /** schemas/20 §3.4 step 5. */
  @Override
  public boolean markMaterialized(WorkingQuote quote) {
    WorkingQuote.Snapshot s = quote.snapshot();
    Instant expiry =
        settings.retention().expiryOf(false, s.materializedAt(), s.updatedAt()).orElseThrow();
    try {
      dynamo.updateItem(
          r ->
              r.tableName(settings.tableName())
                  .key(key("QUOTE#" + s.quoteId(), META))
                  .updateExpression(
                      "SET #materializedAt = :at, #state = :state, #paymentInFlight = :false,"
                          + " #updatedAt = :at, #ttl = :ttl, #version = #version + :one")
                  .conditionExpression("attribute_not_exists(#materializedAt)")
                  .expressionAttributeNames(
                      Map.of(
                          "#materializedAt", "materializedAt",
                          "#state", "state",
                          "#paymentInFlight", "paymentInFlight",
                          "#updatedAt", "updatedAt",
                          "#ttl", "ttl",
                          "#version", "version"))
                  .expressionAttributeValues(
                      Map.of(
                          ":at", str(DynamoTimes.format(s.materializedAt())),
                          ":state", str(s.state().name()),
                          ":false", AttributeValue.fromBool(false),
                          ":ttl", epoch(expiry),
                          ":one", num(1))));
      return true;
    } catch (ConditionalCheckFailedException e) {
      return false;
    }
  }

  /** schemas/20 §3.4 step 6; a marker already gone or naming a newer quote is left alone. */
  @Override
  public void releaseOpenMarker(String journeyId, String quoteId) {
    try {
      dynamo.deleteItem(
          r ->
              r.tableName(settings.tableName())
                  .key(key("JOURNEY#" + journeyId, OPEN_QUOTE))
                  .conditionExpression("#quoteId = :quoteId")
                  .expressionAttributeNames(Map.of("#quoteId", "quoteId"))
                  .expressionAttributeValues(Map.of(":quoteId", str(quoteId))));
    } catch (ConditionalCheckFailedException e) {
      // nothing to release
    }
  }

  private CreateOutcome replay(IdempotencyClaim claim, TransactionCanceledException cause) {
    Map<String, AttributeValue> idem =
        dynamo
            .getItem(
                r ->
                    r.tableName(settings.tableName())
                        .key(key("IDEM#" + claim.key(), IDEM))
                        .consistentRead(true))
            .item();
    if (idem == null || idem.isEmpty()) {
      throw conflict(
          ErrorCodes.CONFLICT, "create", "idempotency key " + claim.key() + " is in flux", cause);
    }
    if (!claim.requestHash().equals(idem.get("requestHash").s())) {
      throw conflict(
          ErrorCodes.IDEMPOTENCY_CONFLICT,
          "create",
          "idempotency key " + claim.key() + " was used for a different request",
          cause);
    }
    return new CreateOutcome(idem.get("responseRef").s(), true);
  }

  private ServiceException classifyUpdateFailure(
      WorkingQuote.Snapshot s, StoreGuard guard, TransactionCanceledException e) {
    List<CancellationReason> reasons = e.hasCancellationReasons() ? e.cancellationReasons() : null;
    if (!failed(reasons, 0)) {
      return conflict(ErrorCodes.CONFLICT, "update", "transaction cancelled: " + reasons, e);
    }
    Map<String, AttributeValue> current = readMeta(s.quoteId());
    if (current == null) {
      return conflict(ErrorCodes.RESOURCE_NOT_FOUND, "update", "quote " + s.quoteId(), e);
    }
    if (current.containsKey("materializedAt")) {
      return conflict(
          ErrorCodes.ILLEGAL_TRANSITION,
          "update",
          "quote " + s.quoteId() + " is materialised and frozen",
          e);
    }
    long actual = Long.parseLong(current.get("version").n());
    if (actual != s.version()) {
      return conflict(
          ErrorCodes.CONFLICT,
          "update",
          "quote " + s.quoteId() + " is at version " + actual + ", expected " + s.version(),
          e);
    }
    String state = current.get("state").s();
    if (guard == StoreGuard.SELECTABLE
        && (QuoteState.QUOTED.name().equals(state)
            || QuoteState.PARTIALLY_QUOTED.name().equals(state))) {
      return conflict(
          ErrorCodes.QUOTE_EXPIRED, "update", "quote " + s.quoteId() + " validity has passed", e);
    }
    return conflict(
        ErrorCodes.ILLEGAL_TRANSITION,
        "update",
        "quote " + s.quoteId() + " is " + state + ", guard " + guard + " not met",
        e);
  }

  private Map<String, AttributeValue> readMeta(String quoteId) {
    Map<String, AttributeValue> item =
        dynamo
            .getItem(
                r ->
                    r.tableName(settings.tableName())
                        .key(key("QUOTE#" + quoteId, META))
                        .consistentRead(true))
            .item();
    return item == null || item.isEmpty() ? null : item;
  }

  private SealedPayload seal(WorkingQuote.Snapshot s) {
    return cipher.seal(codec.encode(s.envelope()), envelopeContext(s.quoteId()));
  }

  // Binds the ciphertext to its item: a payloadEnc copied onto another quote will not decrypt.
  static String envelopeContext(String quoteId) {
    return "quotation-working|QUOTE#" + quoteId;
  }

  private Map<String, AttributeValue> metaItem(
      WorkingQuote.Snapshot s, SealedPayload sealed, long version) {
    Map<String, AttributeValue> item = key("QUOTE#" + s.quoteId(), META);
    item.put("GSI1PK", str("JOURNEY#" + s.journeyId()));
    item.put("GSI1SK", str("QUOTE#" + DynamoTimes.format(s.createdAt()) + "#" + s.quoteId()));
    item.put("quoteId", str(s.quoteId()));
    item.put("leadId", str(s.leadId()));
    item.put("journeyId", str(s.journeyId()));
    item.put("customerId", str(s.customerId()));
    item.put("suitabilityId", str(s.suitabilityId()));
    item.put("consentId", str(s.consentId()));
    item.put("lob", str(s.lob()));
    putIfPresent(item, "productClass", s.productClass());
    item.put("mode", str(s.mode()));
    item.put("category", str(s.category()));
    item.put("state", str(s.state().name()));
    if (s.validUntil() != null) {
      item.put("validUntil", str(DynamoTimes.format(s.validUntil())));
    }
    putIfPresent(item, "provider", s.provider());
    putIfPresent(item, "providerJobId", s.providerJobId());
    // INV-QUO-05: derived from the envelope being written in the same item, never set separately.
    selected(s.envelope())
        .ifPresent(
            o -> {
              item.put("selectedOfferId", str(o.offerId()));
              item.put("selectedPremiumAmount", decimal(o.premiumAmount()));
              item.put("selectedTaxAmount", decimal(o.taxAmount()));
              item.put("selectedSumAssured", decimal(o.sumAssured()));
              item.put("selectedInsurerCode", str(o.insurerCode()));
              item.put("selectedProductCode", str(o.productCode()));
            });
    item.put("paymentInFlight", AttributeValue.fromBool(s.paymentInFlight()));
    item.put("payloadEnc", AttributeValue.fromB(SdkBytes.fromByteArray(sealed.ciphertext())));
    item.put("encryptionKeyId", str(sealed.encryptionKeyId()));
    item.put("payloadSchemaVersion", num(QuoteEnvelope.SCHEMA_VERSION));
    item.put("configVersionRef", str(s.configVersionRef()));
    item.put("version", num(version));
    item.put("createdAt", str(DynamoTimes.format(s.createdAt())));
    item.put("updatedAt", str(DynamoTimes.format(s.updatedAt())));
    settings
        .retention()
        .expiryOf(s.paymentInFlight(), s.materializedAt(), s.updatedAt())
        .ifPresent(expiry -> item.put("ttl", epoch(expiry)));
    return item;
  }

  private WorkingQuote fromItem(Map<String, AttributeValue> item) {
    String quoteId = item.get("quoteId").s();
    String keyId = item.get("encryptionKeyId").s();
    byte[] plaintext =
        cipher.open(
            new SealedPayload(item.get("payloadEnc").b().asByteArray(), keyId),
            envelopeContext(quoteId));
    return WorkingQuote.restore(
        new WorkingQuote.Snapshot(
            quoteId,
            text(item, "leadId"),
            text(item, "journeyId"),
            text(item, "customerId"),
            text(item, "suitabilityId"),
            text(item, "consentId"),
            text(item, "lob"),
            text(item, "productClass"),
            text(item, "mode"),
            text(item, "category"),
            QuoteState.valueOf(text(item, "state")),
            DynamoTimes.parse(text(item, "validUntil")),
            text(item, "provider"),
            text(item, "providerJobId"),
            text(item, "configVersionRef"),
            item.containsKey("paymentInFlight") && item.get("paymentInFlight").bool(),
            DynamoTimes.parse(text(item, "materializedAt")),
            Long.parseLong(item.get("version").n()),
            DynamoTimes.parse(text(item, "createdAt")),
            DynamoTimes.parse(text(item, "updatedAt")),
            codec.decode(plaintext),
            keyId));
  }

  private TransactWriteItem outboxPut(String quoteId, OutboxEvent event) {
    Map<String, AttributeValue> item = key("QUOTE#" + quoteId, "OUTBOX#" + event.eventId());
    item.put("eventId", str(event.eventId()));
    item.put("eventType", str(event.eventType()));
    item.put("aggregateId", str(event.aggregateId()));
    item.put("occurredAt", str(DynamoTimes.format(event.occurredAt())));
    Map<String, AttributeValue> attributes = new LinkedHashMap<>();
    event.attributes().forEach((k, v) -> attributes.put(k, str(v)));
    item.put("attributes", AttributeValue.fromM(attributes));
    // The stream publishes on write, so write time + 7 days is publish + 7 days (schemas/20 §1.1).
    item.put("ttl", epoch(event.occurredAt().plus(settings.outboxTtl())));
    return TransactWriteItem.builder()
        .put(Put.builder().tableName(settings.tableName()).item(item).build())
        .build();
  }

  private TransactWriteItem markerDelete(String journeyId, String quoteId) {
    return TransactWriteItem.builder()
        .delete(
            Delete.builder()
                .tableName(settings.tableName())
                .key(key("JOURNEY#" + journeyId, OPEN_QUOTE))
                .conditionExpression("attribute_not_exists(PK) OR #quoteId = :markerQuoteId")
                .expressionAttributeNames(Map.of("#quoteId", "quoteId"))
                .expressionAttributeValues(Map.of(":markerQuoteId", str(quoteId)))
                .build())
        .build();
  }

  private TransactWriteItem putIfAbsent(Map<String, AttributeValue> item) {
    return TransactWriteItem.builder()
        .put(
            Put.builder()
                .tableName(settings.tableName())
                .item(item)
                .conditionExpression("attribute_not_exists(PK)")
                .build())
        .build();
  }

  private ServiceException conflict(String code, String operation, String reason, Exception e) {
    return errors
        .error(code)
        .component(COMPONENT)
        .operation(operation)
        .reason(reason)
        .cause(e)
        .build();
  }

  private static Optional<QuoteOffer> selected(QuoteEnvelope envelope) {
    String id = envelope.selectedOfferId();
    return id == null
        ? Optional.empty()
        : envelope.offers().stream().filter(o -> o.offerId().equals(id)).findFirst();
  }

  private static boolean failed(List<CancellationReason> reasons, int index) {
    return reasons != null
        && reasons.size() > index
        && CONDITION_FAILED.equals(reasons.get(index).code());
  }

  static Map<String, AttributeValue> key(String pk, String sk) {
    Map<String, AttributeValue> key = new HashMap<>();
    key.put(PK, str(pk));
    key.put(SK, str(sk));
    return key;
  }

  private static void putIfPresent(Map<String, AttributeValue> item, String name, String value) {
    if (value != null) {
      item.put(name, str(value));
    }
  }

  private static String text(Map<String, AttributeValue> item, String name) {
    AttributeValue value = item.get(name);
    return value == null ? null : value.s();
  }

  private static AttributeValue str(String value) {
    return AttributeValue.fromS(value);
  }

  private static AttributeValue num(long value) {
    return AttributeValue.fromN(Long.toString(value));
  }

  // Exact decimal: N carries the plain string, never a double.
  private static AttributeValue decimal(BigDecimal value) {
    return AttributeValue.fromN((value == null ? BigDecimal.ZERO : value).toPlainString());
  }

  private static AttributeValue epoch(Instant instant) {
    return num(instant.getEpochSecond());
  }
}
