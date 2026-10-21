package com.bank.platform.quotation.api;

import com.bank.platform.quotation.application.QuoteWorkingStateService;
import com.bank.platform.quotation.domain.SelectedPremium;
import java.math.BigDecimal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Payment's read of the selected offer (quotation-service API reference §3.4, schemas/20 §3.3). */
@RestController
@RequestMapping("/internal/v1/quotes")
public class QuoteInternalController {

  /** Control attributes only — Payment never sees the envelope ({@code INV-PAY-03}). */
  public record SelectedOfferResponse(
      String quoteId,
      String offerId,
      BigDecimal premiumAmount,
      BigDecimal taxAmount,
      BigDecimal sumAssured,
      String insurerCode,
      String productCode,
      String currency) {

    static SelectedOfferResponse of(SelectedPremium p) {
      return new SelectedOfferResponse(
          p.quoteId(),
          p.offerId(),
          p.premiumAmount(),
          p.taxAmount(),
          p.sumAssured(),
          p.insurerCode(),
          p.productCode(),
          "INR");
    }
  }

  private final QuoteWorkingStateService quotes;

  public QuoteInternalController(QuoteWorkingStateService quotes) {
    this.quotes = quotes;
  }

  /** Also sets {@code paymentInFlight} and removes the item's TTL while money moves. */
  @GetMapping("/{quoteId}/selected")
  public SelectedOfferResponse selected(@PathVariable String quoteId) {
    return SelectedOfferResponse.of(quotes.markPaymentInFlight(quoteId));
  }
}
