package com.bank.platform.quotation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bank.common.error.ErrorCodes;
import com.bank.common.error.ServiceException;
import com.bank.platform.quotation.domain.CreateOutcome;
import com.bank.platform.quotation.domain.NewQuote;
import com.bank.platform.quotation.domain.QuoteOffer;
import com.bank.platform.quotation.domain.QuoteState;
import com.bank.platform.quotation.domain.Ulid;
import com.bank.platform.quotation.domain.WorkingQuote;
import com.bank.platform.quotation.support.EmbeddedDynamoDbTestConfig;
import com.bank.platform.quotation.support.EventSchemas;
import com.bank.platform.quotation.support.LocalDynamoDb;
import com.bank.platform.quotation.support.QuoteFixtures;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

/** Full path: DynamoDB Local working item → H2 (PostgreSQL mode) structured rows, via HTTP too. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(EmbeddedDynamoDbTestConfig.class)
class QuoteMaterializationIntegrationTest {

  @Autowired private QuoteWorkingStateService quotes;
  @Autowired private QuoteMaterializer materializer;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private DynamoDbClient dynamo;
  @Autowired private MockMvc mvc;

  private String selectedQuote(String journeyId) {
    NewQuote request = QuoteFixtures.newQuote(journeyId);
    CreateOutcome created = quotes.create("journey:" + journeyId + ":quote:1", request);
    QuoteOffer chosen = QuoteFixtures.offer("INS01", "12300.00");
    quotes.startProviderJob(created.quoteId(), "1SB", "job-" + created.quoteId());
    quotes.recordOffers(
        created.quoteId(),
        List.of(
            chosen, QuoteFixtures.offer("INS02", "15000.00"), QuoteFixtures.offer("INS03", "0")),
        List.of(),
        Instant.now().plus(Duration.ofDays(7)));
    quotes.select(created.quoteId(), chosen.offerId());
    return created.quoteId();
  }

  private Map<String, AttributeValue> meta(String quoteId) {
    return dynamo
        .getItem(
            r ->
                r.tableName(LocalDynamoDb.TABLE)
                    .key(
                        Map.of(
                            "PK", AttributeValue.fromS("QUOTE#" + quoteId),
                            "SK", AttributeValue.fromS("META")))
                    .consistentRead(true))
        .item();
  }

  private int count(String sql, String quoteId) {
    return jdbc.queryForObject(sql, Integer.class, quoteId);
  }

  @Test
  void createReplaysByIdempotencyKey() {
    NewQuote request = QuoteFixtures.newQuote();
    CreateOutcome first = quotes.create("k-" + request.journeyId(), request);
    CreateOutcome again = quotes.create("k-" + request.journeyId(), request);

    assertThat(again.quoteId()).isEqualTo(first.quoteId());
    assertThat(again.replayed()).isTrue();
    assertThatThrownBy(() -> quotes.create(" ", request))
        .extracting(e -> ((ServiceException) e).getErrorResponse().getCode())
        .isEqualTo(ErrorCodes.MISSING_IDEMPOTENCY_KEY);
    assertThatThrownBy(() -> quotes.get(Ulid.mint()))
        .extracting(e -> ((ServiceException) e).getErrorResponse().getCode())
        .isEqualTo(ErrorCodes.RESOURCE_NOT_FOUND);
  }

  private List<Map<String, AttributeValue>> outbox(String quoteId) {
    return dynamo
        .query(
            r ->
                r.tableName(LocalDynamoDb.TABLE)
                    .keyConditionExpression("PK = :pk AND begins_with(SK, :sk)")
                    .expressionAttributeValues(
                        Map.of(
                            ":pk", AttributeValue.fromS("QUOTE#" + quoteId),
                            ":sk", AttributeValue.fromS("OUTBOX#")))
                    .consistentRead(true))
        .items();
  }

  private static String hold(String paymentId, String offerId) {
    return "{\"paymentId\":\""
        + paymentId
        + "\",\"proposalId\":\""
        + Ulid.mint()
        + "\",\"offerId\":\""
        + offerId
        + "\"}";
  }

  private static String release(String paymentId) {
    return "{\"paymentId\":\"" + paymentId + "\",\"paymentState\":\"DECLINED\"}";
  }

  @Test
  void paymentHoldIsIdempotentExclusiveAndReleasedOnlyByItsPayment() throws Exception {
    String quoteId = selectedQuote(Ulid.mint());
    String offerId = quotes.get(quoteId).envelope().selectedOfferId();
    String paymentId = Ulid.mint();

    for (int i = 0; i < 2; i++) {
      mvc.perform(
              post("/internal/v1/quotes/{id}/payment-hold", quoteId)
                  .header("X-Actor-Id", "system:payment")
                  .header("Idempotency-Key", "payment:" + paymentId + ":quote-hold")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(hold(paymentId, offerId)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.premiumAmount").value(12300.00))
          .andExpect(jsonPath("$.premiumFrequency").value("YEARLY"))
          .andExpect(jsonPath("$.validUntil").exists())
          .andExpect(jsonPath("$.currency").value("INR"));
    }
    Map<String, AttributeValue> held = meta(quoteId);
    assertThat(held).doesNotContainKey("ttl");
    assertThat(held.get("heldByPaymentId").s()).isEqualTo(paymentId);
    assertThat(held.get("version").n()).isEqualTo("5");

    mvc.perform(
            post("/internal/v1/quotes/{id}/payment-hold", quoteId)
                .header("X-Actor-Id", "system:payment")
                .header("Idempotency-Key", "k2")
                .contentType(MediaType.APPLICATION_JSON)
                .content(hold(Ulid.mint(), offerId)))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value(ErrorCodes.PAYMENT_ALREADY_IN_PROGRESS));
    mvc.perform(
            post("/internal/v1/quotes/{id}/payment-hold", quoteId)
                .header("X-Actor-Id", "system:payment")
                .header("Idempotency-Key", "k3")
                .contentType(MediaType.APPLICATION_JSON)
                .content(hold(paymentId, Ulid.mint())))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value(ErrorCodes.INVALID_OFFER_REFERENCE));
    mvc.perform(
            post("/internal/v1/quotes/{id}/payment-hold", quoteId)
                .header("X-Actor-Id", "system:payment")
                .contentType(MediaType.APPLICATION_JSON)
                .content(hold(paymentId, offerId)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value(ErrorCodes.MISSING_IDEMPOTENCY_KEY));

    // a stale release from another payment is a no-op
    mvc.perform(
            post("/internal/v1/quotes/{id}/payment-release", quoteId)
                .header("X-Actor-Id", "system:payment")
                .header("Idempotency-Key", "r0")
                .contentType(MediaType.APPLICATION_JSON)
                .content(release(Ulid.mint())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.released").value(false))
        .andExpect(jsonPath("$.paymentInFlight").value(true));
    for (boolean expected : new boolean[] {true, false}) {
      mvc.perform(
              post("/internal/v1/quotes/{id}/payment-release", quoteId)
                  .header("X-Actor-Id", "system:payment")
                  .header("Idempotency-Key", "r1")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(release(paymentId)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.released").value(expected))
          .andExpect(jsonPath("$.state").value("SELECTED"))
          .andExpect(jsonPath("$.paymentInFlight").value(false));
    }
    assertThat(meta(quoteId)).containsKey("ttl").doesNotContainKey("heldByPaymentId");
  }

  @Test
  void lapsedQuoteIsExpiredOnTheNextTouchAndTheJourneyMayQuoteAgain() throws Exception {
    String journeyId = Ulid.mint();
    CreateOutcome created =
        quotes.create("journey:" + journeyId + ":quote:1", QuoteFixtures.newQuote(journeyId));
    QuoteOffer offer = QuoteFixtures.offer("INS01", "100.00");
    quotes.startProviderJob(created.quoteId(), "1SB", "job-x");
    quotes.recordOffers(
        created.quoteId(), List.of(offer), List.of(), Instant.now().plusMillis(1500));
    quotes.select(created.quoteId(), offer.offerId());
    Thread.sleep(1600);

    mvc.perform(
            post("/internal/v1/quotes/{id}/payment-hold", created.quoteId())
                .header("X-Actor-Id", "system:payment")
                .header("Idempotency-Key", "h")
                .contentType(MediaType.APPLICATION_JSON)
                .content(hold(Ulid.mint(), offer.offerId())))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value(ErrorCodes.QUOTE_EXPIRED));
    assertThat(meta(created.quoteId()).get("state").s()).isEqualTo("EXPIRED");
    assertThat(outbox(created.quoteId()))
        .extracting(i -> i.get("eventType").s())
        .contains("QuoteExpired");
    for (Map<String, AttributeValue> item : outbox(created.quoteId())) {
      assertThat(EventSchemas.validateEnvelope(item.get("envelope").s()))
          .as(item.get("eventType").s())
          .isEmpty();
    }

    // a lapsed QUOTED quote is expired by the next create on the same journey (marker swap)
    String journey2 = Ulid.mint();
    CreateOutcome old =
        quotes.create("journey:" + journey2 + ":quote:1", QuoteFixtures.newQuote(journey2));
    quotes.startProviderJob(old.quoteId(), "1SB", "job-y");
    quotes.recordOffers(
        old.quoteId(),
        List.of(QuoteFixtures.offer("INS01", "100.00")),
        List.of(),
        Instant.now().minusSeconds(1));
    CreateOutcome fresh =
        quotes.create("journey:" + journey2 + ":quote:2", QuoteFixtures.newQuote(journey2));
    assertThat(fresh.quoteId()).isNotEqualTo(old.quoteId());
    assertThat(quotes.get(old.quoteId()).state()).isEqualTo(QuoteState.EXPIRED);
    // a still-valid open quote keeps the marker
    assertThatThrownBy(
            () ->
                quotes.create("journey:" + journey2 + ":quote:3", QuoteFixtures.newQuote(journey2)))
        .extracting(e -> ((ServiceException) e).getErrorResponse().getCode())
        .isEqualTo(ErrorCodes.CONFLICT);
  }

  @Test
  void everyOutboxPayloadMatchesTheEventContract() {
    String quoteId = selectedQuote(Ulid.mint());
    materializer.onPaymentReconciled(Ulid.mint(), quoteId, Ulid.mint());

    List<Map<String, AttributeValue>> items = outbox(quoteId);
    assertThat(items)
        .extracting(i -> i.get("eventType").s())
        .containsExactlyInAnyOrder(
            "QuoteRequested", "QuoteCompleted", "OfferSelected", "QuoteConverted");
    for (Map<String, AttributeValue> item : items) {
      assertThat(EventSchemas.validateEnvelope(item.get("envelope").s()))
          .as(item.get("eventType").s())
          .isEmpty();
      assertThat(item.get("published").bool()).isFalse();
      assertThat(item).doesNotContainKey("ttl");
    }
  }

  @Test
  void paymentReadSetsInFlightAndRemovesTtlThenEndingRestoresIt() throws Exception {
    String quoteId = selectedQuote(Ulid.mint());

    mvc.perform(get("/internal/v1/quotes/{id}/selected", quoteId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.premiumAmount").value(12300.00))
        .andExpect(jsonPath("$.insurerCode").value("INS01"))
        .andExpect(jsonPath("$.currency").value("INR"));
    assertThat(meta(quoteId)).doesNotContainKey("ttl");
    assertThat(meta(quoteId).get("paymentInFlight").bool()).isTrue();
    long version = Long.parseLong(meta(quoteId).get("version").n());

    quotes.markPaymentInFlight(quoteId);
    assertThat(Long.parseLong(meta(quoteId).get("version").n())).isEqualTo(version);

    quotes.paymentEnded(quoteId);
    assertThat(meta(quoteId)).containsKey("ttl");
    assertThat(meta(quoteId).get("paymentInFlight").bool()).isFalse();
  }

  @Test
  void selectingTwiceIsAnIllegalTransition() {
    String quoteId = selectedQuote(Ulid.mint());
    String offerId = quotes.get(quoteId).envelope().selectedOfferId();

    assertThatThrownBy(() -> quotes.select(quoteId, offerId))
        .extracting(e -> ((ServiceException) e).getErrorResponse().getCode())
        .isEqualTo(ErrorCodes.ILLEGAL_TRANSITION);
  }

  @Test
  void materializeIsIdempotentAndFreezesTheItem() throws Exception {
    String journeyId = Ulid.mint();
    String quoteId = selectedQuote(journeyId);
    quotes.markPaymentInFlight(quoteId);
    String body =
        "{\"eventId\":\""
            + Ulid.mint()
            + "\",\"quoteId\":\""
            + quoteId
            + "\",\"paymentId\":\""
            + Ulid.mint()
            + "\",\"applicationNumber\":\"APP-1\"}";

    mvc.perform(
            post("/internal/v1/events/payment-reconciled")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.outcome").value("MATERIALIZED"));
    mvc.perform(
            post("/internal/v1/events/payment-reconciled")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.outcome").value("ALREADY_MATERIALIZED"));

    assertThat(count("SELECT COUNT(*) FROM quotation.quote WHERE quote_id = ?", quoteId))
        .isEqualTo(1);
    // D8: the zero-premium INV-QUO-03 offer is kept as INVALID
    assertThat(
            jdbc.queryForList(
                "SELECT offer_state FROM quotation.offer WHERE quote_id = ? ORDER BY offer_state",
                String.class,
                quoteId))
        .containsExactly("INVALID", "NOT_SELECTED", "SELECTED");
    assertThat(
            jdbc.queryForMap(
                "SELECT selected_offer_id, product_class, evaluation_id, materialized_event_id"
                    + " FROM quotation.quote WHERE quote_id = ?",
                quoteId))
        .containsEntry("PRODUCT_CLASS", "TERM")
        .containsEntry("SELECTED_OFFER_ID", quotes.get(quoteId).envelope().selectedOfferId());
    assertThat(
            jdbc.queryForObject(
                "SELECT state FROM quotation.quote WHERE quote_id = ?", String.class, quoteId))
        .isEqualTo("CONVERTED");
    byte[] members =
        jdbc.queryForObject(
            "SELECT members_enc FROM quotation.quote WHERE quote_id = ?", byte[].class, quoteId);
    assertThat(new String(members, StandardCharsets.ISO_8859_1))
        .doesNotContain(QuoteFixtures.MEMBER_PINCODE);
    assertThat(
            count(
                "SELECT COUNT(*) FROM quotation.idempotency_record WHERE response_ref = ?",
                quoteId))
        .isEqualTo(1);

    Map<String, AttributeValue> item = meta(quoteId);
    assertThat(item).containsKey("materializedAt").containsKey("ttl");
    assertThat(item.get("state").s()).isEqualTo("CONVERTED");
    WorkingQuote frozen = quotes.get(quoteId);
    assertThat(frozen.state()).isEqualTo(QuoteState.CONVERTED);
    assertThatThrownBy(() -> quotes.paymentEnded(quoteId))
        .extracting(e -> ((ServiceException) e).getErrorResponse().getCode())
        .isEqualTo(ErrorCodes.ILLEGAL_TRANSITION);
    // the journey may quote again
    quotes.create("journey:" + journeyId + ":quote:2", QuoteFixtures.newQuote(journeyId));
  }

  @Test
  void redeliveryRebuildsMissingStructuredRowsFromTheFrozenItem() {
    String quoteId = selectedQuote(Ulid.mint());
    materializer.onPaymentReconciled(Ulid.mint(), quoteId, Ulid.mint());
    jdbc.update("DELETE FROM quotation.offer WHERE quote_id = ?", quoteId);
    jdbc.update("DELETE FROM quotation.quote WHERE quote_id = ?", quoteId);

    MaterializeOutcome outcome =
        materializer.onPaymentReconciled(Ulid.mint(), quoteId, Ulid.mint());

    assertThat(outcome).isEqualTo(MaterializeOutcome.ALREADY_MATERIALIZED);
    assertThat(count("SELECT COUNT(*) FROM quotation.quote WHERE quote_id = ?", quoteId))
        .isEqualTo(1);
    assertThat(count("SELECT COUNT(*) FROM quotation.offer WHERE quote_id = ?", quoteId))
        .isEqualTo(3);
  }

  @Test
  void unselectedOrMissingQuoteIsNotMaterialized() {
    CreateOutcome created = quotes.create(Ulid.mint(), QuoteFixtures.newQuote());

    assertThatThrownBy(
            () -> materializer.onPaymentReconciled(Ulid.mint(), created.quoteId(), Ulid.mint()))
        .extracting(e -> ((ServiceException) e).getErrorResponse().getCode())
        .isEqualTo(ErrorCodes.ILLEGAL_TRANSITION);
    assertThatThrownBy(
            () -> materializer.onPaymentReconciled(Ulid.mint(), Ulid.mint(), Ulid.mint()))
        .extracting(e -> ((ServiceException) e).getErrorResponse().getCode())
        .isEqualTo(ErrorCodes.RESOURCE_NOT_FOUND);
  }

  @Test
  void invalidEventBodyIsRejected() throws Exception {
    mvc.perform(
            post("/internal/v1/events/payment-reconciled")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"eventId\":\"e\"}"))
        .andExpect(status().is4xxClientError());
  }
}
