package com.bank.insurance.onesb.adapter.onesb.polling;

import com.bank.insurance.onesb.TestErrors;

import com.bank.common.audit.AuditActions;
import com.bank.common.audit.AuditEvent;
import com.bank.common.audit.AuditEventPublisher;
import com.bank.common.secrets.SecretProvider;
import com.bank.insurance.onesb.adapter.onesb.client.OneSbHttpClient;
import com.bank.insurance.onesb.adapter.onesb.quote.OneSbQuoteAdapter;
import com.bank.common.domain.JobStatus;
import com.bank.common.domain.QuoteOffer;
import com.bank.insurance.onesb.domain.port.outbound.JobStorePort;
import com.bank.insurance.onesb.domain.port.outbound.OneSbPollPort;
import com.bank.insurance.onesb.domain.port.outbound.OneSbQuotePort;
import com.bank.insurance.onesb.lob.LobQuoteHandlerRegistry;
import com.bank.insurance.onesb.lob.life.term.TermQuoteHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("FUNC-002")
@ExtendWith(MockitoExtension.class)
class AsyncJobPollerQuoteTest {

    private static final String POLL_PATH = "/insurance/lifeterm/v1/quote/poll/REQ-1";

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @Mock OneSbPollPort pollPort;
    @Mock OneSbQuotePort quotePort;
    @Mock JobStorePort jobStore;
    @Mock AuditEventPublisher auditEventPublisher;

    @BeforeEach
    void resetWireMock() {
        wireMock.resetAll();
    }

