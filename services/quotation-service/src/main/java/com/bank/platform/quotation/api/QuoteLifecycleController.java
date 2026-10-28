package com.bank.platform.quotation.api;

import com.bank.common.error.ErrorCodes;
import com.bank.common.error.ServiceErrors;
import com.bank.platform.quotation.application.CreateQuoteService;
import com.bank.platform.quotation.application.CreateQuoteService.AcceptedQuote;
import com.bank.platform.quotation.application.GetQuoteService;
import com.bank.platform.quotation.domain.InsurerError;
import com.bank.platform.quotation.domain.NewQuote;
import com.bank.platform.quotation.domain.OfferState;
import com.bank.platform.quotation.domain.QuoteMember;
import com.bank.platform.quotation.domain.QuoteOffer;
import com.bank.platform.quotation.domain.QuoteState;
import com.bank.platform.quotation.domain.WorkingQuote;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * FUNC-033 — {@code createQuote} / {@code getQuote} on {@code /internal/v1/quotes}. Sibling of
 * {@link QuoteInternalController} (payment-hold); methods do not clash.
 */
@RestController
@RequestMapping("/internal/v1/quotes")
public class QuoteLifecycleController {

  static final String ULID = "^[0-9A-HJKMNP-TV-Z]{26}$";
  private static final String COMPONENT = "QuoteLifecycleController";

  private final CreateQuoteService createQuotes;
  private final GetQuoteService getQuotes;
  private final ActorGuard actors;
  private final ServiceErrors errors;

  public QuoteLifecycleController(
      CreateQuoteService createQuotes,
      GetQuoteService getQuotes,
      ActorGuard actors,
      ServiceErrors errors) {
    this.createQuotes = createQuotes;
    this.getQuotes = getQuotes;
    this.actors = actors;
    this.errors = errors;
  }

  @PostMapping
  public ResponseEntity<QuoteAcceptedResponse> createQuote(
      HttpServletRequest httpRequest,
      @RequestHeader(value = "X-Actor-Id", required = false) String actorId,
      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
      @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId,
      @Valid @RequestBody CreateQuoteRequest request) {
    if (idempotencyKey == null || idempotencyKey.isBlank()) {
      throw errors
          .error(ErrorCodes.MISSING_IDEMPOTENCY_KEY)
          .component(COMPONENT)
          .operation("createQuote")
          .build();
    }
    String actor = actors.requireActorId(httpRequest, actorId, "createQuote");
    AcceptedQuote accepted =
        createQuotes.create(idempotencyKey, request.toNewQuote(), actor, correlationId);
    return ResponseEntity.status(HttpStatus.ACCEPTED)
        .body(new QuoteAcceptedResponse(accepted.quoteId(), accepted.state()));
  }

  @GetMapping("/{quoteId}")
  public QuoteResponse getQuote(
      HttpServletRequest httpRequest,
      @PathVariable String quoteId,
      @RequestHeader(value = "X-Actor-Id", required = false) String actorId) {
    actors.requireActorId(httpRequest, actorId, "getQuote");
    if (!quoteId.matches(ULID)) {
      throw errors
          .error(ErrorCodes.VALIDATION_ERROR)
          .component(COMPONENT)
          .operation("getQuote")
          .reason("quoteId must be a ULID")
          .build();
    }
    return QuoteResponse.from(getQuotes.get(quoteId));
  }

  public record CreateQuoteRequest(
      @NotBlank @Pattern(regexp = ULID) String leadId,
      @NotBlank @Pattern(regexp = ULID) String journeyId,
      @NotBlank @Pattern(regexp = ULID) String customerId,
      @NotBlank @Pattern(regexp = ULID) String suitabilityId,
      @NotBlank String lob,
      @NotBlank String productClass,
      @NotBlank String mode,
      @NotBlank String category,
      @NotNull BigDecimal requestedCover,
      @NotEmpty List<@Valid MemberRequest> members,
      BigDecimal premiumAmount,
      PreferencesRequest preferences) {

    NewQuote toNewQuote() {
      List<QuoteMember> mapped = new ArrayList<>();
      for (MemberRequest m : members) {
        mapped.add(
            new QuoteMember(
                m.role(),
                m.sequence(),
                m.dob(),
                m.gender(),
                m.tobacco() != null && m.tobacco(),
                m.annualIncome(),
                m.pincode()));
      }
      NewQuote.Preferences prefs =
          preferences == null
              ? null
              : new NewQuote.Preferences(
                  preferences.premiumFrequency(),
                  preferences.policyTerm(),
                  preferences.premiumPayingTerm());
      return new NewQuote(
          leadId,
          journeyId,
          customerId,
          suitabilityId,
          null,
          lob,
          productClass,
          mode,
          category,
          requestedCover,
          mapped,
          null,
          premiumAmount,
          prefs);
    }
  }

  public record MemberRequest(
      @NotBlank String role,
      int sequence,
      @NotNull LocalDate dob,
      @NotBlank String gender,
      Boolean tobacco,
      BigDecimal annualIncome,
      String pincode) {}

  public record PreferencesRequest(
      String premiumFrequency, Integer policyTerm, Integer premiumPayingTerm) {}

  public record QuoteAcceptedResponse(String quoteId, QuoteState state) {}

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record QuoteResponse(
      String quoteId,
      String journeyId,
      String lob,
      String productClass,
      QuoteState state,
      Instant validUntil,
      String selectedOfferId,
      Boolean paymentInFlight,
      List<OfferResponse> offers,
      List<PartialErrorResponse> partialErrors,
      String configVersionRef,
      String recordSource) {

    static QuoteResponse from(WorkingQuote quote) {
      WorkingQuote.Snapshot snap = quote.snapshot();
      List<OfferResponse> offers = new ArrayList<>();
      for (QuoteOffer offer : quote.envelope().offers()) {
        offers.add(OfferResponse.from(offer));
      }
      List<PartialErrorResponse> errors = new ArrayList<>();
      for (InsurerError error : quote.envelope().partialErrors()) {
        errors.add(new PartialErrorResponse(error.insurerCode(), error.code()));
      }
      return new QuoteResponse(
          quote.quoteId(),
          quote.journeyId(),
          snap.lob(),
          quote.productClass(),
          quote.state(),
          quote.validUntil(),
          quote.envelope().selectedOfferId(),
          quote.paymentInFlight(),
          offers,
          errors,
          snap.configVersionRef(),
          "WORKING");
    }
  }

  @JsonInclude(JsonInclude.Include.NON_NULL)
  public record OfferResponse(
      String offerId,
      String insurerCode,
      String productCode,
      String productName,
      BigDecimal premiumAmount,
      BigDecimal taxAmount,
      String premiumFrequency,
      BigDecimal sumAssured,
      Integer coverTerm,
      Integer premiumPayingTerm,
      boolean outOfBound,
      OfferState offerState,
      String errorSummary) {

    static OfferResponse from(QuoteOffer offer) {
      return new OfferResponse(
          offer.offerId(),
          offer.insurerCode(),
          offer.productCode(),
          offer.productName(),
          offer.premiumAmount(),
          offer.taxAmount(),
          offer.frequency(),
          offer.sumAssured(),
          offer.coverTerm(),
          offer.ppt(),
          offer.outOfBound(),
          offer.offerState(),
          offer.errorSummary());
    }
  }

  public record PartialErrorResponse(String insurerCode, String code) {}
}
