package com.bank.common.error;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;

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
@Builder(builderClassName = "Builder", builderMethodName = "hiddenBuilder")
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

    /** Exception classes and redacted messages, outermost first. */
    @Singular("causeFrame") List<String> causeChain;

    String remediation;
    String runbook;

    /**
     * Starts a diagnostic for {@code code}, pre-filled from the catalogue where it is registered.
     *
     * <p>This is the entry point rather than a bare {@code builder()} because a diagnostic without
     * a code cannot look up its own category or runbook, and one built that way would reach a log
     * line missing exactly the fields support searches on.
     */
    public static Builder builder(String code) {
        Builder builder = hiddenBuilder().code(code).incidentId(IncidentId.generate());
        ErrorCatalogue.find(code).ifPresent(d -> builder.category(d.category()).runbook(d.runbook()));
        return builder;
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
     * Only the three builder methods that do something a generated setter cannot.
     *
     * <p>The other ten were pass-throughs to the generated builder — code whose entire content was
     * its own signature. Nothing here reaches into Lombok's generated internals: a shared library
     * coupled to codegen field names breaks on an upgrade, in every consumer at once.
     */
    public static class Builder {

        /** The three upstream fields always travel together. */
        public Builder upstream(String system, String code, Integer status) {
            return upstreamSystem(system).upstreamCode(code).upstreamStatus(status);
        }

        /**
         * Records the exception chain, outermost first, as {@code SimpleName: message}.
         *
         * <p>Bounded at {@link #MAX_CAUSE_DEPTH} frames: a chain longer than that says more about
         * wrapping than cause, and an unbounded chain in a log line is how one failure fills a log
         * budget.
         */
        public Builder cause(Throwable t) {
            Throwable current = t;
            int depth = 0;
            while (current != null && depth++ < MAX_CAUSE_DEPTH) {
                causeFrame(current.getClass().getSimpleName()
                    + (current.getMessage() != null ? ": " + current.getMessage() : ""));
                current = current.getCause() == current ? null : current.getCause();
            }
            return this;
        }
    }

    /** Deepest exception chain recorded. */
    static final int MAX_CAUSE_DEPTH = 8;
}
