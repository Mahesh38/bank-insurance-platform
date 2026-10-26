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
   * This service's id, as it appears in every error response, log line and metric tag. Defaults to
   * {@code spring.application.name}.
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
   * production would put upstream prose and internal routes on a device, so production does not
   * merely default it off — it rejects the setting and fails to start.
   */
  private boolean exposeDiagnostics = false;

  /**
   * Status returned for a bean-validation failure. 422 platform-wide; {@code
   * bank-persistence-service} publishes 400 on {@code /internal/v1}.
   */
  private int validationStatus = 422;

  /** Status for an unparseable request body. Defaults to {@link #getValidationStatus()}. */
  private Integer malformedBodyStatus;

  /** Whether {@code bank.error.count} is emitted. */
  private boolean metricsEnabled = true;

  /**
   * This service's id, as it appears in every error response, log line and metric tag. Defaults to
   * {@code spring.application.name}.
   */
  @java.lang.SuppressWarnings("all")
  @MechanicallyGenerated
  public String getServiceId() {
    return this.serviceId;
  }

  /** The layer of the request ladder this service occupies. */
  @java.lang.SuppressWarnings("all")
  @MechanicallyGenerated
  public PlatformLayer getLayer() {
    return this.layer;
  }

  /**
   * Whether this service faces a client. Architectural and permanent, not an environment switch.
   *
   * <p>{@code PUBLIC} services redact; {@code INTERNAL} ones pass diagnostics to the calling
   * service, which redacts in its turn. Defaults to {@code PUBLIC}, so a service that declares
   * nothing leaks nothing.
   */
  @java.lang.SuppressWarnings("all")
  @MechanicallyGenerated
  public TrustBoundary getBoundary() {
    return this.boundary;
  }

  /**
   * Whether diagnostics may leave this service.
   *
   * <p><strong>False by default, and refused outright under the {@code prod} profile.</strong>
   * Turning it on in dev or UAT lets an engineer read the full reason, upstream code and cause
   * chain straight off the response instead of correlating by incident id. Turning it on in
   * production would put upstream prose and internal routes on a device, so production does not
   * merely default it off — it rejects the setting and fails to start.
   */
  @java.lang.SuppressWarnings("all")
  @MechanicallyGenerated
  public boolean isExposeDiagnostics() {
    return this.exposeDiagnostics;
  }

  /**
   * Status returned for a bean-validation failure. 422 platform-wide; {@code
   * bank-persistence-service} publishes 400 on {@code /internal/v1}.
   */
  @java.lang.SuppressWarnings("all")
  @MechanicallyGenerated
  public int getValidationStatus() {
    return this.validationStatus;
  }

  /** Status for an unparseable request body. Defaults to {@link #getValidationStatus()}. */
  @java.lang.SuppressWarnings("all")
  @MechanicallyGenerated
  public Integer getMalformedBodyStatus() {
    return this.malformedBodyStatus;
  }

  /** Whether {@code bank.error.count} is emitted. */
  @java.lang.SuppressWarnings("all")
  @MechanicallyGenerated
  public boolean isMetricsEnabled() {
    return this.metricsEnabled;
  }

  /**
   * This service's id, as it appears in every error response, log line and metric tag. Defaults to
   * {@code spring.application.name}.
   */
  @java.lang.SuppressWarnings("all")
  @MechanicallyGenerated
  public void setServiceId(final String serviceId) {
    this.serviceId = serviceId;
  }

  /** The layer of the request ladder this service occupies. */
  @java.lang.SuppressWarnings("all")
  @MechanicallyGenerated
  public void setLayer(final PlatformLayer layer) {
    this.layer = layer;
  }

  /**
   * Whether this service faces a client. Architectural and permanent, not an environment switch.
   *
   * <p>{@code PUBLIC} services redact; {@code INTERNAL} ones pass diagnostics to the calling
   * service, which redacts in its turn. Defaults to {@code PUBLIC}, so a service that declares
   * nothing leaks nothing.
   */
  @java.lang.SuppressWarnings("all")
  @MechanicallyGenerated
  public void setBoundary(final TrustBoundary boundary) {
    this.boundary = boundary;
  }

  /**
   * Whether diagnostics may leave this service.
   *
   * <p><strong>False by default, and refused outright under the {@code prod} profile.</strong>
   * Turning it on in dev or UAT lets an engineer read the full reason, upstream code and cause
   * chain straight off the response instead of correlating by incident id. Turning it on in
   * production would put upstream prose and internal routes on a device, so production does not
   * merely default it off — it rejects the setting and fails to start.
   */
  @java.lang.SuppressWarnings("all")
  @MechanicallyGenerated
  public void setExposeDiagnostics(final boolean exposeDiagnostics) {
    this.exposeDiagnostics = exposeDiagnostics;
  }

  /**
   * Status returned for a bean-validation failure. 422 platform-wide; {@code
   * bank-persistence-service} publishes 400 on {@code /internal/v1}.
   */
  @java.lang.SuppressWarnings("all")
  @MechanicallyGenerated
  public void setValidationStatus(final int validationStatus) {
    this.validationStatus = validationStatus;
  }

  /** Status for an unparseable request body. Defaults to {@link #getValidationStatus()}. */
  @java.lang.SuppressWarnings("all")
  @MechanicallyGenerated
  public void setMalformedBodyStatus(final Integer malformedBodyStatus) {
    this.malformedBodyStatus = malformedBodyStatus;
  }

  /** Whether {@code bank.error.count} is emitted. */
  @java.lang.SuppressWarnings("all")
  @MechanicallyGenerated
  public void setMetricsEnabled(final boolean metricsEnabled) {
    this.metricsEnabled = metricsEnabled;
  }
}
