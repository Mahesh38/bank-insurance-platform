package com.bank.insurance.onesb.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.util.List;
import java.util.Locale;

/**
 * Which audit sinks are active, per environment (RISK-012).
 *
 * <p>{@code insurance.audit.sinks} is a list so the transport decision stays a configuration
 * choice rather than a code change. {@code LOG,PERSISTENCE} is the default: the log line serves
 * the operator during an incident, the persisted row serves the auditor two years later.
 */
@ConfigurationProperties(prefix = "insurance.audit")
public record AuditProperties(@DefaultValue("LOG,PERSISTENCE") List<String> sinks) {

    /**
     * Audit transports. {@code KAFKA} is declared but not implemented — see
     * {@code AuditSinkConfig}, which fails startup rather than silently dropping events if it is
     * selected. Naming it here keeps the intended shape of the decision visible instead of
     * leaving it as tribal knowledge.
     */
    public enum Sink {
        /** Structured line to the application log. Operator-facing, not durable evidence. */
        LOG,
        /** HTTP append to {@code audit_event} in bank-persistence-service. Durable. */
        PERSISTENCE,
        /** Reserved for an event-backbone transport. NOT IMPLEMENTED. */
        KAFKA
    }

    public List<Sink> resolvedSinks() {
        return sinks.stream()
                .map(s -> s.trim().toUpperCase(Locale.ROOT))
                .filter(s -> !s.isEmpty())
                .map(s -> {
                    try {
                        return Sink.valueOf(s);
                    } catch (IllegalArgumentException e) {
                        throw new IllegalStateException(
                                "Unknown audit sink '" + s + "'. Valid values: "
                                        + java.util.Arrays.toString(Sink.values())
                                        + ". Check insurance.audit.sinks.", e);
                    }
                })
                .distinct()
                .toList();
    }
}
