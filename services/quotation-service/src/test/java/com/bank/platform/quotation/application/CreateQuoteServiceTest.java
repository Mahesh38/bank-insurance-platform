package com.bank.platform.quotation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.platform.quotation.adapter.consent.StubConsentCheckpointAdapter;
import com.bank.platform.quotation.adapter.hub.HttpHubQuoteAdapter;
import com.bank.platform.quotation.application.CreateQuoteService.AcceptedQuote;
import com.bank.platform.quotation.domain.ConsentCheckpointPort;
import com.bank.platform.quotation.domain.CreateOutcome;
import com.bank.platform.quotation.domain.HubQuotePort;
import com.bank.platform.quotation.domain.HubQuotePort.Accepted;
import com.bank.platform.quotation.domain.NewQuote;
import com.bank.platform.quotation.domain.QuoteState;
import com.bank.platform.quotation.domain.SuitabilityGatePort;
import com.bank.platform.quotation.domain.Ulid;
import com.bank.platform.quotation.domain.WorkingQuote;
import com.bank.platform.quotation.support.QuoteFixtures;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** FUNC-033 — createQuote orchestrates gates → working state → Hub submit once. */
@Tag("unit")
@Tag("FUNC-033")
class CreateQuoteServiceTest {

  @Test
  @DisplayName("SAVINGS create calls Hub once with bank SSOT premium + PT/PPT preferences")
  void create_savings_hubCalledOnceWithMappedBody() {
    SuitabilityGatePort suitability = mock(SuitabilityGatePort.class);
    ConsentCheckpointPort consent = mock(ConsentCheckpointPort.class);
    QuoteWorkingStateService quotes = mock(QuoteWorkingStateService.class);
    HubQuotePort hub = mock(HubQuotePort.class);

    String quoteId = Ulid.mint();
    String providerRequestId = Ulid.mint();
    NewQuote draft = QuoteFixtures.savingsQuote(Ulid.mint());
    when(consent.requireBeforeMultiQuote(draft.customerId()))
        .thenReturn(StubConsentCheckpointAdapter.STUB_CONSENT_ID);

    NewQuote persisted =
        new NewQuote(
            draft.leadId(),
            draft.journeyId(),
            draft.customerId(),
            draft.suitabilityId(),
            StubConsentCheckpointAdapter.STUB_CONSENT_ID,
            draft.lob(),
            draft.productClass(),
            draft.mode(),
            draft.category(),
            draft.requestedCover(),
            draft.members(),
            CreateQuoteService.CONFIG_VERSION_REF,
            draft.premiumAmount(),
            draft.preferences());
    WorkingQuote requested =
        WorkingQuote.request(quoteId, persisted, Instant.parse("2026-10-28T08:00:00Z"));
    WorkingQuote inProgress =
        WorkingQuote.request(quoteId, persisted, Instant.parse("2026-10-28T08:00:00Z"));
    inProgress.startProviderJob("ONE_SB", providerRequestId, Instant.parse("2026-10-28T08:00:01Z"));

    when(quotes.create(anyString(), any(NewQuote.class)))
        .thenReturn(new CreateOutcome(quoteId, false));
    when(quotes.get(quoteId)).thenReturn(requested);
    when(hub.submitQuote(eq(quoteId), any(NewQuote.class), eq("RM-1001"), eq("corr-1")))
        .thenReturn(new Accepted(providerRequestId, "ONE_SB", "IN_PROGRESS"));
    when(quotes.startProviderJob(quoteId, "ONE_SB", providerRequestId)).thenReturn(inProgress);

    CreateQuoteService service = new CreateQuoteService(suitability, consent, quotes, hub);
    AcceptedQuote accepted = service.create("journey:1:quote:1", draft, "RM-1001", "corr-1");

    assertThat(accepted.quoteId()).isEqualTo(quoteId);
    assertThat(accepted.state()).isEqualTo(QuoteState.IN_PROGRESS);

    verify(suitability)
        .requireUsable(
            draft.customerId(), draft.lob(), draft.productClass(), draft.suitabilityId());
    verify(consent).requireBeforeMultiQuote(draft.customerId());

    ArgumentCaptor<NewQuote> submitted = ArgumentCaptor.forClass(NewQuote.class);
    verify(hub, times(1))
        .submitQuote(eq(quoteId), submitted.capture(), eq("RM-1001"), eq("corr-1"));
    NewQuote body = submitted.getValue();
    assertThat(body.productClass()).isEqualTo("SAVINGS");
    assertThat(body.category()).isEqualTo("PREMIUM");
    assertThat(body.premiumAmount()).isEqualByComparingTo("25000");
    assertThat(body.preferences().premiumFrequency()).isEqualTo("HALF_YEARLY");
    assertThat(body.preferences().policyTerm()).isEqualTo(15);
    assertThat(body.preferences().premiumPayingTerm()).isEqualTo(10);
    assertThat(body.consentId()).isEqualTo(StubConsentCheckpointAdapter.STUB_CONSENT_ID);

    Map<String, Object> hubWire = HttpHubQuoteAdapter.toHubBody(quoteId, body);
    @SuppressWarnings("unchecked")
    Map<String, Object> hubBody = (Map<String, Object>) hubWire.get("body");
    assertThat(hubBody.get("premiumAmount")).isEqualTo(body.premiumAmount());
    @SuppressWarnings("unchecked")
    Map<String, Object> prefs = (Map<String, Object>) hubBody.get("preferences");
    assertThat(prefs)
        .containsEntry("premiumFrequency", "HALF_YEARLY")
        .containsEntry("policyTerm", 15)
        .containsEntry("premiumPayingTerm", 10);
  }

