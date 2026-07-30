package com.bank.insurance.onesb.adapter.onesb.polling;

import com.bank.insurance.onesb.domain.model.JobStatus;
import com.bank.insurance.onesb.domain.model.QuoteOffer;
import com.bank.insurance.onesb.domain.port.outbound.JobStorePort;
import com.bank.insurance.onesb.domain.port.outbound.OneSbPollPort;
import com.bank.insurance.onesb.domain.port.outbound.OneSbQuotePort;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("FUNC-002")
@ExtendWith(MockitoExtension.class)
class AsyncJobPollerQuoteTest {

    @Mock OneSbPollPort pollPort;
    @Mock OneSbQuotePort quotePort;
    @Mock JobStorePort jobStore;

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
                ms -> {}
        );

        AsyncJobPoller.PollOutcome outcome = poller.pollQuoteUntilDone("job-1", "TERM", "REQ-1");

        assertThat(outcome.terminalStatus()).isEqualTo(JobStatus.COMPLETED);
        assertThat(outcome.pollAttempts()).isGreaterThanOrEqualTo(2);
        verify(jobStore).completeJob(eq("job-1"), any());
    }

    @Test
    void pollQuoteUntilDone_timeout_failsWithPollTimeout() {
        when(quotePort.isPollComplete(anyString(), anyString(), anyString())).thenReturn(false);

        AsyncJobPoller poller = new AsyncJobPoller(
                pollPort, quotePort, jobStore,
                new PollingProperties(1L, 2.0d, 10L, 3),
                Runnable::run,
                ms -> {}
        );

        AsyncJobPoller.PollOutcome outcome = poller.pollQuoteUntilDone("job-to", "TERM", "REQ-x");

        assertThat(outcome.terminalStatus()).isEqualTo(JobStatus.TIMEOUT);
        verify(jobStore).failJob("job-to", AsyncJobPoller.POLL_TIMEOUT_REASON);
        verify(jobStore, times(3)).recordPollAttempt(
                eq("job-to"), anyInt(), anyInt(), eq(false), anyInt(), any());
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
}
