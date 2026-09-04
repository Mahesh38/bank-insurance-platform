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

    // --- Error identity (ERR-004) ---------------------------------------------------
    // These are what turn a log platform into an incident tool: a support engineer pastes the
    // incidentId an RM read off the screen and gets every line of that failure, across services.

    /** Names one failure across every hop. Generated at first failure, never regenerated. */
    public static final String INCIDENT_ID = "incidentId";

    /** The stable error code, matching {@code ErrorCodes} and catalogue 04. */
    public static final String ERROR_CODE = "errorCode";

    /** The error's class — validation, authorization, compliance gate, upstream, and so on. */
    public static final String ERROR_CATEGORY = "errorCategory";

    /** The service that emitted the response. */
    public static final String SERVICE = "service";

    /**
     * The service the failure actually began in — equal to {@link #SERVICE} for a local failure.
     *
     * <p>This is the field that answers "is it us or them" without reading the message.
     */
    public static final String ORIGIN_SERVICE = "originService";

    /** The layer of the request ladder the refusal was produced at (L1..L7). */
    public static final String LAYER = "layer";
}
