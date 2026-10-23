package com.bank.platform.quotation.api;

import com.bank.common.error.ErrorCodes;
import com.bank.common.error.ServiceErrors;
import com.bank.platform.quotation.application.QuoteWorkingStateService;
import com.bank.platform.quotation.application.QuoteWorkingStateService.ReleaseResult;
import com.bank.platform.quotation.domain.QuoteState;
import com.bank.platform.quotation.domain.SelectedPremium;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Payment's hold on the selected offer (contract {@code quotation-service/openapi.yaml}, operations
 * {@code holdForPayment}, {@code releasePaymentHold}, deprecated {@code getSelectedLegacy}; LLD
 * §4.4). Control attributes only — Payment never sees the envelope ({@code INV-PAY-03}).
 */
@RestController
@RequestMapping("/internal/v1/quotes")
public class QuoteInternalController {

  static final String ULID = "^[0-9A-HJKMNP-TV-Z]{26}$";
  private static final String COMPONENT = "QuoteInternalController";

  /** {@code SelectedPremium}; {@code premiumFrequency} and {@code validUntil} only on the hold. */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record SelectedOfferResponse(
      String quoteId,
      String offerId,
      BigDecimal premiumAmount,
      BigDecimal taxAmount,
      BigDecimal sumAssured,
      String insurerCode,
      String productCode,
      String premiumFrequency,
      String currency,
      Instant validUntil) {

    static SelectedOfferResponse hold(SelectedPremium p) {
      return new SelectedOfferResponse(
          p.quoteId(),
          p.offerId(),
          p.premiumAmount(),
          p.taxAmount(),
          p.sumAssured(),
          p.insurerCode(),
          p.productCode(),
          p.premiumFrequency(),
          "INR",
          p.validUntil());
    }

    static SelectedOfferResponse legacy(SelectedPremium p) {
      return new SelectedOfferResponse(
          p.quoteId(),
          p.offerId(),
          p.premiumAmount(),
          p.taxAmount(),
          p.sumAssured(),
          p.insurerCode(),
          p.productCode(),
          null,
          "INR",
          null);
    }
  }

  public record PaymentHoldRequest(
      @NotBlank @Pattern(regexp = ULID) String paymentId,
      @NotBlank @Pattern(regexp = ULID) String proposalId,
      @NotBlank @Pattern(regexp = ULID) String offerId) {}

  public record PaymentReleaseRequest(
      @NotBlank @Pattern(regexp = ULID) String paymentId,
      @NotNull @Pattern(regexp = "^(DECLINED|EXPIRED|REJECTED|REFUNDED)$") String paymentState) {}

  public record ReleaseResponse(
      String quoteId, QuoteState state, boolean paymentInFlight, boolean released) {}

  private final QuoteWorkingStateService quotes;
  private final ServiceErrors errors;

  public QuoteInternalController(QuoteWorkingStateService quotes, ServiceErrors errors) {
    this.quotes = quotes;
    this.errors = errors;
  }

  @PostMapping("/{quoteId}/payment-hold")
  public SelectedOfferResponse holdForPayment(
      @PathVariable String quoteId,
      @RequestHeader(value = "X-Actor-Id", required = false) String actorId,
      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
      @Valid @RequestBody PaymentHoldRequest request) {
    requireHeaders("holdForPayment", quoteId, actorId, idempotencyKey);
    return SelectedOfferResponse.hold(
        quotes.holdForPayment(quoteId, request.paymentId(), request.offerId(), actorId));
  }

  @PostMapping("/{quoteId}/payment-release")
  public ReleaseResponse releasePaymentHold(
      @PathVariable String quoteId,
      @RequestHeader(value = "X-Actor-Id", required = false) String actorId,
      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
      @Valid @RequestBody PaymentReleaseRequest request) {
    requireHeaders("releasePaymentHold", quoteId, actorId, idempotencyKey);
    ReleaseResult result = quotes.releasePaymentHold(quoteId, request.paymentId());
    return new ReleaseResponse(
        quoteId, result.quote().state(), result.quote().paymentInFlight(), result.released());
  }

  /**
   * DEPRECATED ({@code getSelectedLegacy}): a GET with a side effect — sets {@code paymentInFlight}
   * and removes the item's TTL. Kept until payment-service calls {@code payment-hold} (ADR-027 §4).
   */
  @Deprecated
  @GetMapping("/{quoteId}/selected")
  public SelectedOfferResponse selected(@PathVariable String quoteId) {
    return SelectedOfferResponse.legacy(quotes.markPaymentInFlight(quoteId));
  }

  private void requireHeaders(
      String operation, String quoteId, String actorId, String idempotencyKey) {
    if (idempotencyKey == null || idempotencyKey.isBlank()) {
      throw errors
          .error(ErrorCodes.MISSING_IDEMPOTENCY_KEY)
          .component(COMPONENT)
          .operation(operation)
          .build();
    }
    if (actorId == null || actorId.isBlank() || actorId.length() > 64) {
      throw invalid(operation, "X-Actor-Id is required (<= 64 chars)");
    }
    if (!quoteId.matches(ULID)) {
      throw invalid(operation, "quoteId must be a ULID");
    }
  }

  private RuntimeException invalid(String operation, String reason) {
    return errors
        .error(ErrorCodes.VALIDATION_ERROR)
        .component(COMPONENT)
        .operation(operation)
        .reason(reason)
        .build();
  }
}
