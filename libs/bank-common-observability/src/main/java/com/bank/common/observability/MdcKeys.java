package com.bank.common.observability;

/**
 * Standard MDC (Mapped Diagnostic Context) key names for structured logging.
 *
 * <p>All services must use these exact key names so that the bank's log
 * aggregation platform (e.g. Elasticsearch, Splunk) can correlate log lines
 * across services using a single field name.
 *
 * <p>Usage example (Logback):
 * <pre>{@code
 * MdcContext.put(MdcKeys.JOB_ID, jobId);
 * try {
 *     doWork();
 * } finally {
 *     MdcContext.clear(MdcKeys.JOB_ID);
 * }
 * }</pre>
 */
public final class MdcKeys {

    private MdcKeys() {}

    /** Async job identifier — present throughout job processing lifecycle. */
    public static final String JOB_ID     = "jobId";

    /** Line of Business — e.g. TERM, HEALTH, MOTOR. */
    public static final String LOB        = "lob";

    /** Authenticated actor identifier (matches JWT {@code actor_id} claim). */
    public static final String ACTOR_ID   = "actorId";

    /** OpenTelemetry / distributed tracing trace ID. */
    public static final String TRACE_ID   = "traceId";

    /** OpenTelemetry span ID. */
    public static final String SPAN_ID    = "spanId";

    /** Caller-supplied journey identifier from bank frontend. */
    public static final String JOURNEY_ID = "journeyId";

    /** HTTP request correlation ID (from {@code X-Correlation-Id} header). */
    public static final String CORRELATION_ID = "correlationId";
}
