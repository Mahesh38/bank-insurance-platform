package com.bank.common.error;

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
public final class ErrorHandlingSettings {
  /** Fails the build of a service that has no id: an unattributed error is the problem here. */
  private final String serviceId;

  private final PlatformLayer layer;

  /** Architectural and permanent: whether this service faces a client at all. */
  private final TrustBoundary boundary;

  /** An environment switch for debugging. Refused under a production profile. */
  private final boolean exposeDiagnostics;

  /**
   * The status this service returns for a bean-validation failure.
   *
   * <p>Configurable because the services genuinely differ and both are published: {@code
   * 1sb-integration-service} answers 422 for FUNC-001/FUNC-002 while {@code
   * bank-persistence-service} answers 400 on {@code /internal/v1}. Unifying them would be a silent
   * breaking change to a live contract, so it is a setting rather than a constant.
   */
  private final int validationStatus;

  /** The status for an unparseable body. Falls back to {@link #validationStatus()}. */
  private final Integer malformedBodyStatusOverride;

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
   * naming: {@link #boundary()} is <em>architectural</em> and permanent, while {@link
   * #exposeDiagnostics()} is an <em>environment</em> switch refused in production.
   *
   * <p>A single flag would have forced {@code 1sb-integration-service}, which is cluster-private
   * and legitimately does not redact, to set the debug switch — and then fail to start in
   * production against the guard meant to protect devices.
   */
  public boolean redacts() {
    return boundary == TrustBoundary.PUBLIC && !exposeDiagnostics;
  }

  @java.lang.SuppressWarnings("all")
  @MechanicallyGenerated
  private static PlatformLayer $default$layer() {
    return PlatformLayer.L5;
  }

  @java.lang.SuppressWarnings("all")
  @MechanicallyGenerated
  private static TrustBoundary $default$boundary() {
    return TrustBoundary.PUBLIC;
  }

  @java.lang.SuppressWarnings("all")
  @MechanicallyGenerated
  private static int $default$validationStatus() {
    return 422;
  }

  /**
   * Creates a new {@code ErrorHandlingSettings} instance.
   *
   * @param serviceId Fails the build of a service that has no id: an unattributed error is the
   *     problem here.
   * @param layer
   * @param boundary Architectural and permanent: whether this service faces a client at all.
   * @param exposeDiagnostics An environment switch for debugging. Refused under a production
   *     profile.
   * @param validationStatus The status this service returns for a bean-validation failure.
   *     <p>Configurable because the services genuinely differ and both are published: {@code
   *     1sb-integration-service} answers 422 for FUNC-001/FUNC-002 while {@code
   *     bank-persistence-service} answers 400 on {@code /internal/v1}. Unifying them would be a
   *     silent breaking change to a live contract, so it is a setting rather than a constant.
   * @param malformedBodyStatusOverride The status for an unparseable body. Falls back to {@link
   *     #validationStatus()}.
   */
  @java.lang.SuppressWarnings("all")
  @MechanicallyGenerated
  ErrorHandlingSettings(
      final String serviceId,
      final PlatformLayer layer,
      final TrustBoundary boundary,
      final boolean exposeDiagnostics,
      final int validationStatus,
      final Integer malformedBodyStatusOverride) {
    if (serviceId == null) {
      throw new java.lang.NullPointerException("serviceId is marked non-null but is null");
    }
    this.serviceId = serviceId;
    this.layer = layer;
    this.boundary = boundary;
    this.exposeDiagnostics = exposeDiagnostics;
    this.validationStatus = validationStatus;
    this.malformedBodyStatusOverride = malformedBodyStatusOverride;
  }

  @java.lang.SuppressWarnings("all")
  @MechanicallyGenerated
  public static class ErrorHandlingSettingsBuilder {
    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    private String serviceId;

    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    private boolean layer$set;

    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    private PlatformLayer layer$value;

    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    private boolean boundary$set;

    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    private TrustBoundary boundary$value;

    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    private boolean exposeDiagnostics;

    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    private boolean validationStatus$set;

    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    private int validationStatus$value;

    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    private Integer malformedBodyStatusOverride;

    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    ErrorHandlingSettingsBuilder() {}

    /**
     * Fails the build of a service that has no id: an unattributed error is the problem here.
     *
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    public ErrorHandlingSettings.ErrorHandlingSettingsBuilder serviceId(final String serviceId) {
      if (serviceId == null) {
        throw new java.lang.NullPointerException("serviceId is marked non-null but is null");
      }
      this.serviceId = serviceId;
      return this;
    }

    /**
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    public ErrorHandlingSettings.ErrorHandlingSettingsBuilder layer(final PlatformLayer layer) {
      this.layer$value = layer;
      layer$set = true;
      return this;
    }

    /**
     * Architectural and permanent: whether this service faces a client at all.
     *
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    public ErrorHandlingSettings.ErrorHandlingSettingsBuilder boundary(
        final TrustBoundary boundary) {
      this.boundary$value = boundary;
      boundary$set = true;
      return this;
    }

    /**
     * An environment switch for debugging. Refused under a production profile.
     *
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    public ErrorHandlingSettings.ErrorHandlingSettingsBuilder exposeDiagnostics(
        final boolean exposeDiagnostics) {
      this.exposeDiagnostics = exposeDiagnostics;
      return this;
    }

    /**
     * The status this service returns for a bean-validation failure.
     *
     * <p>Configurable because the services genuinely differ and both are published: {@code
     * 1sb-integration-service} answers 422 for FUNC-001/FUNC-002 while {@code
     * bank-persistence-service} answers 400 on {@code /internal/v1}. Unifying them would be a
     * silent breaking change to a live contract, so it is a setting rather than a constant.
     *
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    public ErrorHandlingSettings.ErrorHandlingSettingsBuilder validationStatus(
        final int validationStatus) {
      this.validationStatus$value = validationStatus;
      validationStatus$set = true;
      return this;
    }

    /**
     * The status for an unparseable body. Falls back to {@link #validationStatus()}.
     *
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    public ErrorHandlingSettings.ErrorHandlingSettingsBuilder malformedBodyStatusOverride(
        final Integer malformedBodyStatusOverride) {
      this.malformedBodyStatusOverride = malformedBodyStatusOverride;
      return this;
    }

    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    public ErrorHandlingSettings build() {
      PlatformLayer layer$value = this.layer$value;
      if (!this.layer$set) {
        layer$value = ErrorHandlingSettings.$default$layer();
      }
      TrustBoundary boundary$value = this.boundary$value;
      if (!this.boundary$set) {
        boundary$value = ErrorHandlingSettings.$default$boundary();
      }
      int validationStatus$value = this.validationStatus$value;
      if (!this.validationStatus$set) {
        validationStatus$value = ErrorHandlingSettings.$default$validationStatus();
      }
      return new ErrorHandlingSettings(
          this.serviceId,
          layer$value,
          boundary$value,
          this.exposeDiagnostics,
          validationStatus$value,
          this.malformedBodyStatusOverride);
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    public java.lang.String toString() {
      return "ErrorHandlingSettings.ErrorHandlingSettingsBuilder(serviceId="
          + this.serviceId
          + ", layer$value="
          + this.layer$value
          + ", boundary$value="
          + this.boundary$value
          + ", exposeDiagnostics="
          + this.exposeDiagnostics
          + ", validationStatus$value="
          + this.validationStatus$value
          + ", malformedBodyStatusOverride="
          + this.malformedBodyStatusOverride
          + ")";
    }
  }

  /** Fails the build of a service that has no id: an unattributed error is the problem here. */
  @java.lang.SuppressWarnings("all")
  @MechanicallyGenerated
  public String serviceId() {
    return this.serviceId;
  }

  @java.lang.SuppressWarnings("all")
  @MechanicallyGenerated
  public PlatformLayer layer() {
    return this.layer;
  }

  /** Architectural and permanent: whether this service faces a client at all. */
  @java.lang.SuppressWarnings("all")
  @MechanicallyGenerated
  public TrustBoundary boundary() {
    return this.boundary;
  }

  /** An environment switch for debugging. Refused under a production profile. */
  @java.lang.SuppressWarnings("all")
  @MechanicallyGenerated
  public boolean exposeDiagnostics() {
    return this.exposeDiagnostics;
  }

  /**
   * The status this service returns for a bean-validation failure.
   *
   * <p>Configurable because the services genuinely differ and both are published: {@code
   * 1sb-integration-service} answers 422 for FUNC-001/FUNC-002 while {@code
   * bank-persistence-service} answers 400 on {@code /internal/v1}. Unifying them would be a silent
   * breaking change to a live contract, so it is a setting rather than a constant.
   */
  @java.lang.SuppressWarnings("all")
  @MechanicallyGenerated
  public int validationStatus() {
    return this.validationStatus;
  }

  /** The status for an unparseable body. Falls back to {@link #validationStatus()}. */
  @java.lang.SuppressWarnings("all")
  @MechanicallyGenerated
  public Integer malformedBodyStatusOverride() {
    return this.malformedBodyStatusOverride;
  }

  @java.lang.Override
  @java.lang.SuppressWarnings("all")
  @MechanicallyGenerated
  public boolean equals(final java.lang.Object o) {
    if (o == this) {
      return true;
    }
    if (!(o instanceof ErrorHandlingSettings)) {
      return false;
    }
    final ErrorHandlingSettings other = (ErrorHandlingSettings) o;
    if (this.exposeDiagnostics() != other.exposeDiagnostics()) {
      return false;
    }
    if (this.validationStatus() != other.validationStatus()) {
      return false;
    }
    final java.lang.Object this$malformedBodyStatusOverride = this.malformedBodyStatusOverride();
    final java.lang.Object other$malformedBodyStatusOverride = other.malformedBodyStatusOverride();
    if (this$malformedBodyStatusOverride == null
        ? other$malformedBodyStatusOverride != null
        : !this$malformedBodyStatusOverride.equals(other$malformedBodyStatusOverride)) {
      return false;
    }
    final java.lang.Object this$serviceId = this.serviceId();
    final java.lang.Object other$serviceId = other.serviceId();
    if (this$serviceId == null
        ? other$serviceId != null
        : !this$serviceId.equals(other$serviceId)) {
      return false;
    }
    final java.lang.Object this$layer = this.layer();
    final java.lang.Object other$layer = other.layer();
    if (this$layer == null ? other$layer != null : !this$layer.equals(other$layer)) {
      return false;
    }
    final java.lang.Object this$boundary = this.boundary();
    final java.lang.Object other$boundary = other.boundary();
    if (this$boundary == null ? other$boundary != null : !this$boundary.equals(other$boundary)) {
      return false;
    }
    return true;
  }

  @java.lang.Override
  @java.lang.SuppressWarnings("all")
  @MechanicallyGenerated
  public int hashCode() {
    final int PRIME = 59;
    int result = 1;
    result = result * PRIME + (this.exposeDiagnostics() ? 79 : 97);
    result = result * PRIME + this.validationStatus();
    final java.lang.Object $malformedBodyStatusOverride = this.malformedBodyStatusOverride();
    result =
        result * PRIME
            + ($malformedBodyStatusOverride == null ? 43 : $malformedBodyStatusOverride.hashCode());
    final java.lang.Object $serviceId = this.serviceId();
    result = result * PRIME + ($serviceId == null ? 43 : $serviceId.hashCode());
    final java.lang.Object $layer = this.layer();
    result = result * PRIME + ($layer == null ? 43 : $layer.hashCode());
    final java.lang.Object $boundary = this.boundary();
    result = result * PRIME + ($boundary == null ? 43 : $boundary.hashCode());
    return result;
  }

  @java.lang.Override
  @java.lang.SuppressWarnings("all")
  @MechanicallyGenerated
  public java.lang.String toString() {
    return "ErrorHandlingSettings(serviceId="
        + this.serviceId()
        + ", layer="
        + this.layer()
        + ", boundary="
        + this.boundary()
        + ", exposeDiagnostics="
        + this.exposeDiagnostics()
        + ", validationStatus="
        + this.validationStatus()
        + ", malformedBodyStatusOverride="
        + this.malformedBodyStatusOverride()
        + ")";
  }
}
