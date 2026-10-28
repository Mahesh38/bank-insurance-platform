package com.bank.platform.quotation.application;

import com.bank.platform.quotation.domain.ConsentCheckpointPort;
import com.bank.platform.quotation.domain.CreateOutcome;
import com.bank.platform.quotation.domain.HubQuotePort;
import com.bank.platform.quotation.domain.HubQuotePort.Accepted;
import com.bank.platform.quotation.domain.NewQuote;
import com.bank.platform.quotation.domain.QuoteState;
import com.bank.platform.quotation.domain.SuitabilityGatePort;
import com.bank.platform.quotation.domain.WorkingQuote;
import org.springframework.stereotype.Service;

/**
 * FUNC-033 — createQuote: C1 suitability + BEFORE_MULTI_QUOTE consent → working state → Hub submit
 * → {@code IN_PROGRESS}. Answers {@code 202 {quoteId, state}}.
 */
@Service
public class CreateQuoteService {

  static final String CONFIG_VERSION_REF = "SUITABILITY.GATE@runtime+CONSENT.POLICY@runtime";

  private final SuitabilityGatePort suitability;
  private final ConsentCheckpointPort consent;
  private final QuoteWorkingStateService quotes;
  private final HubQuotePort hub;

  public CreateQuoteService(
      SuitabilityGatePort suitability,
      ConsentCheckpointPort consent,
      QuoteWorkingStateService quotes,
      HubQuotePort hub) {
    this.suitability = suitability;
    this.consent = consent;
    this.quotes = quotes;
    this.hub = hub;
  }

  public AcceptedQuote create(
      String idempotencyKey, NewQuote draft, String actorId, String correlationId) {
    suitability.requireUsable(
        draft.customerId(), draft.lob(), draft.productClass(), draft.suitabilityId());
    String consentId = consent.requireBeforeMultiQuote(draft.customerId());
    NewQuote request =
        new NewQuote(
            draft.leadId(),
            draft.journeyId(),
            draft.customerId(),
            draft.suitabilityId(),
            consentId,
            draft.lob(),
            draft.productClass(),
            draft.mode(),
            draft.category(),
            draft.requestedCover(),
            draft.members(),
            draft.configVersionRef() == null || draft.configVersionRef().isBlank()
                ? CONFIG_VERSION_REF
                : draft.configVersionRef(),
            draft.premiumAmount(),
            draft.preferences());
    CreateOutcome outcome = quotes.create(idempotencyKey, request);
    WorkingQuote quote = quotes.get(outcome.quoteId());
    if (quote.state() == QuoteState.REQUESTED) {
      Accepted accepted = hub.submitQuote(outcome.quoteId(), request, actorId, correlationId);
      quote =
          quotes.startProviderJob(
              outcome.quoteId(), accepted.provider(), accepted.providerRequestId());
    }
    return new AcceptedQuote(quote.quoteId(), quote.state());
  }

  public record AcceptedQuote(String quoteId, QuoteState state) {}
}
