package com.bank.common.error;

import lombok.Builder;
import lombok.Value;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The engineer-facing half of a failure. <strong>Never crosses the redaction boundary.</strong>
 *
 * <p>This is what makes a log answer the questions a support engineer actually asks
 * ({@code 07-PLATFORM-ERROR-CONTRACT.md §6}):
 *
 * <table border="1">
 *   <caption>Question to field</caption>
 *   <tr><td>What failed</td><td>{@code code}, {@code category}</td></tr>
 *   <tr><td>Where</td><td>{@code service}, {@code layer}, {@code component}, {@code operation}, {@code origin}</td></tr>
 *   <tr><td>When / which request</td><td>{@code incidentId}, {@code correlationId}</td></tr>
 *   <tr><td>Why</td><td>{@code reason}, {@code upstreamCode}, {@code upstreamStatus}</td></tr>
 *   <tr><td>How</td><td>{@code causeChain}</td></tr>
 *   <tr><td>What to do</td><td>{@code remediation}, {@code runbook}</td></tr>
 * </table>
 *
 * <p>{@code reason} is the one free-text field, and it is the one an engineer writes. It must
 * describe the decision, never echo a request body: identifiers on the non-PII allow-list only
 * ({@code 07 §8}).
 */
@Value
@Builder(builderMethodName = "hiddenBuilder")
public class ErrorDiagnostic {

    String incidentId;
    String code;
    ErrorCategory category;
    String service;
    PlatformLayer layer;
    String component;
    String operation;
    ErrorOrigin origin;
    String reason;
    String upstreamSystem;
    String upstreamCode;
    Integer upstreamStatus;
    List<String> causeChain;
    String remediation;
    String runbook;

    /** Starts a diagnostic for {@code code}, pre-filled from the catalogue where it is registered. */
    public static Builder builder(String code) {
        return new Builder(code);
    }

    /**
     * The service that actually failed — the origin's service when the failure arrived from
     * another service, otherwise this one.
     *
     * <p>This is the value the {@code originService} metric tag carries, and the one that answers
     * "is it us or them" on a dashboard without reading a single log line.
     */
    public String effectiveOriginService() {
        return origin != null ? origin.service() : service;
    }

    /** True when this failure began in another service. */
    public boolean isPropagated() {
        return origin != null && !origin.service().equals(service);
    }

    @Override
    public String toString() {
        return "ErrorDiagnostic{incidentId=" + incidentId + ", code=" + code
            + ", service=" + service + ", layer=" + layer
            + ", origin=" + (origin != null ? origin.service() + "/" + origin.code() : "none") + "}";
    }

    /**
     * Builder. {@code code} is required; everything else is best-effort context.
     *
     * <p>Hand-written rather than generated, because it does two things generation cannot: it seeds
     * the category and runbook from the catalogue, and {@link #cause(Throwable)} walks an exception
     * chain into strings.
     */
    public static final class Builder {
        private final ErrorDiagnosticBuilder delegate = ErrorDiagnostic.hiddenBuilder();
        private final List<String> causeChain = new ArrayList<>();

        private Builder(String code) {
            delegate.code(code).incidentId(IncidentId.generate());
            ErrorCatalogue.find(code).ifPresent(d -> delegate.category(d.category()).runbook(d.runbook()));
        }

        public Builder incidentId(String v)     { if (v != null) delegate.incidentId(v); return this; }
        public Builder category(ErrorCategory v){ delegate.category(v); return this; }
        public Builder service(String v)        { delegate.service(v); return this; }
        public Builder layer(PlatformLayer v)   { delegate.layer(v); return this; }
        public Builder component(String v)      { delegate.component(v); return this; }
        public Builder operation(String v)      { delegate.operation(v); return this; }
        public Builder origin(ErrorOrigin v)    { delegate.origin(v); return this; }
        public Builder reason(String v)         { delegate.reason(v); return this; }
        public Builder remediation(String v)    { delegate.remediation(v); return this; }
        public Builder runbook(String v)        { delegate.runbook(v); return this; }

        public Builder upstream(String system, String upstreamCode, Integer status) {
            delegate.upstreamSystem(system).upstreamCode(upstreamCode).upstreamStatus(status);
            return this;
        }

        /**
         * Records the exception chain, outermost first, as {@code SimpleName: message}.
         *
         * <p>Bounded at {@link #MAX_CAUSE_DEPTH} frames: a chain longer than that is a wrapping
         * problem, and an unbounded chain in a log line is how one failure fills a log budget.
         */
        public Builder cause(Throwable t) {
            Throwable current = t;
            int depth = 0;
            while (current != null && depth++ < MAX_CAUSE_DEPTH) {
                causeChain.add(current.getClass().getSimpleName()
                    + (current.getMessage() != null ? ": " + current.getMessage() : ""));
                current = current.getCause() == current ? null : current.getCause();
            }
            return this;
        }

        public ErrorDiagnostic build() {
            return delegate.causeChain(Collections.unmodifiableList(new ArrayList<>(causeChain))).build();
        }
    }

    /** Deepest exception chain recorded. Beyond this the chain says more about wrapping than cause. */
    static final int MAX_CAUSE_DEPTH = 8;
}
