package com.bank.common.observability;

/**
 * Standardised metric name prefixes for the bank's observability platform.
 * All services use these prefixes to ensure consistent Prometheus/Grafana dashboards.
 */
public final class BankMetricNames {

    public static final String ONESB_JOB_CREATED = "bank.onesb.job.created";
    public static final String ONESB_JOB_COMPLETED = "bank.onesb.job.completed";
    public static final String ONESB_POLL_ATTEMPTS = "bank.onesb.poll.attempts";
    public static final String ONESB_POLL_DURATION = "bank.onesb.poll.duration";
    public static final String ONESB_HTTP_REQUEST_DURATION = "bank.onesb.http.request.duration";
    public static final String ONESB_ERROR_COUNT = "bank.onesb.error.count";
    public static final String ONESB_IDEMPOTENCY_HIT = "bank.onesb.idempotency.hit";

    private BankMetricNames() {}
}
