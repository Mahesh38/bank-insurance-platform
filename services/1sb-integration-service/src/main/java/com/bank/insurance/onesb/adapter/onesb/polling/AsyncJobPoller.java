package com.bank.insurance.onesb.adapter.onesb.polling;

import com.bank.common.audit.AuditActions;
import com.bank.common.audit.AuditEvent;
import com.bank.common.audit.AuditEventPublisher;
import com.bank.common.audit.AuditOutcomes;
import com.bank.insurance.onesb.domain.model.JobStatus;
import com.bank.insurance.onesb.domain.model.QuoteOffer;
import com.bank.insurance.onesb.domain.port.outbound.JobPollSchedulerPort;
import com.bank.insurance.onesb.domain.port.outbound.JobStorePort;
import com.bank.insurance.onesb.domain.port.outbound.OneSbPollPort;
import com.bank.insurance.onesb.domain.port.outbound.OneSbPollPort.PollResult;
import com.bank.insurance.onesb.domain.port.outbound.OneSbQuotePort;
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
 * Tomcat request thread. Prefer {@link #pollUntilDone} / {@link #pollQuoteUntilDone} in unit
 * tests; production uses {@link #schedulePoll} / {@link #scheduleQuotePoll}.
 * <p>
 * On max attempts: job → {@link JobStatus#TIMEOUT} with reason {@code POLL_TIMEOUT}
 * (caller-facing errors for timeout are retryable at the API layer — Phase 3).
 */
@Component
public class AsyncJobPoller implements JobPollSchedulerPort {

    public static final String POLL_TIMEOUT_REASON = "POLL_TIMEOUT";

    private static final Logger log = LoggerFactory.getLogger(AsyncJobPoller.class);

    private final OneSbPollPort pollPort;
    private final OneSbQuotePort quotePort;
    private final JobStorePort jobStore;
    private final PollingProperties properties;
    private final Executor pollingExecutor;
    private final Sleeper sleeper;
    private final AuditEventPublisher auditEventPublisher;

    @Autowired
    public AsyncJobPoller(OneSbPollPort pollPort,
                          OneSbQuotePort quotePort,
                          JobStorePort jobStore,
                          PollingProperties properties,
                          @Qualifier("pollingExecutor") Executor pollingExecutor,
                          AuditEventPublisher auditEventPublisher) {
        this(pollPort, quotePort, jobStore, properties, pollingExecutor, Thread::sleep, auditEventPublisher);
    }

    AsyncJobPoller(OneSbPollPort pollPort,
                   OneSbQuotePort quotePort,
                   JobStorePort jobStore,
                   PollingProperties properties,
                   Executor pollingExecutor,
                   Sleeper sleeper,
                   AuditEventPublisher auditEventPublisher) {
        this.pollPort = pollPort;
        this.quotePort = quotePort;
        this.jobStore = jobStore;
        this.properties = properties;
        this.pollingExecutor = pollingExecutor;
        this.sleeper = sleeper;
        this.auditEventPublisher = auditEventPublisher;
    }

    /**
     * Package-private constructor for legacy unit tests that only exercise path-based polling.
     */
    AsyncJobPoller(OneSbPollPort pollPort,
                   JobStorePort jobStore,
                   PollingProperties properties,
                   Executor pollingExecutor,
                   Sleeper sleeper) {
        this(pollPort, unusedQuotePort(), jobStore, properties, pollingExecutor, sleeper, unusedAudit());
    }

    /** Test helper when audit verification is not needed. */
    AsyncJobPoller(OneSbPollPort pollPort,
                   OneSbQuotePort quotePort,
                   JobStorePort jobStore,
                   PollingProperties properties,
                   Executor pollingExecutor,
                   Sleeper sleeper) {
        this(pollPort, quotePort, jobStore, properties, pollingExecutor, sleeper, unusedAudit());
    }

    private static AuditEventPublisher unusedAudit() {
        return event -> { };
    }

    private static OneSbQuotePort unusedQuotePort() {
        return new OneSbQuotePort() {
            @Override
            public String submitQuote(String jobId, String path, Object payload) {
                throw new UnsupportedOperationException();
            }

            @Override
            public List<QuoteOffer> pollQuoteResult(String jobId, String externalReqId, String lob) {
                return List.of();
            }

            @Override
            public boolean isPollComplete(String jobId, String externalReqId, String lob) {
                return false;
            }
        };
    }

    /**
     * Schedules path-based polling asynchronously and returns immediately.
     */
    @Override
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
     * Schedules quote polling via {@link OneSbQuotePort}; completes job with offers when done.
     */
    @Override
    public void scheduleQuotePoll(String jobId, String lob, String externalReqId) {
        pollingExecutor.execute(() -> {
            try {
                pollQuoteUntilDone(jobId, lob, externalReqId);
            } catch (Exception ex) {
                log.warn("Async quote poll failed for jobId={}: {}", jobId, ex.toString());
                try {
                    jobStore.failJob(jobId, "POLL_ERROR");
                } catch (Exception failEx) {
                    log.warn("Failed to mark job {} as failed: {}", jobId, failEx.toString());
                }
            }
        });
    }

    /**
     * Synchronous path-based poll loop for unit tests and the async worker.
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
                List<QuoteOffer> offers = result.offers() != null ? result.offers() : List.of();
                jobStore.completeJob(jobId, offers);
                return new PollOutcome(JobStatus.COMPLETED, attempts.get());
            }
        }

        jobStore.failJob(jobId, POLL_TIMEOUT_REASON);
        return new PollOutcome(JobStatus.TIMEOUT, attempts.get());
    }

    /**
     * Synchronous quote poll loop using {@link OneSbQuotePort}.
     */
    public PollOutcome pollQuoteUntilDone(String jobId, String lob, String externalReqId) {
        AtomicInteger attempts = new AtomicInteger(0);
        for (int attempt = 1; attempt <= properties.maxAttempts(); attempt++) {
            long delay = properties.delayBeforeAttemptMs(attempt);
            if (delay > 0) {
                sleepQuietly(delay);
            }

            long started = System.currentTimeMillis();
            boolean complete;
            String errorMessage = null;
            int httpStatus = 200;
            List<QuoteOffer> offers = List.of();
            try {
                complete = quotePort.isPollComplete(jobId, externalReqId, lob);
                if (complete) {
                    offers = quotePort.pollQuoteResult(jobId, externalReqId, lob);
                }
            } catch (Exception ex) {
                complete = false;
                httpStatus = 0;
                errorMessage = ex.getMessage();
            }
            int durationMs = (int) Math.min(Integer.MAX_VALUE, System.currentTimeMillis() - started);
            attempts.incrementAndGet();

            jobStore.recordPollAttempt(
                    jobId,
                    attempt,
                    httpStatus,
                    complete,
                    durationMs,
                    errorMessage
            );

            if (complete) {
                List<QuoteOffer> finalOffers = offers != null ? offers : List.of();
                jobStore.completeJob(jobId, finalOffers);
                JobStatus status = resolveCompleteStatus(finalOffers);
                publishQuoteCompleted(jobId, lob, status, finalOffers);
                return new PollOutcome(status, attempts.get());
            }
        }

        jobStore.failJob(jobId, POLL_TIMEOUT_REASON);
        return new PollOutcome(JobStatus.TIMEOUT, attempts.get());
    }

    static JobStatus resolveCompleteStatus(List<QuoteOffer> offers) {
        if (offers == null || offers.isEmpty()) {
            return JobStatus.COMPLETED;
        }
        boolean hasError = offers.stream()
                .anyMatch(o -> o.errorSummary() != null && !o.errorSummary().isBlank());
        boolean hasSuccess = offers.stream()
                .anyMatch(o -> o.errorSummary() == null || o.errorSummary().isBlank());
        return hasError && hasSuccess ? JobStatus.PARTIAL : JobStatus.COMPLETED;
    }

    private void publishQuoteCompleted(String jobId, String lob, JobStatus status, List<QuoteOffer> offers) {
        if (status != JobStatus.COMPLETED && status != JobStatus.PARTIAL) {
            return;
        }
        try {
            long offerCount = offers != null ? offers.size() : 0;
            long partialErrorCount = offers == null ? 0 : offers.stream()
                    .filter(o -> o.errorSummary() != null && !o.errorSummary().isBlank())
                    .count();
            AuditEvent event = AuditEvent.builder()
                    .actorId("system")
                    .actorType("SYSTEM")
                    .action(AuditActions.QUOTE_COMPLETED)
                    .resourceType("QUOTE_JOB")
                    .resourceId(jobId)
                    .outcome(AuditOutcomes.SUCCESS)
                    .lob(lob)
                    .metadata("jobId", jobId)
                    .metadata("offerCount", String.valueOf(offerCount))
                    .metadata("partialErrorCount", String.valueOf(partialErrorCount))
                    .metadata("status", status.name())
                    .build();
            auditEventPublisher.publish(event);
        } catch (Exception e) {
            log.warn("Failed to publish QUOTE_COMPLETED for jobId={}: {}", jobId, e.toString());
        }
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
