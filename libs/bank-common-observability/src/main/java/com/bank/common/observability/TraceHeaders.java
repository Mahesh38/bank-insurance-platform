package com.bank.common.observability;

/**
 * Standard HTTP header names used for distributed tracing and correlation.
 *
 * <p>Inbound requests from bank clients carry these headers; the service must
 * propagate them to outbound 1SB calls and include them in all log MDC entries.
 */
public final class TraceHeaders {

    private TraceHeaders() {}

    /** W3C Trace Context propagation header. */
    public static final String TRACE_PARENT    = "traceparent";

    /** W3C Trace Context state header. */
    public static final String TRACE_STATE     = "tracestate";

    /** Bank-internal correlation identifier. */
    public static final String CORRELATION_ID  = "X-Correlation-Id";

    /** Bank-internal request identifier (per-hop, not cross-service). */
    public static final String REQUEST_ID      = "X-Request-Id";
}
