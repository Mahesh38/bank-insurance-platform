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
        "{\"eventId\":\"evt-"
            + quoteId
            + "\",\"quoteId\":\""
            + quoteId
            + "\",\"paymentId\":\"P1\"}";

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
    // the zero-premium INV-QUO-03 offer has no structured row (ck_offer_positive)
    assertThat(
            jdbc.queryForList(
                "SELECT offer_state FROM quotation.offer WHERE quote_id = ? ORDER BY offer_state",
                String.class,
                quoteId))
        .containsExactly("NOT_SELECTED", "SELECTED");
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
    materializer.onPaymentReconciled("evt-a-" + quoteId, quoteId);
    jdbc.update("DELETE FROM quotation.offer WHERE quote_id = ?", quoteId);
    jdbc.update("DELETE FROM quotation.quote WHERE quote_id = ?", quoteId);

    MaterializeOutcome outcome = materializer.onPaymentReconciled("evt-b-" + quoteId, quoteId);

    assertThat(outcome).isEqualTo(MaterializeOutcome.ALREADY_MATERIALIZED);
    assertThat(count("SELECT COUNT(*) FROM quotation.quote WHERE quote_id = ?", quoteId))
        .isEqualTo(1);
    assertThat(count("SELECT COUNT(*) FROM quotation.offer WHERE quote_id = ?", quoteId))
        .isEqualTo(2);
  }

  @Test
  void unselectedOrMissingQuoteIsNotMaterialized() {
    CreateOutcome created = quotes.create(Ulid.mint(), QuoteFixtures.newQuote());

    assertThatThrownBy(() -> materializer.onPaymentReconciled("e1", created.quoteId()))
        .extracting(e -> ((ServiceException) e).getErrorResponse().getCode())
        .isEqualTo(ErrorCodes.ILLEGAL_TRANSITION);
    assertThatThrownBy(() -> materializer.onPaymentReconciled("e2", Ulid.mint()))
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
