package com.bank.platform.quotation.adapter.dynamodb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bank.common.error.ErrorCodes;
import com.bank.common.error.PlatformLayer;
import com.bank.common.error.ServiceErrors;
import com.bank.common.error.ServiceException;
import com.bank.platform.quotation.adapter.crypto.AesGcmEnvelopeCipher;
import com.bank.platform.quotation.adapter.crypto.LocalDataKeyProvider;
import com.bank.platform.quotation.adapter.envelope.QuoteEnvelopeCodec;
import com.bank.platform.quotation.domain.CreateOutcome;
import com.bank.platform.quotation.domain.IdempotencyClaim;
import com.bank.platform.quotation.domain.NewQuote;
import com.bank.platform.quotation.domain.OutboxEvent;
import com.bank.platform.quotation.domain.QuoteOffer;
import com.bank.platform.quotation.domain.QuoteState;
import com.bank.platform.quotation.domain.StoreGuard;
import com.bank.platform.quotation.domain.Ulid;
import com.bank.platform.quotation.domain.WorkingQuote;
import com.bank.platform.quotation.domain.WorkingRetention;
import com.bank.platform.quotation.support.LocalDynamoDb;
import com.bank.platform.quotation.support.QuoteFixtures;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/** Against in-process DynamoDB Local: real condition expressions and transactions. */
class DynamoWorkingQuoteStoreTest {

  private static final Instant NOW = Instant.parse("2026-10-21T10:00:00.250Z");
  private static final Duration WORKING = Duration.ofDays(90);

  private final DynamoDbClient dynamo = LocalDynamoDb.client();
  private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
  private final DynamoWorkingQuoteStore store =
      new DynamoWorkingQuoteStore(
          dynamo,
          new QuoteTableSettings(
              LocalDynamoDb.TABLE,
              new WorkingRetention(WORKING, Duration.ofDays(30)),
              Duration.ofHours(24),
              Duration.ofDays(7)),
          new AesGcmEnvelopeCipher(
              new LocalDataKeyProvider("p7MSmdyOLeyBue5/ZJpPP3TE/ZQrLr4WSPjrhS0d0F8=", "test")),
          new QuoteEnvelopeCodec(),
          ServiceErrors.of("quotation", PlatformLayer.L5),
          clock);

  private WorkingQuote created(NewQuote request) {
    WorkingQuote quote = WorkingQuote.request(Ulid.mint(), request, NOW);
    store.create(quote, new IdempotencyClaim(Ulid.mint(), "hash"), event(quote));
    return quote;
  }

  private static OutboxEvent event(WorkingQuote quote) {
    return new OutboxEvent(Ulid.mint(), "QuoteRequested", quote.quoteId(), NOW, Map.of());
  }

  private Map<String, AttributeValue> item(String pk, String sk) {
    return dynamo
        .getItem(
            r ->
                r.tableName(LocalDynamoDb.TABLE)
                    .key(DynamoWorkingQuoteStore.key(pk, sk))
                    .consistentRead(true))
        .item();
  }

  private WorkingQuote selected() {
    WorkingQuote quote = created(QuoteFixtures.newQuote());
    QuoteOffer offer = QuoteFixtures.offer("INS01", "12300.00");
    quote.recordOffers(
        List.of(offer, QuoteFixtures.offer("INS02", "15000.00")),
        List.of(),
        NOW.plus(Duration.ofDays(7)),
        NOW);
    store.update(quote, StoreGuard.NONE, null);
    quote.select(offer.offerId(), NOW);
    store.update(quote, StoreGuard.SELECTABLE, event(quote));
    return quote;
  }

  @Test
  void createWritesMetaIdemMarkerAndOutboxInOneTransaction() {
    NewQuote request = QuoteFixtures.newQuote();
    WorkingQuote quote = WorkingQuote.request(Ulid.mint(), request, NOW);
    String idemKey = "journey:" + request.journeyId() + ":quote:1";
    OutboxEvent event = event(quote);

    CreateOutcome outcome = store.create(quote, new IdempotencyClaim(idemKey, "h1"), event);

    assertThat(outcome.replayed()).isFalse();
    assertThat(quote.version()).isEqualTo(1);
    Map<String, AttributeValue> meta = item("QUOTE#" + quote.quoteId(), "META");
    assertThat(meta.get("state").s()).isEqualTo("REQUESTED");
    assertThat(meta.get("version").n()).isEqualTo("1");
    assertThat(meta.get("ttl").n()).isEqualTo(Long.toString(NOW.plus(WORKING).getEpochSecond()));
    assertThat(meta.get("GSI1PK").s()).isEqualTo("JOURNEY#" + request.journeyId());
    assertThat(meta.get("encryptionKeyId").s()).isEqualTo("local:test");
    assertThat(meta.get("payloadSchemaVersion").n()).isEqualTo("1");
    assertThat(item("IDEM#" + idemKey, "IDEM").get("responseRef").s()).isEqualTo(quote.quoteId());
    assertThat(item("JOURNEY#" + request.journeyId(), "OPEN_QUOTE").get("quoteId").s())
        .isEqualTo(quote.quoteId());
    assertThat(item("QUOTE#" + quote.quoteId(), "OUTBOX#" + event.eventId()).get("eventType").s())
        .isEqualTo("QuoteRequested");
  }

