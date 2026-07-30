package com.bank.insurance.onesb.adapter.onesb.polling;

import com.bank.insurance.onesb.domain.model.JobStatus;
import com.bank.insurance.onesb.domain.model.QuoteOffer;
import com.bank.insurance.onesb.domain.port.outbound.JobStorePort;
import com.bank.insurance.onesb.domain.port.outbound.OneSbPollPort;
import com.bank.insurance.onesb.domain.port.outbound.OneSbPollPort.PollResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Polls 1SB until complete or max attempts. Runs on {@code pollingExecutor} — never the
 * Tomcat request thread. Prefer {@link #pollUntilDone} in unit tests; production uses
 * {@link #schedulePoll}.
 * <p>
 * On max attempts: job → {@link JobStatus#TIMEOUT} with reason {@code POLL_TIMEOUT}
 * (caller-facing errors for timeout are retryable at the API layer — Phase 3).
 */
@Component
public class AsyncJobPoller {

    public static final String POLL_TIMEOUT_REASON = "POLL_TIMEOUT";

    private static final Logger log = LoggerFactory.getLogger(AsyncJobPoller.class);

    private final OneSbPollPort pollPort;
    private final JobStorePort jobStore;
    private final PollingProperties properties;
    private final Executor pollingExecutor;
    private final Sleeper sleeper;

    @Autowired
    public AsyncJobPoller(OneSbPollPort pollPort,
                          JobStorePort jobStore,
                          PollingProperties properties,
                          @Qualifier("pollingExecutor") Executor pollingExecutor) {
        this(pollPort, jobStore, properties, pollingExecutor, Thread::sleep);
    }

    AsyncJobPoller(OneSbPollPort pollPort,
                   JobStorePort jobStore,
                   PollingProperties properties,
                   Executor pollingExecutor,
                   Sleeper sleeper) {
        this.pollPort = pollPort;
        this.jobStore = jobStore;
        this.properties = properties;
        this.pollingExecutor = pollingExecutor;
        this.sleeper = sleeper;
    }

    /**
     * Schedules polling asynchronously and returns immediately.
     */
    public void schedulePoll(String jobId, String pollPath) {
        pollingExecutor.execute(() -> {
            try {
                pollUntilDone(jobId, pollPath);
            } catch (Exception ex) {
                log.warn("Async poll failed for jobId={}: {}", jobId, ex.toString());
                try {
                    jobStore.failJob(jobId, "POLL_ERROR");
                } catch (Exception failEx) {
                    log.warn("Failed to mark job {} as failed: {}", jobId, failEx.toString());
                }
            }
        });
    }

    /**
     * Synchronous poll loop for unit tests and the async worker.
     * Does not block the HTTP request thread when invoked only via {@link #schedulePoll}.
     */
    public PollOutcome pollUntilDone(String jobId, String pollPath) {
        AtomicInteger attempts = new AtomicInteger(0);
        for (int attempt = 1; attempt <= properties.maxAttempts(); attempt++) {
            long delay = properties.delayBeforeAttemptMs(attempt);
            if (delay > 0) {
                sleepQuietly(delay);
            }

            long started = System.currentTimeMillis();
            PollResult result = pollPort.poll(pollPath);
            int durationMs = (int) Math.min(Integer.MAX_VALUE, System.currentTimeMillis() - started);
            attempts.incrementAndGet();

            jobStore.recordPollAttempt(
                    jobId,
                    attempt,
                    result.httpStatus(),
                    result.complete(),
                    durationMs,
                    result.errorMessage()
            );

            if (result.complete()) {
                jobStore.completeJob(jobId, List.<QuoteOffer>of());
                return new PollOutcome(JobStatus.COMPLETED, attempts.get());
            }
        }

        jobStore.failJob(jobId, POLL_TIMEOUT_REASON);
        return new PollOutcome(JobStatus.TIMEOUT, attempts.get());
    }

    private void sleepQuietly(long delayMs) {
        try {
            sleeper.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Poll sleep interrupted", e);
        }
    }

    @FunctionalInterface
    interface Sleeper {
        void sleep(long millis) throws InterruptedException;
    }

    public record PollOutcome(JobStatus terminalStatus, int pollAttempts) {}
}
