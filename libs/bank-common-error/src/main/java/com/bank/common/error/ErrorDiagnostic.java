package com.bank.common.error;

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
public final class ErrorDiagnostic {

    private final String incidentId;
    private final String code;
    private final ErrorCategory category;
    private final String service;
    private final PlatformLayer layer;
    private final String component;
    private final String operation;
    private final ErrorOrigin origin;
    private final String reason;
    private final String upstreamSystem;
    private final String upstreamCode;
    private final Integer upstreamStatus;
    private final List<String> causeChain;
    private final String remediation;
    private final String runbook;

    private ErrorDiagnostic(Builder b) {
        this.incidentId = b.incidentId;
        this.code = b.code;
        this.category = b.category;
        this.service = b.service;
        this.layer = b.layer;
        this.component = b.component;
        this.operation = b.operation;
        this.origin = b.origin;
        this.reason = b.reason;
        this.upstreamSystem = b.upstreamSystem;
        this.upstreamCode = b.upstreamCode;
        this.upstreamStatus = b.upstreamStatus;
        this.causeChain = Collections.unmodifiableList(new ArrayList<>(b.causeChain));
        this.remediation = b.remediation;
        this.runbook = b.runbook;
    }

    public static Builder builder(String code) {
        return new Builder(code);
    }

    public String getIncidentId()      { return incidentId; }
    public String getCode()            { return code; }
    public ErrorCategory getCategory() { return category; }
    public String getService()         { return service; }
    public PlatformLayer getLayer()    { return layer; }
    public String getComponent()       { return component; }
    public String getOperation()       { return operation; }
    public ErrorOrigin getOrigin()     { return origin; }
    public String getReason()          { return reason; }
    public String getUpstreamSystem()  { return upstreamSystem; }
    public String getUpstreamCode()    { return upstreamCode; }
    public Integer getUpstreamStatus() { return upstreamStatus; }
    public List<String> getCauseChain(){ return causeChain; }
    public String getRemediation()     { return remediation; }
    public String getRunbook()         { return runbook; }

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

    /** Builder. {@code code} is required; everything else is best-effort context. */
    public static final class Builder {
        private final String code;
        private String incidentId = IncidentId.generate();
        private ErrorCategory category;
        private String service;
        private PlatformLayer layer;
        private String component;
        private String operation;
        private ErrorOrigin origin;
        private String reason;
        private String upstreamSystem;
        private String upstreamCode;
        private Integer upstreamStatus;
        private final List<String> causeChain = new ArrayList<>();
        private String remediation;
        private String runbook;

        private Builder(String code) {
            this.code = code;
            ErrorCatalogue.find(code).ifPresent(d -> {
                this.category = d.category();
                this.runbook = d.runbook();
            });
        }

        public Builder incidentId(String v)     { if (v != null) this.incidentId = v; return this; }
        public Builder category(ErrorCategory v){ this.category = v; return this; }
        public Builder service(String v)        { this.service = v; return this; }
        public Builder layer(PlatformLayer v)   { this.layer = v; return this; }
        public Builder component(String v)      { this.component = v; return this; }
        public Builder operation(String v)      { this.operation = v; return this; }
        public Builder origin(ErrorOrigin v)    { this.origin = v; return this; }
        public Builder reason(String v)         { this.reason = v; return this; }
        public Builder remediation(String v)    { this.remediation = v; return this; }
        public Builder runbook(String v)        { this.runbook = v; return this; }

        public Builder upstream(String system, String upstreamCode, Integer status) {
            this.upstreamSystem = system;
            this.upstreamCode = upstreamCode;
            this.upstreamStatus = status;
            return this;
        }

        /**
         * Records the exception chain, outermost first, as {@code SimpleName: message}.
         *
         * <p>Bounded at eight frames: a chain longer than that is a wrapping problem, and an
         * unbounded chain in a log line is how one failure fills a log budget.
         */
        public Builder cause(Throwable t) {
            Throwable current = t;
            int guard = 0;
            while (current != null && guard++ < 8) {
                causeChain.add(current.getClass().getSimpleName()
                    + (current.getMessage() != null ? ": " + current.getMessage() : ""));
                current = current.getCause() == current ? null : current.getCause();
            }
            return this;
        }

        public ErrorDiagnostic build() {
            return new ErrorDiagnostic(this);
        }
    }
}
