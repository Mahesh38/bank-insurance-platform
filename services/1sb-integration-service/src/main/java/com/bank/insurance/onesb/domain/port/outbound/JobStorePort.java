package com.bank.insurance.onesb.domain.port.outbound;

import com.bank.common.domain.JobStatus;
import com.bank.common.domain.QuoteJob;
import com.bank.common.domain.QuoteOffer;

import java.util.List;
import java.util.Optional;

/**
 * Port for persisting and retrieving integration jobs.
 * Implemented by {@code adapter.persistence.HttpJobStoreAdapter} (HTTP → persistence service).
 * <p>
 * Status transitions (best-effort; persistence does not enforce a state machine):
 * {@code PENDING → RUNNING → COMPLETED|PARTIAL|FAILED|TIMEOUT}.
 * Invalid / unexpected transitions are not rejected by the HTTP adapter — callers should
 * follow the happy path; overbuilt FSM deferred.
 */
public interface JobStorePort {

    String createJob(String lob, String jobType, String journeyId, String idempotencyKey, String actorId);

    void updateJobStatus(String jobId, JobStatus status);

    /** Sets status {@link JobStatus#RUNNING} and records external 1SB {@code reqId}. */
    void updateJobPolling(String jobId, String externalReqId);

    void completeJob(String jobId, List<QuoteOffer> offers);

    /**
     * Completes the job and optionally persists insurer {@code applicationNumber}
     * (proposal jobs). Blank / null applicationNumber leaves the column unchanged.
     */
    void completeJob(String jobId, List<QuoteOffer> offers, String applicationNumber);

    /**
     * Fails the job. When {@code failureReason} is {@code POLL_TIMEOUT}, status is
     * {@link JobStatus#TIMEOUT}; otherwise {@link JobStatus#FAILED}.
     */
    void failJob(String jobId, String failureReason);

    /** Records a single poll attempt for audit / SLA (persistence {@code job_poll_attempt}). */
    void recordPollAttempt(String jobId, int attemptNumber, int httpStatus,
                           boolean complete, int durationMs, String errorMessage);

    Optional<QuoteJob> findQuoteJob(String jobId);
}
