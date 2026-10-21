package com.bank.platform.quotation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bank.common.error.ErrorCodes;
import com.bank.platform.quotation.support.QuoteFixtures;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class WorkingQuoteTest {

  private static final Instant NOW = Instant.parse("2026-10-21T10:00:00Z");
  private static final Instant VALID_UNTIL = NOW.plus(Duration.ofDays(7));

  private final QuoteOffer cheap = QuoteFixtures.offer("INS01", "12300.00");
  private final QuoteOffer dear = QuoteFixtures.offer("INS02", "15000.00");

  private WorkingQuote quoted() {
    WorkingQuote quote = WorkingQuote.request("Q1", QuoteFixtures.newQuote(), NOW);
    quote.startProviderJob("1SB", "job-1", NOW);
    quote.recordOffers(List.of(cheap, dear), List.of(), VALID_UNTIL, NOW);
    return quote;
  }

  @Test
  void createIsRequestedAtVersionZero() {
    WorkingQuote quote = WorkingQuote.request("Q1", QuoteFixtures.newQuote(), NOW);

    assertThat(quote.state()).isEqualTo(QuoteState.REQUESTED);
    assertThat(quote.version()).isZero();
    assertThat(quote.envelope().members()).hasSize(1);
  }

  @Test
  void invalidOffersAreKeptButNeverSelectable() {
    WorkingQuote quote = WorkingQuote.request("Q1", QuoteFixtures.newQuote(), NOW);
    QuoteOffer zero = QuoteFixtures.offer("INS03", "0");

    quote.recordOffers(
        List.of(cheap, zero), List.of(new InsurerError("INS04", "TIMEOUT")), VALID_UNTIL, NOW);

    assertThat(quote.state()).isEqualTo(QuoteState.PARTIALLY_QUOTED);
    assertThat(quote.envelope().offers())
        .extracting(QuoteOffer::offerState)
        .containsExactly(OfferState.QUOTED, OfferState.INVALID);
    assertThatThrownBy(() -> quote.select(zero.offerId(), NOW))
        .isInstanceOf(QuoteRuleException.class)
        .extracting(e -> ((QuoteRuleException) e).code())
        .isEqualTo(ErrorCodes.INVALID_OFFER_REFERENCE);
  }

  @Test
  void noValidOfferFails() {
    WorkingQuote quote = WorkingQuote.request("Q1", QuoteFixtures.newQuote(), NOW);

    quote.recordOffers(List.of(QuoteFixtures.offer("INS01", "-1")), List.of(), VALID_UNTIL, NOW);

    assertThat(quote.state()).isEqualTo(QuoteState.FAILED);
    assertThat(quote.state().endsWithoutSale()).isTrue();
  }

  @Test
  void selectMarksSiblingsAndExposesPremium() {
    WorkingQuote quote = quoted();

    quote.select(cheap.offerId(), NOW.plusSeconds(60));

    assertThat(quote.state()).isEqualTo(QuoteState.SELECTED);
    assertThat(quote.envelope().selectedOfferId()).isEqualTo(cheap.offerId());
    assertThat(quote.envelope().offers())
        .extracting(QuoteOffer::offerState)
        .containsExactly(OfferState.SELECTED, OfferState.NOT_SELECTED);
    assertThat(quote.selectedPremium().orElseThrow().premiumAmount())
        .isEqualByComparingTo("12300.00");
  }

  @Test
  void selectAfterValidityIsExpired() {
    WorkingQuote quote = quoted();

    assertThatThrownBy(() -> quote.select(cheap.offerId(), VALID_UNTIL))
        .extracting(e -> ((QuoteRuleException) e).code())
        .isEqualTo(ErrorCodes.QUOTE_EXPIRED);
  }

  @Test
  void selectUnknownOfferOrFromWrongStateIsRefused() {
    WorkingQuote quote = quoted();
    assertThatThrownBy(() -> quote.select("nope", NOW))
        .extracting(e -> ((QuoteRuleException) e).code())
        .isEqualTo(ErrorCodes.INVALID_OFFER_REFERENCE);

    WorkingQuote requested = WorkingQuote.request("Q2", QuoteFixtures.newQuote(), NOW);
    assertThatThrownBy(() -> requested.select(cheap.offerId(), NOW))
        .extracting(e -> ((QuoteRuleException) e).code())
        .isEqualTo(ErrorCodes.ILLEGAL_TRANSITION);
    assertThatThrownBy(() -> requested.markPaymentInFlight(NOW))
        .extracting(e -> ((QuoteRuleException) e).code())
        .isEqualTo(ErrorCodes.ILLEGAL_TRANSITION);
  }

  @Test
  void paymentInFlightRemovesExpiryAndEndingRestoresIt() {
    WorkingRetention retention = new WorkingRetention(Duration.ofDays(90), Duration.ofDays(30));
    WorkingQuote quote = quoted();
    quote.select(cheap.offerId(), NOW);

    quote.markPaymentInFlight(NOW);
    assertThat(retention.expiryOf(true, null, NOW)).isEmpty();

    quote.paymentEnded(NOW.plusSeconds(5));
    assertThat(quote.paymentInFlight()).isFalse();
    assertThat(retention.expiryOf(false, null, quote.updatedAt()))
        .contains(NOW.plusSeconds(5).plus(Duration.ofDays(90)));
  }

  @Test
  void materializedQuoteIsFrozen() {
    WorkingQuote quote = quoted();
    quote.select(cheap.offerId(), NOW);

    quote.materialize(NOW);

    assertThat(quote.state()).isEqualTo(QuoteState.CONVERTED);
    assertThat(quote.isMaterialized()).isTrue();
    assertThatThrownBy(() -> quote.paymentEnded(NOW))
        .extracting(e -> ((QuoteRuleException) e).code())
        .isEqualTo(ErrorCodes.ILLEGAL_TRANSITION);
    assertThatThrownBy(() -> quote.materialize(NOW)).isInstanceOf(QuoteRuleException.class);
  }

  @Test
  void onlySelectedQuoteMaterializes() {
    WorkingQuote quote = quoted();

    assertThatThrownBy(() -> quote.materialize(NOW)).isInstanceOf(QuoteRuleException.class);
    assertThatThrownBy(() -> quote.recordOffers(List.of(), List.of(), VALID_UNTIL, NOW))
        .isInstanceOf(QuoteRuleException.class);
  }
}
