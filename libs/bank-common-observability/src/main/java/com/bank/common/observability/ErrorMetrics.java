package com.bank.common.observability;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;

/**
 * The one platform-wide error series — work item {@code ERR-004}.
 *
 * <p>One counter, consistently tagged, is what a dashboard is built from. Without it the only way
 * to answer "which errors are growing" is to grep logs, and the only way to answer "is it us or
 * them" is to read them.
 *
 * <pre>{@code
 * bank.error.count { service, code, category, layer, originService, retryable, httpStatus }
 * }</pre>
 *
 * <p>Answerable from this one series, with no new metric per question: top-k codes over time,
 * error rate by service, <em>which service is actually at fault</em> (by {@code originService}),
 * compliance-refusal rate ({@code category=COMPLIANCE_GATE}), platform-versus-caller fault, and
 * how much of the traffic is retryable.
 *
 * <h2>The cardinality guard</h2>
 * Every tag value is a bounded token — an enum name, a registered service id, a status code. A
 * metric tag that can take an unbounded set of values is not a dashboard, it is an outage: one
 * time series per identifier will exhaust a Prometheus instance far faster than the traffic that
 * produced it. {@link #safeTag} enforces that at the boundary rather than trusting call sites,
 * because the call site that gets it wrong is the one nobody reviews.
 */
public final class ErrorMetrics {

    /** Counter: every failure the platform produces. */
    public static final String ERROR_COUNT = "bank.error.count";

    /** Substituted for a null or unusable tag value, so a series never silently disappears. */
    static final String UNKNOWN = "unknown";

    /** Longest accepted tag value. Anything longer is a message or an id, not a category. */
    static final int MAX_TAG_LENGTH = 48;

    /** Most digits a legitimate tag value carries. Identifiers carry far more. */
    static final int MAX_DIGITS = 2;

    private final MeterRegistry registry;

    public ErrorMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    /**
     * Records one failure.
     *
     * @param service       the service that emitted the response
     * @param code          the stable error code
     * @param category      the error class
     * @param layer         the ladder rung, {@code L1}..{@code L7}
     * @param originService where the failure began — equal to {@code service} when local
     * @param retryable     whether a machine may repeat the request unchanged
     * @param httpStatus    the status returned
     */
    public void record(String service, String code, String category, String layer,
                       String originService, boolean retryable, int httpStatus) {
        registry.counter(ERROR_COUNT, Tags.of(
            "service", safeTag(service),
            "code", safeTag(code),
            "category", safeTag(category),
            "layer", safeTag(layer),
            "originService", safeTag(originService),
            "retryable", Boolean.toString(retryable),
            "httpStatus", Integer.toString(httpStatus)
        )).increment();
    }

    /**
     * Reduces a value to a bounded tag, or {@link #UNKNOWN}.
     *
     * <p>Three rejections, in order of how they go wrong in practice:
     *
     * <ol>
     *   <li><strong>Absent or overlong</strong> — a null tag, or a value long enough to be prose.</li>
     *   <li><strong>Punctuation or whitespace</strong> — a message ({@code "Quote job not found: j-1"})
     *       or a path ({@code "/v1/quotes/abc"}) that reached a tag by mistake.</li>
     *   <li><strong>Digit-heavy</strong> — the one that actually causes outages. A job id or a UUID
     *       is otherwise indistinguishable in shape from a service name: {@code job-8f2c1e44-0b7a}
     *       and {@code journey-orchestration} are both lowercase words joined by hyphens. Counting
     *       digits separates them, because every legitimate tag value here is an enum name, a
     *       service id or a layer — none of which carries more than {@link #MAX_DIGITS} digits
     *       ({@code L6} has one), while identifiers are full of them.</li>
     * </ol>
     *
     * <p>Losing one tag's precision is recoverable. An unbounded label set is an outage, and it
     * arrives faster than the traffic that caused it.
     */
    static String safeTag(String value) {
        if (value == null || value.isBlank() || value.length() > MAX_TAG_LENGTH) {
            return UNKNOWN;
        }
        String trimmed = value.trim();
        int digits = 0;
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            boolean allowed = (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z')
                || (c >= '0' && c <= '9') || c == '_' || c == '-' || c == '.';
            if (!allowed) {
                return UNKNOWN;
            }
            if (c >= '0' && c <= '9' && ++digits > MAX_DIGITS) {
                return UNKNOWN;
            }
        }
        return trimmed;
    }
}
