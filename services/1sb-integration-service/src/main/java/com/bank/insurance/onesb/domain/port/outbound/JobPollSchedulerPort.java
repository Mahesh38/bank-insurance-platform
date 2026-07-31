package com.bank.insurance.onesb.domain.port.outbound;

/**
 * Schedules async polling for integration jobs. Implemented by
 * {@code adapter.onesb.polling.AsyncJobPoller}.
 */
public interface JobPollSchedulerPort {

    /** Generic path-based poll (no offer normalisation). */
    void schedulePoll(String jobId, String pollPath);

    /**
     * Quote-specific poll using {@link OneSbQuotePort} for completion detection and offers.
     * On max attempts: {@code failJob(POLL_TIMEOUT)}.
     */
    void scheduleQuotePoll(String jobId, String lob, String externalReqId);
}