  @Test
  @DisplayName("idempotent replay of IN_PROGRESS quote does not resubmit to Hub")
  void create_replayInProgress_skipsHub() {
    SuitabilityGatePort suitability = mock(SuitabilityGatePort.class);
    ConsentCheckpointPort consent = mock(ConsentCheckpointPort.class);
    QuoteWorkingStateService quotes = mock(QuoteWorkingStateService.class);
    HubQuotePort hub = mock(HubQuotePort.class);

    String quoteId = Ulid.mint();
    NewQuote draft = QuoteFixtures.savingsQuote(Ulid.mint());
    when(consent.requireBeforeMultiQuote(any()))
        .thenReturn(StubConsentCheckpointAdapter.STUB_CONSENT_ID);
    NewQuote persisted =
        new NewQuote(
            draft.leadId(),
            draft.journeyId(),
            draft.customerId(),
            draft.suitabilityId(),
            StubConsentCheckpointAdapter.STUB_CONSENT_ID,
            draft.lob(),
            draft.productClass(),
            draft.mode(),
            draft.category(),
            draft.requestedCover(),
            draft.members(),
            CreateQuoteService.CONFIG_VERSION_REF,
            draft.premiumAmount(),
            draft.preferences());
    WorkingQuote inProgress =
        WorkingQuote.request(quoteId, persisted, Instant.parse("2026-10-28T08:00:00Z"));
    inProgress.startProviderJob("ONE_SB", Ulid.mint(), Instant.parse("2026-10-28T08:00:01Z"));

    when(quotes.create(anyString(), any(NewQuote.class)))
        .thenReturn(new CreateOutcome(quoteId, true));
    when(quotes.get(quoteId)).thenReturn(inProgress);

    CreateQuoteService service = new CreateQuoteService(suitability, consent, quotes, hub);
    AcceptedQuote accepted = service.create("journey:1:quote:1", draft, "RM-1001", "corr-1");

    assertThat(accepted.state()).isEqualTo(QuoteState.IN_PROGRESS);
    verify(hub, times(0)).submitQuote(any(), any(), any(), any());
  }
}