    @Test
    void pollQuoteUntilDone_pendingThenComplete_withOffers() {
        AtomicInteger calls = new AtomicInteger();
        when(quotePort.isPollComplete(eq("job-1"), eq("REQ-1"), eq("TERM"))).thenAnswer(inv -> {
            return calls.incrementAndGet() >= 2;
        });
        when(quotePort.pollQuoteResult("job-1", "REQ-1", "TERM")).thenReturn(List.of(
                new QuoteOffer("o1", "INS", "Insurer", "P", "Prod",
                        new BigDecimal("100"), "Y", new BigDecimal("5000000"),
                        false, "AVAILABLE", null)
        ));

        AsyncJobPoller poller = new AsyncJobPoller(
                pollPort, quotePort, jobStore,
                new PollingProperties(1L, 2.0d, 10L, 5),
                Runnable::run,
                ms -> {},
                auditEventPublisher
        );

        AsyncJobPoller.PollOutcome outcome = poller.pollQuoteUntilDone("job-1", "TERM", "REQ-1");

        assertThat(outcome.terminalStatus()).isEqualTo(JobStatus.COMPLETED);
        assertThat(outcome.pollAttempts()).isGreaterThanOrEqualTo(2);
        verify(jobStore).completeJob(eq("job-1"), any());

        ArgumentCaptor<AuditEvent> auditCaptor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditEventPublisher).publish(auditCaptor.capture());
        assertThat(auditCaptor.getValue().getAction()).isEqualTo(AuditActions.QUOTE_COMPLETED);
        assertThat(auditCaptor.getValue().getResourceId()).isEqualTo("job-1");
        assertThat(auditCaptor.getValue().getMetadata()).containsEntry("offerCount", "1");
        assertThat(auditCaptor.getValue().getMetadata()).containsEntry("partialErrorCount", "0");
    }

    @Test
    void pollQuoteUntilDone_mixedOffers_completesPartial_andAudits() {
        when(quotePort.isPollComplete("job-partial", "REQ-p", "TERM")).thenReturn(true);
        List<QuoteOffer> mixed = List.of(
                new QuoteOffer("a", "I1", null, "P", "Ok", BigDecimal.ONE, "Y",
                        BigDecimal.TEN, false, "AVAILABLE", null),
                new QuoteOffer("b", "I2", null, "P", "Bad", null, null, null,
                        false, "ERROR", "declined")
        );
        when(quotePort.pollQuoteResult("job-partial", "REQ-p", "TERM")).thenReturn(mixed);

        AsyncJobPoller poller = new AsyncJobPoller(
                pollPort, quotePort, jobStore,
                new PollingProperties(1L, 2.0d, 10L, 5),
                Runnable::run,
                ms -> {},
                auditEventPublisher
        );

        AsyncJobPoller.PollOutcome outcome = poller.pollQuoteUntilDone("job-partial", "TERM", "REQ-p");

        assertThat(outcome.terminalStatus()).isEqualTo(JobStatus.PARTIAL);
        verify(jobStore).completeJob("job-partial", mixed);

        ArgumentCaptor<AuditEvent> auditCaptor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditEventPublisher).publish(auditCaptor.capture());
        assertThat(auditCaptor.getValue().getAction()).isEqualTo(AuditActions.QUOTE_COMPLETED);
        assertThat(auditCaptor.getValue().getMetadata()).containsEntry("status", "PARTIAL");
        assertThat(auditCaptor.getValue().getMetadata()).containsEntry("partialErrorCount", "1");
    }

    @Test
    void pollQuoteUntilDone_timeout_failsWithPollTimeout() {
        when(quotePort.isPollComplete(anyString(), anyString(), anyString())).thenReturn(false);

        AsyncJobPoller poller = new AsyncJobPoller(
                pollPort, quotePort, jobStore,
                new PollingProperties(1L, 2.0d, 10L, 3),
                Runnable::run,
                ms -> {},
                auditEventPublisher
        );

        AsyncJobPoller.PollOutcome outcome = poller.pollQuoteUntilDone("job-to", "TERM", "REQ-x");

        assertThat(outcome.terminalStatus()).isEqualTo(JobStatus.TIMEOUT);
        verify(jobStore).failJob("job-to", AsyncJobPoller.POLL_TIMEOUT_REASON);
        verify(jobStore, times(3)).recordPollAttempt(
                eq("job-to"), anyInt(), anyInt(), eq(false), anyInt(), any());
        verify(auditEventPublisher, times(0)).publish(any());
    }

    @Test
    void resolveCompleteStatus_partialWhenMixedOffers() {
        List<QuoteOffer> offers = List.of(
                new QuoteOffer("a", "I1", null, "P", "Ok", BigDecimal.ONE, "Y",
                        BigDecimal.TEN, false, "AVAILABLE", null),
                new QuoteOffer("b", "I2", null, "P", "Bad", null, null, null,
                        false, "ERROR", "declined")
        );
        assertThat(AsyncJobPoller.resolveCompleteStatus(offers)).isEqualTo(JobStatus.PARTIAL);
    }

    @Test
    void pollQuoteUntilDone_wireMockPendingThenComplete_viaRealAdapter() {
        wireMock.stubFor(get(urlEqualTo(POLL_PATH))
                .inScenario("quote-poll")
                .whenScenarioStateIs("Started")
                .willSetStateTo("ready")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"data\":{\"isPollComplete\":false}}")));
        wireMock.stubFor(get(urlEqualTo(POLL_PATH))
                .inScenario("quote-poll")
                .whenScenarioStateIs("ready")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "data": {
                                    "isPollComplete": true,
                                    "quote": [{
                                      "offerId": "off-wm",
                                      "insurerCode": "HDFC",
                                      "productCode": "T1",
                                      "productName": "Term",
                                      "premiumAmount": 5000,
                                      "sumAssured": 5000000
                                    }]
                                  }
                                }
                                """)));

        SecretProvider secrets = mock(SecretProvider.class);
        TermQuoteHandler handler = new TermQuoteHandler(secrets);
        LobQuoteHandlerRegistry registry = new LobQuoteHandlerRegistry(List.of(handler), TestErrors.ONESB);
        RestClient restClient = RestClient.builder()
                .baseUrl(wireMock.baseUrl())
                .requestFactory(http1Factory())
                .build();
        OneSbQuoteAdapter realAdapter = new OneSbQuoteAdapter(
                new OneSbHttpClient(restClient), registry, new ObjectMapper());

        AsyncJobPoller poller = new AsyncJobPoller(
                pollPort, realAdapter, jobStore,
                new PollingProperties(1L, 2.0d, 10L, 5),
                Runnable::run,
                ms -> {},
                auditEventPublisher
        );

        AsyncJobPoller.PollOutcome outcome = poller.pollQuoteUntilDone("job-wm", "TERM", "REQ-1");

        assertThat(outcome.terminalStatus()).isEqualTo(JobStatus.COMPLETED);
        assertThat(outcome.pollAttempts()).isGreaterThanOrEqualTo(2);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<QuoteOffer>> offersCaptor = ArgumentCaptor.forClass(List.class);
        verify(jobStore).completeJob(eq("job-wm"), offersCaptor.capture());
        assertThat(offersCaptor.getValue()).hasSize(1);
        assertThat(offersCaptor.getValue().getFirst().insurerCode()).isEqualTo("HDFC");
        verify(auditEventPublisher).publish(any(AuditEvent.class));
    }

    private static JdkClientHttpRequestFactory http1Factory() {
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(10));
        return factory;
    }
}
