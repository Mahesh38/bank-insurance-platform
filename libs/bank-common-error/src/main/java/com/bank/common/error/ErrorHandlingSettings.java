package com.bank.common.error;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.Accessors;

/**
 * What one service needs to know to answer errors consistently: who it is, where it sits, and
 * whether it redacts.
 *
 * <p>These were literals scattered across the codebase — the service id alone appeared thirty-one
 * times. A rename was a thirty-one-site edit in which a single typo produced a <em>silently
 * mis-attributed</em> error, which defeats the purpose of recording the service at all. They are
 * now resolved once, from configuration, and injected.
 *
 * <p>Every default is the safe one. {@link TrustBoundary#PUBLIC} means a service that declares
 * nothing redacts, so forgetting to configure a new service leaks nothing.
 */
@Value
@Builder
@Accessors(fluent = true)
public class ErrorHandlingSettings {

    /** Fails the build of a service that has no id: an unattributed error is the problem here. */
    @NonNull String serviceId;

    @Builder.Default PlatformLayer layer = PlatformLayer.L5;

    /** Architectural and permanent: whether this service faces a client at all. */
    @Builder.Default TrustBoundary boundary = TrustBoundary.PUBLIC;

    /** An environment switch for debugging. Refused under a production profile. */
    boolean exposeDiagnostics;

    /**
     * The status this service returns for a bean-validation failure.
     *
     * <p>Configurable because the services genuinely differ and both are published:
     * {@code 1sb-integration-service} answers 422 for FUNC-001/FUNC-002 while
     * {@code bank-persistence-service} answers 400 on {@code /internal/v1}. Unifying them would be
     * a silent breaking change to a live contract, so it is a setting rather than a constant.
     */
    @Builder.Default int validationStatus = 422;

    /** The status for an unparseable body. Falls back to {@link #validationStatus()}. */
    Integer malformedBodyStatusOverride;

    public static ErrorHandlingSettingsBuilder builder(String serviceId) {
        return new ErrorHandlingSettingsBuilder().serviceId(serviceId);
    }

    public int malformedBodyStatus() {
        return malformedBodyStatusOverride != null ? malformedBodyStatusOverride : validationStatus;
    }

    /**
     * True when this service must strip diagnostics before responding.
     *
     * <p>Two independent things decide this, and collapsing them into one flag is a mistake worth
     * naming: {@link #boundary()} is <em>architectural</em> and permanent, while
     * {@link #exposeDiagnostics()} is an <em>environment</em> switch refused in production.
     *
     * <p>A single flag would have forced {@code 1sb-integration-service}, which is cluster-private
     * and legitimately does not redact, to set the debug switch — and then fail to start in
     * production against the guard meant to protect devices.
     */
    public boolean redacts() {
        return boundary == TrustBoundary.PUBLIC && !exposeDiagnostics;
    }
}