  @Test
  void sameKeySameRequestReplaysAndDifferentRequestConflicts() {
    NewQuote request = QuoteFixtures.newQuote();
    WorkingQuote first = WorkingQuote.request(Ulid.mint(), request, NOW);
    store.create(first, new IdempotencyClaim("k-" + first.quoteId(), "same"), event(first));

    WorkingQuote retry = WorkingQuote.request(Ulid.mint(), request, NOW);
    CreateOutcome replay =
        store.create(retry, new IdempotencyClaim("k-" + first.quoteId(), "same"), event(retry));

    assertThat(replay).isEqualTo(new CreateOutcome(first.quoteId(), true));
    assertThat(item("QUOTE#" + retry.quoteId(), "META")).isNullOrEmpty();
    WorkingQuote other = WorkingQuote.request(Ulid.mint(), request, NOW);
    assertThatThrownBy(
            () ->
                store.create(
                    other, new IdempotencyClaim("k-" + first.quoteId(), "different"), event(other)))
        .isInstanceOf(ServiceException.class)
        .extracting(e -> ((ServiceException) e).getErrorResponse().getCode())
        .isEqualTo(ErrorCodes.IDEMPOTENCY_CONFLICT);
  }

  @Test
  void oneOpenQuotePerJourney() {
    String journeyId = Ulid.mint();
    created(QuoteFixtures.newQuote(journeyId));

    assertThatThrownBy(() -> created(QuoteFixtures.newQuote(journeyId)))
        .isInstanceOf(ServiceException.class)
        .extracting(e -> ((ServiceException) e).getErrorResponse().getCode())
        .isEqualTo(ErrorCodes.CONFLICT);
  }

  @Test
  void staleVersionIsConflict() {
    WorkingQuote quote = created(QuoteFixtures.newQuote());
    WorkingQuote stale = store.find(quote.quoteId()).orElseThrow();
    quote.startProviderJob("1SB", "job-1", NOW);
    store.update(quote, StoreGuard.NONE, null);

    stale.startProviderJob("1SB", "job-2", NOW);

    assertThatThrownBy(() -> store.update(stale, StoreGuard.NONE, null))
        .isInstanceOf(ServiceException.class)
        .extracting(e -> ((ServiceException) e).getErrorResponse().getCode())
        .isEqualTo(ErrorCodes.CONFLICT);
    WorkingQuote current = store.find(quote.quoteId()).orElseThrow();
    assertThat(current.version()).isEqualTo(2);
    assertThat(current.snapshot().providerJobId()).isEqualTo("job-1");
  }

  @Test
  void selectionWritesAttributesAndEnvelopeTogether() {
    WorkingQuote quote = selected();

    Map<String, AttributeValue> meta = item("QUOTE#" + quote.quoteId(), "META");
    WorkingQuote read = store.find(quote.quoteId()).orElseThrow();
    assertThat(meta.get("state").s()).isEqualTo("SELECTED");
    assertThat(meta.get("selectedOfferId").s()).isEqualTo(read.envelope().selectedOfferId());
    assertThat(meta.get("selectedPremiumAmount").n()).isEqualTo("12300.00");
    assertThat(meta.get("selectedInsurerCode").s()).isEqualTo("INS01");
    assertThat(read.selectedOffer().orElseThrow().offerState().name()).isEqualTo("SELECTED");
    assertThat(read.version()).isEqualTo(3);
  }

