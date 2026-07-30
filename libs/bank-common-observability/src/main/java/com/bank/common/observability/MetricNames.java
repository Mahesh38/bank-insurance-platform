package com.bank.common.observability;

/**
 * Standardised Micrometer metric names for the 1SB insurance integration service.
 *
 * <p>These names are registered on the bank's Prometheus/Grafana dashboards.
 * Do not rename — dashboard panels are keyed on these strings.
 *
 * <p>All metric names use the {@code bank.onesb} prefix.
 *
 * <p>Typical tag keys used with these metrics: {@code lob}, {@code jobType},
 * {@code outcome}, {@code operation}, {@code statusCode}, {@code errorCode}, {@code source}.
 */
public final class MetricNames {

    private MetricNames() {}

    // --- Job lifecycle ---

    /** Counter: incremented when a new integration job is created. Tags: lob, jobType. */
    public static final String JOB_CREATED       = "bank.onesb.job.created";

    /** Counter: incremented when a job reaches a terminal state. Tags: lob, jobType, finalStatus. */
    public static final String JOB_COMPLETED     = "bank.onesb.job.completed";

    // --- Polling ---

    /** Histogram: number of poll attempts per job. Tags: lob, jobType. */
    public static final String POLL_ATTEMPTS     = "bank.onesb.poll.attempts";

    /** Timer: total time from job creation to terminal state. Tags: lob, jobType, outcome. */
    public static final String POLL_DURATION     = "bank.onesb.poll.duration";

    // --- HTTP outbound ---

    /** Timer: duration of outbound HTTP calls to 1SB. Tags: lob, operation, statusCode. */
    public static final String HTTP_REQUEST_DURATION = "bank.onesb.http.request.duration";

    // --- Errors ---

    /** Counter: error events by code. Tags: lob, errorCode, source. */
    public static final String ERROR_COUNT       = "bank.onesb.error.count";

    // --- Auth ---

    /** Counter: upstream auth failures (1SB returned 401/403). Tags: operation. */
    public static final String UPSTREAM_AUTH_FAILURE = "bank.onesb.upstream.auth.failure";

    // --- Idempotency ---

    /** Counter: idempotency cache hits (duplicate requests served from cache). */
    public static final String IDEMPOTENCY_HIT   = "bank.onesb.idempotency.hit";
}
