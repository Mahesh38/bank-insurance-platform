package com.bank.common.error;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Error-handling configuration, bound from {@code bank.error.*}.
 *
 * <p>Everything here was previously a literal compiled into a service. The defaults are chosen so
 * that a service which configures nothing behaves safely rather than conveniently.
 */
@ConfigurationProperties("bank.error")
public class PlatformErrorProperties {

    /**
     * This service's id, as it appears in every error response, log line and metric tag.
     * Defaults to {@code spring.application.name}.
     */
    private String serviceId;

    /** The layer of the request ladder this service occupies. */
    private PlatformLayer layer = PlatformLayer.L5;

    /**
     * Whether this service faces a client. Architectural and permanent, not an environment switch.
     *
     * <p>{@code PUBLIC} services redact; {@code INTERNAL} ones pass diagnostics to the calling
     * service, which redacts in its turn. Defaults to {@code PUBLIC}, so a service that declares
     * nothing leaks nothing.
     */
    private TrustBoundary boundary = TrustBoundary.PUBLIC;

    /**
     * Whether diagnostics may leave this service.
     *
     * <p><strong>False by default, and refused outright under the {@code prod} profile.</strong>
     * Turning it on in dev or UAT lets an engineer read the full reason, upstream code and cause
     * chain straight off the response instead of correlating by incident id. Turning it on in
     * production would put upstream prose and internal routes on a device, which is precisely the
     * defect this contract was written to close — so production does not merely default it off, it
     * rejects the setting and fails to start.
     */
    private boolean exposeDiagnostics = false;

    /**
     * Status returned for a bean-validation failure. 422 platform-wide; {@code bank-persistence-service}
     * publishes 400 on {@code /internal/v1} and sets it here rather than by subclassing.
     */
    private int validationStatus = 422;

    /** Status for an unparseable request body. Defaults to {@link #getValidationStatus()}. */
    private Integer malformedBodyStatus;

    /** Whether {@code bank.error.count} is emitted. */
    private boolean metricsEnabled = true;

    public String getServiceId() { return serviceId; }
    public void setServiceId(String serviceId) { this.serviceId = serviceId; }

    public PlatformLayer getLayer() { return layer; }
    public void setLayer(PlatformLayer layer) { this.layer = layer; }

    public TrustBoundary getBoundary() { return boundary; }
    public void setBoundary(TrustBoundary boundary) { this.boundary = boundary; }

    public boolean isExposeDiagnostics() { return exposeDiagnostics; }
    public void setExposeDiagnostics(boolean exposeDiagnostics) { this.exposeDiagnostics = exposeDiagnostics; }

    public int getValidationStatus() { return validationStatus; }
    public void setValidationStatus(int validationStatus) { this.validationStatus = validationStatus; }

    public Integer getMalformedBodyStatus() { return malformedBodyStatus; }
    public void setMalformedBodyStatus(Integer malformedBodyStatus) { this.malformedBodyStatus = malformedBodyStatus; }

    public boolean isMetricsEnabled() { return metricsEnabled; }
    public void setMetricsEnabled(boolean metricsEnabled) { this.metricsEnabled = metricsEnabled; }
}