  @Test
  void selectionAfterValidityIsRefusedByTheStoreCondition() {
    WorkingQuote quote = created(QuoteFixtures.newQuote());
    QuoteOffer offer = QuoteFixtures.offer("INS01", "100");
    // validUntil already in the store's past, though the in-memory check below uses an earlier now
    quote.recordOffers(List.of(offer), List.of(), NOW.minusSeconds(1), NOW.minusSeconds(10));
    store.update(quote, StoreGuard.NONE, null);
    quote.select(offer.offerId(), NOW.minusSeconds(5));

    assertThatThrownBy(() -> store.update(quote, StoreGuard.SELECTABLE, null))
        .extracting(e -> ((ServiceException) e).getErrorResponse().getCode())
        .isEqualTo(ErrorCodes.QUOTE_EXPIRED);
  }

  @Test
  void ttlIsRemovedWhilePaymentInFlightAndRestoredAfter() {
    WorkingQuote quote = selected();
    String pk = "QUOTE#" + quote.quoteId();

    quote.markPaymentInFlight(NOW);
    store.update(quote, StoreGuard.SELECTED, null);
    assertThat(item(pk, "META")).doesNotContainKey("ttl");
    assertThat(item(pk, "META").get("paymentInFlight").bool()).isTrue();

    quote.paymentEnded(NOW);
    store.update(quote, StoreGuard.SELECTED, null);
    assertThat(item(pk, "META").get("ttl").n())
        .isEqualTo(Long.toString(NOW.plus(WORKING).getEpochSecond()));
  }

  @Test
  void materializedItemIsFrozen() {
    WorkingQuote quote = selected();
    WorkingQuote stale = store.find(quote.quoteId()).orElseThrow();
    quote.materialize(NOW);

    assertThat(store.markMaterialized(quote)).isTrue();
    assertThat(store.markMaterialized(quote)).isFalse();

    Map<String, AttributeValue> meta = item("QUOTE#" + quote.quoteId(), "META");
    assertThat(meta.get("state").s()).isEqualTo("CONVERTED");
    assertThat(meta.get("ttl").n())
        .isEqualTo(Long.toString(NOW.plus(Duration.ofDays(30)).getEpochSecond()));
    stale.paymentEnded(NOW);
    assertThatThrownBy(() -> store.update(stale, StoreGuard.NONE, null))
        .extracting(e -> ((ServiceException) e).getErrorResponse().getCode())
        .isEqualTo(ErrorCodes.ILLEGAL_TRANSITION);
  }

  @Test
  void failedQuoteReleasesTheJourneyMarker() {
    String journeyId = Ulid.mint();
    WorkingQuote quote = created(QuoteFixtures.newQuote(journeyId));
    quote.recordOffers(List.of(), List.of(), null, NOW);
    assertThat(quote.state()).isEqualTo(QuoteState.FAILED);

    store.update(quote, StoreGuard.NONE, event(quote));

    assertThat(item("JOURNEY#" + journeyId, "OPEN_QUOTE")).isNullOrEmpty();
    created(QuoteFixtures.newQuote(journeyId));
  }

  @Test
  void releaseLeavesAMarkerOfAnotherQuote() {
    String journeyId = Ulid.mint();
    WorkingQuote quote = created(QuoteFixtures.newQuote(journeyId));

    store.releaseOpenMarker(journeyId, "someone-else");
    assertThat(item("JOURNEY#" + journeyId, "OPEN_QUOTE")).isNotEmpty();
    store.releaseOpenMarker(journeyId, quote.quoteId());
    assertThat(item("JOURNEY#" + journeyId, "OPEN_QUOTE")).isNullOrEmpty();
  }

  @Test
  void envelopeCiphertextHoldsNoPlaintextPii() {
    WorkingQuote quote = selected();

    byte[] payload = item("QUOTE#" + quote.quoteId(), "META").get("payloadEnc").b().asByteArray();
    String raw = new String(payload, StandardCharsets.ISO_8859_1);

    assertThat(raw)
        .doesNotContain(QuoteFixtures.MEMBER_PINCODE)
        .doesNotContain("1990-04-12")
        .doesNotContain("LIFE_ASSURED");
    assertThat(store.find(quote.quoteId()).orElseThrow().envelope().members().get(0).pincode())
        .isEqualTo(QuoteFixtures.MEMBER_PINCODE);
  }

  @Test
  void updateOfMissingQuoteIsNotFound() {
    WorkingQuote ghost = WorkingQuote.request(Ulid.mint(), QuoteFixtures.newQuote(), NOW);
    ghost.markPersisted(1, "local:test");

    assertThat(store.find(ghost.quoteId())).isEmpty();
    assertThatThrownBy(() -> store.update(ghost, StoreGuard.NONE, null))
        .extracting(e -> ((ServiceException) e).getErrorResponse().getCode())
        .isEqualTo(ErrorCodes.RESOURCE_NOT_FOUND);
  }
}
