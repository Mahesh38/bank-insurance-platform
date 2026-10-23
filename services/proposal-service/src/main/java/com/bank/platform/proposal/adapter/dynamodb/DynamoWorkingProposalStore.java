package com.bank.platform.proposal.adapter.dynamodb;

import com.bank.common.error.ErrorCodes;
import com.bank.common.error.ServiceErrors;
import com.bank.common.error.ServiceException;
import com.bank.platform.proposal.adapter.envelope.ProposalEnvelopeCodec;
import com.bank.platform.proposal.domain.CreateOutcome;
import com.bank.platform.proposal.domain.EnvelopeCipher;
import com.bank.platform.proposal.domain.IdempotencyClaim;
import com.bank.platform.proposal.domain.OutboxEvent;
import com.bank.platform.proposal.domain.ProposalEnvelope;
import com.bank.platform.proposal.domain.ProposalState;
import com.bank.platform.proposal.domain.SealedPayload;
import com.bank.platform.proposal.domain.StoreGuard;
import com.bank.platform.proposal.domain.WorkingProposal;
import com.bank.platform.proposal.domain.WorkingProposalStore;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
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
 * The working proposal in {@code {env}-proposal-working}, item shapes of schemas/20 §2: META,
 * OUTBOX, IDEM, the {@code JOURNEY#/OPEN_PROPOSAL} marker and the {@code APPNO#} marker.
 *
 * <p>Nominee and every form value are only inside {@code payloadEnc} (INV-PRP-05). Updates carry
 * {@code version = :expected AND attribute_not_exists(materializedAt)} (§3.2).
 */
@Component
public class DynamoWorkingProposalStore implements WorkingProposalStore {

  static final String PK = "PK";
  static final String SK = "SK";
  static final String META = "META";
  static final String OPEN_PROPOSAL = "OPEN_PROPOSAL";
  static final String IDEM = "IDEM";
  static final String APPNO = "APPNO";
  private static final String CONDITION_FAILED = "ConditionalCheckFailed";
  private static final String COMPONENT = "DynamoWorkingProposalStore";

  private static final List<String> REMOVABLE =
      List.of("applicationNumber", "underwritingState", "issuanceMode", "heldByPaymentId", "ttl");

  private final DynamoDbClient dynamo;
  private final ProposalTableSettings settings;
  private final EnvelopeCipher cipher;
  private final ProposalEnvelopeCodec codec;
  private final ServiceErrors errors;
  private final DynamoOutbox outbox;

  public DynamoWorkingProposalStore(
      DynamoDbClient dynamo,
      ProposalTableSettings settings,
      EnvelopeCipher cipher,
      ProposalEnvelopeCodec codec,
      ServiceErrors errors,
      DynamoOutbox outbox) {
    this.outbox = outbox;
    this.dynamo = dynamo;
    this.settings = settings;
    this.cipher = cipher;
    this.codec = codec;
    this.errors = errors;
  }

  @Override
  public CreateOutcome create(WorkingProposal proposal, IdempotencyClaim claim, OutboxEvent event) {
    WorkingProposal.Snapshot s = proposal.snapshot();
    SealedPayload sealed = seal(s);

    Map<String, AttributeValue> idem = key("IDEM#" + claim.key(), IDEM);
    idem.put("requestHash", str(claim.requestHash()));
    idem.put("responseRef", str(s.proposalId()));
    idem.put("createdAt", str(DynamoTimes.format(s.createdAt())));
    idem.put("ttl", epoch(s.createdAt().plus(settings.idempotencyTtl())));

    Map<String, AttributeValue> marker = key("JOURNEY#" + s.journeyId(), OPEN_PROPOSAL);
    marker.put("proposalId", str(s.proposalId()));
    marker.put("createdAt", str(DynamoTimes.format(s.createdAt())));

    List<TransactWriteItem> items =
        List.of(
            putIfAbsent(idem),
            putIfAbsent(marker),
            putIfAbsent(metaItem(s, sealed, 1)),
            outbox.put("PROPOSAL#" + s.proposalId(), event));
    try {
      dynamo.transactWriteItems(r -> r.transactItems(items));
    } catch (TransactionCanceledException e) {
      List<CancellationReason> reasons = reasons(e);
      if (failed(reasons, 0)) {
        return replay(claim, e);
      }
      String reason =
          failed(reasons, 1)
              ? "journey " + s.journeyId() + " already has an open proposal"
              : "proposal " + s.proposalId() + " could not be created: " + reasons;
      throw error(ErrorCodes.CONFLICT, "create", reason, e);
    }
    proposal.markPersisted(1, sealed.encryptionKeyId());
    return new CreateOutcome(s.proposalId(), false);
  }

  @Override
  public Optional<WorkingProposal> find(String proposalId) {
    Map<String, AttributeValue> item = readMeta(proposalId);
    return item == null ? Optional.empty() : Optional.of(fromItem(item));
  }

  @Override
  public void update(WorkingProposal proposal, StoreGuard guard, OutboxEvent event) {
    WorkingProposal.Snapshot s = proposal.snapshot();
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
    String condition = "#version = :expected AND attribute_not_exists(#materializedAt)";
    if (guard == StoreGuard.AWAITING_PAYMENT) {
      names.put("#state", "state");
      values.put(":awaiting", str(ProposalState.AWAITING_PAYMENT.name()));
      condition += " AND #state = :awaiting";
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
                    .key(key("PROPOSAL#" + s.proposalId(), META))
                    .updateExpression(expression)
                    .conditionExpression(condition)
                    .expressionAttributeNames(names)
                    .expressionAttributeValues(values)
                    .build())
            .build());
    int appNoIndex = -1;
    String appNo = proposal.unclaimedApplicationNumber();
    if (appNo != null) {
      Map<String, AttributeValue> marker = key("APPNO#" + appNo, APPNO);
      marker.put("proposalId", str(s.proposalId()));
      marker.put("createdAt", str(DynamoTimes.format(s.updatedAt())));
      appNoIndex = items.size();
      items.add(putIfAbsent(marker));
    }
    if (event != null) {
      items.add(outbox.put("PROPOSAL#" + s.proposalId(), event));
    }
    if (s.state().endsWithoutSale()) {
      items.add(markerDelete(s.journeyId(), s.proposalId()));
    }
    try {
      dynamo.transactWriteItems(r -> r.transactItems(items));
    } catch (TransactionCanceledException e) {
      List<CancellationReason> reasons = reasons(e);
      if (failed(reasons, 0)) {
        throw classifyUpdateFailure(s, guard, e);
      }
      if (failed(reasons, appNoIndex)) {
        throw error(
            ErrorCodes.CONFLICT,
            "update",
            "application number " + appNo + " already belongs to another proposal",
            e);
      }
      throw error(ErrorCodes.CONFLICT, "update", "transaction cancelled: " + reasons, e);
    }
    proposal.markPersisted(expected + 1, sealed.encryptionKeyId());
  }

  /** schemas/20 §3.4 step 5, plus OUTBOX {@code ProposalMaterialised} in the same transaction. */
  @Override
  public boolean markMaterialized(WorkingProposal proposal, OutboxEvent event) {
    WorkingProposal.Snapshot s = proposal.snapshot();
    Instant expiry =
        settings.retention().expiryOf(false, s.materializedAt(), s.updatedAt()).orElseThrow();
    Map<String, String> names = new HashMap<>();
    names.put("#materializedAt", "materializedAt");
    names.put("#state", "state");
    names.put("#paymentInFlight", "paymentInFlight");
    names.put("#heldByPaymentId", "heldByPaymentId");
    names.put("#updatedAt", "updatedAt");
    names.put("#ttl", "ttl");
    names.put("#version", "version");
    List<TransactWriteItem> items = new ArrayList<>();
    items.add(
        TransactWriteItem.builder()
            .update(
                Update.builder()
                    .tableName(settings.tableName())
                    .key(key("PROPOSAL#" + s.proposalId(), META))
                    .updateExpression(
                        "SET #materializedAt = :at, #state = :state, #paymentInFlight = :false,"
                            + " #updatedAt = :at, #ttl = :ttl, #version = #version + :one"
                            + " REMOVE #heldByPaymentId")
                    .conditionExpression("attribute_not_exists(#materializedAt)")
                    .expressionAttributeNames(names)
                    .expressionAttributeValues(
                        Map.of(
                            ":at", str(DynamoTimes.format(s.materializedAt())),
                            ":state", str(s.state().name()),
                            ":false", AttributeValue.fromBool(false),
                            ":ttl", epoch(expiry),
                            ":one", num(1)))
                    .build())
            .build());
    if (event != null) {
      items.add(outbox.put("PROPOSAL#" + s.proposalId(), event));
    }
    try {
      dynamo.transactWriteItems(r -> r.transactItems(items));
      return true;
    } catch (TransactionCanceledException e) {
      if (failed(reasons(e), 0)) {
        return false;
      }
      throw error(ErrorCodes.CONFLICT, "markMaterialized", "transaction cancelled", e);
    }
  }

  @Override
  public void releaseOpenMarker(String journeyId, String proposalId) {
    try {
      dynamo.deleteItem(
          r ->
              r.tableName(settings.tableName())
                  .key(key("JOURNEY#" + journeyId, OPEN_PROPOSAL))
                  .conditionExpression("#proposalId = :proposalId")
                  .expressionAttributeNames(Map.of("#proposalId", "proposalId"))
                  .expressionAttributeValues(Map.of(":proposalId", str(proposalId))));
    } catch (ConditionalCheckFailedException e) {
      // already released, or it names a newer proposal
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
      throw error(
          ErrorCodes.CONFLICT, "create", "idempotency key " + claim.key() + " is in flux", cause);
    }
    if (!claim.requestHash().equals(idem.get("requestHash").s())) {
      throw error(
          ErrorCodes.IDEMPOTENCY_CONFLICT,
          "create",
          "idempotency key " + claim.key() + " was used for a different request",
          cause);
    }
    return new CreateOutcome(idem.get("responseRef").s(), true);
  }

  private ServiceException classifyUpdateFailure(
      WorkingProposal.Snapshot s, StoreGuard guard, TransactionCanceledException e) {
    Map<String, AttributeValue> current = readMeta(s.proposalId());
    if (current == null) {
      return error(ErrorCodes.RESOURCE_NOT_FOUND, "update", "proposal " + s.proposalId(), e);
    }
    if (current.containsKey("materializedAt")) {
      return error(
          ErrorCodes.ILLEGAL_TRANSITION,
          "update",
          "proposal " + s.proposalId() + " is materialised and frozen",
          e);
    }
    long actual = Long.parseLong(current.get("version").n());
    if (actual != s.version()) {
      return error(
          ErrorCodes.CONFLICT,
          "update",
          "proposal " + s.proposalId() + " is at version " + actual + ", expected " + s.version(),
          e);
    }
    return error(
        ErrorCodes.ILLEGAL_TRANSITION,
        "update",
        "proposal " + s.proposalId() + " is " + current.get("state").s() + ", guard " + guard,
        e);
  }

  private Map<String, AttributeValue> readMeta(String proposalId) {
    Map<String, AttributeValue> item =
        dynamo
            .getItem(
                r ->
                    r.tableName(settings.tableName())
                        .key(key("PROPOSAL#" + proposalId, META))
                        .consistentRead(true))
            .item();
    return item == null || item.isEmpty() ? null : item;
  }

  private SealedPayload seal(WorkingProposal.Snapshot s) {
    return cipher.seal(codec.encode(s.envelope()), envelopeContext(s.proposalId()));
  }

  // Binds the ciphertext to its item: a payloadEnc copied onto another proposal will not decrypt.
  static String envelopeContext(String proposalId) {
    return "proposal-working|PROPOSAL#" + proposalId;
  }

  private Map<String, AttributeValue> metaItem(
      WorkingProposal.Snapshot s, SealedPayload sealed, long version) {
    Map<String, AttributeValue> item = key("PROPOSAL#" + s.proposalId(), META);
    item.put("GSI1PK", str("JOURNEY#" + s.journeyId()));
    item.put("GSI1SK", str("PROPOSAL#" + DynamoTimes.format(s.createdAt()) + "#" + s.proposalId()));
    item.put("proposalId", str(s.proposalId()));
    item.put("leadId", str(s.leadId()));
    item.put("journeyId", str(s.journeyId()));
    item.put("customerId", str(s.customerId()));
    item.put("quoteId", str(s.quoteId()));
    item.put("offerId", str(s.offerId()));
    item.put("consentId", str(s.consentId()));
    item.put("lob", str(s.lob()));
    putIfPresent(item, "applicationNumber", s.applicationNumber());
    item.put("schemaId", str(s.schemaId()));
    item.put("schemaVersion", str(s.schemaVersion()));
    item.put("state", str(s.state().name()));
    putIfPresent(item, "underwritingState", s.underwritingState());
    putIfPresent(item, "issuanceMode", s.issuanceMode());
    item.put("paymentInFlight", AttributeValue.fromBool(s.paymentInFlight()));
    putIfPresent(item, "heldByPaymentId", s.heldByPaymentId());
    item.put("payloadEnc", AttributeValue.fromB(SdkBytes.fromByteArray(sealed.ciphertext())));
    item.put("encryptionKeyId", str(sealed.encryptionKeyId()));
    item.put("payloadSchemaVersion", num(ProposalEnvelope.SCHEMA_VERSION));
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

  private WorkingProposal fromItem(Map<String, AttributeValue> item) {
    String proposalId = item.get("proposalId").s();
    String keyId = item.get("encryptionKeyId").s();
    byte[] plaintext =
        cipher.open(
            new SealedPayload(item.get("payloadEnc").b().asByteArray(), keyId),
            envelopeContext(proposalId));
    return WorkingProposal.restore(
        new WorkingProposal.Snapshot(
            proposalId,
            text(item, "leadId"),
            text(item, "journeyId"),
            text(item, "customerId"),
            text(item, "quoteId"),
            text(item, "offerId"),
            text(item, "consentId"),
            text(item, "lob"),
            text(item, "applicationNumber"),
            text(item, "schemaId"),
            text(item, "schemaVersion"),
            ProposalState.valueOf(text(item, "state")),
            text(item, "underwritingState"),
            text(item, "issuanceMode"),
            text(item, "configVersionRef"),
            item.containsKey("paymentInFlight") && item.get("paymentInFlight").bool(),
            text(item, "heldByPaymentId"),
            DynamoTimes.parse(text(item, "materializedAt")),
            Long.parseLong(item.get("version").n()),
            DynamoTimes.parse(text(item, "createdAt")),
            DynamoTimes.parse(text(item, "updatedAt")),
            // Envelope v1 items (before D8) are still readable (schemas/20 §2).
            codec.decode(
                plaintext,
                item.containsKey("payloadSchemaVersion")
                    ? Integer.parseInt(item.get("payloadSchemaVersion").n())
                    : 1,
                text(item, "schemaVersion")),
            keyId));
  }

  private TransactWriteItem markerDelete(String journeyId, String proposalId) {
    return TransactWriteItem.builder()
        .delete(
            Delete.builder()
                .tableName(settings.tableName())
                .key(key("JOURNEY#" + journeyId, OPEN_PROPOSAL))
                .conditionExpression("attribute_not_exists(PK) OR #proposalId = :markerProposalId")
                .expressionAttributeNames(Map.of("#proposalId", "proposalId"))
                .expressionAttributeValues(Map.of(":markerProposalId", str(proposalId)))
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

  private ServiceException error(String code, String operation, String reason, Exception e) {
    return errors
        .error(code)
        .component(COMPONENT)
        .operation(operation)
        .reason(reason)
        .cause(e)
        .build();
  }

  private static List<CancellationReason> reasons(TransactionCanceledException e) {
    return e.hasCancellationReasons() ? e.cancellationReasons() : null;
  }

  private static boolean failed(List<CancellationReason> reasons, int index) {
    return index >= 0
        && reasons != null
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

  private static AttributeValue epoch(Instant instant) {
    return num(instant.getEpochSecond());
  }
}
