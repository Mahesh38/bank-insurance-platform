package com.bank.insurance.onesb.adapter.onesb.polling;

import com.bank.insurance.onesb.adapter.onesb.client.OneSbHttpClient;
import com.bank.insurance.onesb.domain.model.JobStatus;
import com.bank.insurance.onesb.domain.port.outbound.JobStorePort;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.web.client.RestClient;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class AsyncJobPollerTest {

    private static final String POLL_PATH = "/insurance/lifeterm/v1/quote/poll/REQ-1";

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    private JobStorePort jobStore;

    @BeforeEach
    void setUp() {
        jobStore = mock(JobStorePort.class);
        wireMock.resetAll();
    }

    private AsyncJobPoller newPoller(PollingProperties props, Executor executor) {
        RestClient restClient = RestClient.builder().baseUrl(wireMock.baseUrl()).build();
        OneSbHttpClient httpClient = new OneSbHttpClient(restClient);
        OneSbHttpClientPollAdapter pollPort = new OneSbHttpClientPollAdapter(httpClient, new ObjectMapper());
        return new AsyncJobPoller(
                pollPort,
                jobStore,
                props,
                executor,
                ms -> { /* no-op sleep for fast tests */ }
        );
    }

    @Test
    void pendingThenComplete_marksCompletedWithAtLeastTwoAttempts() {
        wireMock.stubFor(get(urlEqualTo(POLL_PATH))
                .inScenario("poll")
                .whenScenarioStateIs("Started")
                .willSetStateTo("second")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"data\":{\"isPollComplete\":false}}")));
        wireMock.stubFor(get(urlEqualTo(POLL_PATH))
                .inScenario("poll")
                .whenScenarioStateIs("second")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"data\":{\"isPollComplete\":true}}")));

        AsyncJobPoller poller = newPoller(new PollingProperties(1L, 2.0d, 10L, 5), Runnable::run);
        AsyncJobPoller.PollOutcome outcome = poller.pollUntilDone("job-1", POLL_PATH);

        assertThat(outcome.terminalStatus()).isEqualTo(JobStatus.COMPLETED);
        assertThat(outcome.pollAttempts()).isGreaterThanOrEqualTo(2);
        verify(jobStore, times(2)).recordPollAttempt(
                eq("job-1"), anyInt(), eq(200), anyBoolean(), anyInt(), any());
        verify(jobStore).completeJob(eq("job-1"), any(), any());
    }

    @Test
    void alwaysIncompleteUntilMax_marksTimeout() {
        wireMock.stubFor(get(urlEqualTo(POLL_PATH))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"data\":{\"isPollComplete\":false}}")));

        AsyncJobPoller poller = newPoller(new PollingProperties(1L, 2.0d, 10L, 3), Runnable::run);
        AsyncJobPoller.PollOutcome outcome = poller.pollUntilDone("job-timeout", POLL_PATH);

        assertThat(outcome.terminalStatus()).isEqualTo(JobStatus.TIMEOUT);
        assertThat(outcome.pollAttempts()).isEqualTo(3);
        verify(jobStore).failJob("job-timeout", AsyncJobPoller.POLL_TIMEOUT_REASON);
        verify(jobStore, times(3)).recordPollAttempt(
                eq("job-timeout"), anyInt(), eq(200), eq(false), anyInt(), any());
    }

    @Test
    void schedulePoll_returnsImmediatelyAndCompletesAsync() throws Exception {
        wireMock.stubFor(get(urlEqualTo(POLL_PATH))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"data\":{\"isPollComplete\":true}}")));

        CountDownLatch done = new CountDownLatch(1);
        Executor async = r -> new Thread(() -> {
            try {
                r.run();
            } finally {
                done.countDown();
            }
        }).start();

        AsyncJobPoller poller = newPoller(new PollingProperties(1L, 2.0d, 10L, 5), async);

        long start = System.nanoTime();
        poller.schedulePoll("job-async", POLL_PATH);
        long elapsedMs = (System.nanoTime() - start) / 1_000_000L;

        assertThat(elapsedMs).isLessThan(500L);
        assertThat(done.await(5, TimeUnit.SECONDS)).isTrue();
        verify(jobStore).completeJob(eq("job-async"), any(), any());
    }
}
