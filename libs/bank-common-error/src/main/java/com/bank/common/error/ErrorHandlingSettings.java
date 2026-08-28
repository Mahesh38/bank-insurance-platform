package com.bank.common.error;

import java.util.Objects;

/**
 * What one service needs to know to answer errors consistently: who it is, where it sits, and
 * whether it redacts.
 *
 * <p>These were literals scattered across the codebase — the service id alone appeared as a string
 * constant thirty-one times. A rename was a thirty-one-site edit in which a single typo produced a
 * <em>silently mis-attributed</em> error, which defeats the purpose of recording the service at all.
 * They are now resolved once, from configuration, and injected.
 *
 * <p>Every default is the safe one. {@link TrustBoundary#PUBLIC} means a service that declares
 * nothing redacts, so forgetting to configure a new service leaks nothing.
 */
public final class ErrorHandlingSettings {

    private final String serviceId;
    private final PlatformLayer layer;
    private final TrustBoundary boundary;
    private final boolean exposeDiagnostics;
    private final int validationStatus;
    private final int malformedBodyStatus;

    private ErrorHandlingSettings(Builder b) {
        this.serviceId = Objects.requireNonNull(b.serviceId, "serviceId must be configured — "
            + "set bank.error.service-id or spring.application.name");
        this.layer = b.layer;
        this.boundary = b.boundary;
        this.exposeDiagnostics = b.exposeDiagnostics;
        this.validationStatus = b.validationStatus;
        this.malformedBodyStatus = b.malformedBodyStatus != null
            ? b.malformedBodyStatus : b.validationStatus;
    }

    public static Builder builder(String serviceId) {
        return new Builder(serviceId);
    }

    public String serviceId()          { return serviceId; }
    public PlatformLayer layer()       { return layer; }
    public TrustBoundary boundary()    { return boundary; }

    /**
     * The status this service returns for a bean-validation failure.
     *
     * <p>Configurable because the services genuinely differ and both are published:
     * {@code 1sb-integration-service} answers 422 for FUNC-001/FUNC-002 while
     * {@code bank-persistence-service} answers 400 on {@code /internal/v1}. Unifying them would be
     * a silent breaking change to a live contract, so it is a setting rather than a constant.
     */
    public int validationStatus()      { return validationStatus; }

    /** The status for an unparseable body. Defaults to {@link #validationStatus()}. */
    public int malformedBodyStatus()   { return malformedBodyStatus; }

    /** True when diagnostics are deliberately exposed for debugging. Never true in production. */
    public boolean exposeDiagnostics() {
        return exposeDiagnostics;
    }

    /**
     * True when this service must strip diagnostics before responding.
     *
     * <p>Two independent things decide this, and collapsing them into one flag is a mistake worth
     * naming: {@link #boundary()} is <em>architectural</em> — whether this service faces a device
     * at all — and is permanent for the life of the service. {@link #exposeDiagnostics()} is an
     * <em>environment</em> switch for debugging, and is refused in production.
     *
     * <p>A single flag would have forced {@code 1sb-integration-service}, which is cluster-private
     * and legitimately does not redact, to set the debug switch — and then fail to start in
     * production against the guard meant to protect devices.
     */
    public boolean redacts() {
        return boundary == TrustBoundary.PUBLIC && !exposeDiagnostics;
    }

    public static final class Builder {
        private final String serviceId;
        private PlatformLayer layer = PlatformLayer.L5;
        private TrustBoundary boundary = TrustBoundary.PUBLIC;
        private boolean exposeDiagnostics = false;
        private int validationStatus = 422;
        private Integer malformedBodyStatus;

        private Builder(String serviceId) {
            this.serviceId = serviceId;
        }

        public Builder layer(PlatformLayer v)           { if (v != null) this.layer = v; return this; }
        public Builder boundary(TrustBoundary v)        { if (v != null) this.boundary = v; return this; }
        public Builder exposeDiagnostics(boolean v)     { this.exposeDiagnostics = v; return this; }
        public Builder validationStatus(int v)          { this.validationStatus = v; return this; }
        public Builder malformedBodyStatus(Integer v)   { this.malformedBodyStatus = v; return this; }

        public ErrorHandlingSettings build() {
            return new ErrorHandlingSettings(this);
        }
    }
}
