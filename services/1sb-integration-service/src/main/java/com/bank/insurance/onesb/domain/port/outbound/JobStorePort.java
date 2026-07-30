package com.bank.insurance.onesb.domain.port.outbound;

import com.bank.insurance.onesb.domain.model.JobStatus;
import com.bank.insurance.onesb.domain.model.QuoteJob;
import com.bank.insurance.onesb.domain.model.QuoteOffer;

import java.util.List;
import java.util.Optional;

/**
 * Port for persisting and retrieving integration jobs.
 * Implemented by {@code adapter.persistence.HttpJobStoreAdapter} (HTTP → persistence service).
 */
public interface JobStorePort {

    String createJob(String lob, String jobType, String journeyId, String idempotencyKey, String actorId);

    void updateJobStatus(String jobId, JobStatus status);

    void updateJobPolling(String jobId, String externalReqId);

    void completeJob(String jobId, List<QuoteOffer> offers);

    void failJob(String jobId, String failureReason);

    Optional<QuoteJob> findQuoteJob(String jobId);
}
