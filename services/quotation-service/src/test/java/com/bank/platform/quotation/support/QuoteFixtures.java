package com.bank.platform.quotation.support;

import com.bank.platform.quotation.domain.NewQuote;
import com.bank.platform.quotation.domain.OfferState;
import com.bank.platform.quotation.domain.QuoteMember;
import com.bank.platform.quotation.domain.QuoteOffer;
import com.bank.platform.quotation.domain.Ulid;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class QuoteFixtures {

  public static final String MEMBER_PINCODE = "400001";

  private QuoteFixtures() {}

  public static NewQuote newQuote() {
    return newQuote(Ulid.mint());
  }

  public static NewQuote newQuote(String journeyId) {
    return new NewQuote(
        Ulid.mint(),
        journeyId,
        Ulid.mint(),
        Ulid.mint(),
        Ulid.mint(),
        "LIFE",
        "TERM",
        "MULTI",
        "SUM_ASSURED",
        new BigDecimal("10000000"),
        List.of(
            new QuoteMember(
                "LIFE_ASSURED",
                1,
                LocalDate.of(1990, 4, 12),
                "MALE",
                false,
                new BigDecimal("1500000"),
                MEMBER_PINCODE)),
        "cfg-2026-10-21");
  }

  public static QuoteOffer offer(String insurer, String premium) {
    return new QuoteOffer(
        Ulid.mint(),
        insurer,
        "TERM-PLUS",
        "Term Plus",
        new BigDecimal(premium),
        new BigDecimal("2214.00"),
        new BigDecimal("10000000"),
        "YEARLY",
        30,
        30,
        false,
        OfferState.QUOTED,
        null);
  }
}
