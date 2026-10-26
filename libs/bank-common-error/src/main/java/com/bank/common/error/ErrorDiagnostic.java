package com.bank.common.error;

import java.util.List;

/**
 * The engineer-facing half of a failure. <strong>Never crosses the redaction boundary.</strong>
 *
 * <p>This is what makes a log answer the questions a support engineer actually asks ({@code
 * 07-PLATFORM-ERROR-CONTRACT.md §6}):
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

  /** Exception classes and redacted messages, outermost first. */
  private final List<String> causeChain;

  private final String remediation;
  private final String runbook;

  /**
   * Starts a diagnostic for {@code code}, pre-filled from the catalogue where it is registered.
   *
   * <p>This is the entry point rather than a bare {@code builder()} because a diagnostic without a
   * code cannot look up its own category or runbook, and one built that way would reach a log line
   * missing exactly the fields support searches on.
   */
  public static Builder builder(String code) {
    Builder builder = hiddenBuilder().code(code).incidentId(IncidentId.generate());
    ErrorCatalogue.find(code).ifPresent(d -> builder.category(d.category()).runbook(d.runbook()));
    return builder;
  }

  /**
   * The service that actually failed — the origin's service when the failure arrived from another
   * service, otherwise this one.
   *
   * <p>This is the value the {@code originService} metric tag carries, and the one that answers "is
   * it us or them" on a dashboard without reading a single log line.
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
    return "ErrorDiagnostic{incidentId="
        + incidentId
        + ", code="
        + code
        + ", service="
        + service
        + ", layer="
        + layer
        + ", origin="
        + (origin != null ? origin.service() + "/" + origin.code() : "none")
        + "}";
  }

  /**
   * Only the three builder methods that do something a generated setter cannot.
   *
   * <p>The other ten were pass-throughs to the generated builder — code whose entire content was
   * its own signature. Nothing here reaches into Lombok's generated internals: a shared library
   * coupled to codegen field names breaks on an upgrade, in every consumer at once.
   */
  public static class Builder {
    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    private String incidentId;

    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    private String code;

    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    private ErrorCategory category;

    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    private String service;

    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    private PlatformLayer layer;

    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    private String component;

    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    private String operation;

    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    private ErrorOrigin origin;

    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    private String reason;

    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    private String upstreamSystem;

    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    private String upstreamCode;

    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    private Integer upstreamStatus;

    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    private java.util.ArrayList<String> causeChain;

    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    private String remediation;

    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    private String runbook;

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
        causeFrame(
            current.getClass().getSimpleName()
                + (current.getMessage() != null ? ": " + current.getMessage() : ""));
        current = current.getCause() == current ? null : current.getCause();
      }
      return this;
    }

    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    Builder() {}

    /**
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    public ErrorDiagnostic.Builder incidentId(final String incidentId) {
      this.incidentId = incidentId;
      return this;
    }

    /**
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    public ErrorDiagnostic.Builder code(final String code) {
      this.code = code;
      return this;
    }

    /**
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    public ErrorDiagnostic.Builder category(final ErrorCategory category) {
      this.category = category;
      return this;
    }

    /**
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    public ErrorDiagnostic.Builder service(final String service) {
      this.service = service;
      return this;
    }

    /**
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    public ErrorDiagnostic.Builder layer(final PlatformLayer layer) {
      this.layer = layer;
      return this;
    }

    /**
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    public ErrorDiagnostic.Builder component(final String component) {
      this.component = component;
      return this;
    }

    /**
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    public ErrorDiagnostic.Builder operation(final String operation) {
      this.operation = operation;
      return this;
    }

    /**
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    public ErrorDiagnostic.Builder origin(final ErrorOrigin origin) {
      this.origin = origin;
      return this;
    }

    /**
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    public ErrorDiagnostic.Builder reason(final String reason) {
      this.reason = reason;
      return this;
    }

    /**
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    public ErrorDiagnostic.Builder upstreamSystem(final String upstreamSystem) {
      this.upstreamSystem = upstreamSystem;
      return this;
    }

    /**
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    public ErrorDiagnostic.Builder upstreamCode(final String upstreamCode) {
      this.upstreamCode = upstreamCode;
      return this;
    }

    /**
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    public ErrorDiagnostic.Builder upstreamStatus(final Integer upstreamStatus) {
      this.upstreamStatus = upstreamStatus;
      return this;
    }

    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    public ErrorDiagnostic.Builder causeFrame(final String causeFrame) {
      if (this.causeChain == null) {
        this.causeChain = new java.util.ArrayList<String>();
      }
      this.causeChain.add(causeFrame);
      return this;
    }

    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    public ErrorDiagnostic.Builder causeChain(
        final java.util.Collection<? extends String> causeChain) {
      if (causeChain == null) {
        throw new java.lang.NullPointerException("causeChain cannot be null");
      }
      if (this.causeChain == null) {
        this.causeChain = new java.util.ArrayList<String>();
      }
      this.causeChain.addAll(causeChain);
      return this;
    }

    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    public ErrorDiagnostic.Builder clearCauseChain() {
      if (this.causeChain != null) {
        this.causeChain.clear();
      }
      return this;
    }

    /**
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    public ErrorDiagnostic.Builder remediation(final String remediation) {
      this.remediation = remediation;
      return this;
    }

    /**
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    public ErrorDiagnostic.Builder runbook(final String runbook) {
      this.runbook = runbook;
      return this;
    }

    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    public ErrorDiagnostic build() {
      java.util.List<String> causeChain;
      switch (this.causeChain == null ? 0 : this.causeChain.size()) {
        case 0:
          causeChain = java.util.Collections.emptyList();
          break;
        case 1:
          causeChain = java.util.Collections.singletonList(this.causeChain.get(0));
          break;
        default:
          causeChain =
              java.util.Collections.unmodifiableList(
                  new java.util.ArrayList<String>(this.causeChain));
      }
      return new ErrorDiagnostic(
          this.incidentId,
          this.code,
          this.category,
          this.service,
          this.layer,
          this.component,
          this.operation,
          this.origin,
          this.reason,
          this.upstreamSystem,
          this.upstreamCode,
          this.upstreamStatus,
          causeChain,
          this.remediation,
          this.runbook);
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    public java.lang.String toString() {
      return "ErrorDiagnostic.Builder(incidentId="
          + this.incidentId
          + ", code="
          + this.code
          + ", category="
          + this.category
          + ", service="
          + this.service
          + ", layer="
          + this.layer
          + ", component="
          + this.component
          + ", operation="
          + this.operation
          + ", origin="
          + this.origin
          + ", reason="
          + this.reason
          + ", upstreamSystem="
          + this.upstreamSystem
          + ", upstreamCode="
          + this.upstreamCode
          + ", upstreamStatus="
          + this.upstreamStatus
          + ", causeChain="
          + this.causeChain
          + ", remediation="
          + this.remediation
          + ", runbook="
          + this.runbook
          + ")";
    }
  }

  /** Deepest exception chain recorded. */
  static final int MAX_CAUSE_DEPTH = 8;

  /**
   * Creates a new {@code ErrorDiagnostic} instance.
   *
   * @param incidentId
   * @param code
   * @param category
   * @param service
   * @param layer
   * @param component
   * @param operation
   * @param origin
   * @param reason
   * @param upstreamSystem
   * @param upstreamCode
   * @param upstreamStatus
   * @param causeChain Exception classes and redacted messages, outermost first.
   * @param remediation
   * @param runbook
   */
  @java.lang.SuppressWarnings("all")
  @MechanicallyGenerated
  ErrorDiagnostic(
      final String incidentId,
      final String code,
      final ErrorCategory category,
      final String service,
      final PlatformLayer layer,
      final String component,
      final String operation,
      final ErrorOrigin origin,
      final String reason,
      final String upstreamSystem,
      final String upstreamCode,
      final Integer upstreamStatus,
      final List<String> causeChain,
      final String remediation,
      final String runbook) {
    this.incidentId = incidentId;
    this.code = code;
    this.category = category;
    this.service = service;
    this.layer = layer;
    this.component = component;
    this.operation = operation;
    this.origin = origin;
    this.reason = reason;
    this.upstreamSystem = upstreamSystem;
    this.upstreamCode = upstreamCode;
    this.upstreamStatus = upstreamStatus;
    this.causeChain = causeChain;
    this.remediation = remediation;
    this.runbook = runbook;
  }

  @java.lang.SuppressWarnings("all")
  @MechanicallyGenerated
  public static ErrorDiagnostic.Builder hiddenBuilder() {
    return new ErrorDiagnostic.Builder();
  }

  @java.lang.SuppressWarnings("all")
  @MechanicallyGenerated
  public String getIncidentId() {
    return this.incidentId;
  }

  @java.lang.SuppressWarnings("all")
  @MechanicallyGenerated
  public String getCode() {
    return this.code;
  }

  @java.lang.SuppressWarnings("all")
  @MechanicallyGenerated
  public ErrorCategory getCategory() {
    return this.category;
  }

  @java.lang.SuppressWarnings("all")
  @MechanicallyGenerated
  public String getService() {
    return this.service;
  }

  @java.lang.SuppressWarnings("all")
  @MechanicallyGenerated
  public PlatformLayer getLayer() {
    return this.layer;
  }

  @java.lang.SuppressWarnings("all")
  @MechanicallyGenerated
  public String getComponent() {
    return this.component;
  }

  @java.lang.SuppressWarnings("all")
  @MechanicallyGenerated
  public String getOperation() {
    return this.operation;
  }

  @java.lang.SuppressWarnings("all")
  @MechanicallyGenerated
  public ErrorOrigin getOrigin() {
    return this.origin;
  }

  @java.lang.SuppressWarnings("all")
  @MechanicallyGenerated
  public String getReason() {
    return this.reason;
  }

  @java.lang.SuppressWarnings("all")
  @MechanicallyGenerated
  public String getUpstreamSystem() {
    return this.upstreamSystem;
  }

  @java.lang.SuppressWarnings("all")
  @MechanicallyGenerated
  public String getUpstreamCode() {
    return this.upstreamCode;
  }

  @java.lang.SuppressWarnings("all")
  @MechanicallyGenerated
  public Integer getUpstreamStatus() {
    return this.upstreamStatus;
  }

  /** Exception classes and redacted messages, outermost first. */
  @java.lang.SuppressWarnings("all")
  @MechanicallyGenerated
  public List<String> getCauseChain() {
    return this.causeChain;
  }

  @java.lang.SuppressWarnings("all")
  @MechanicallyGenerated
  public String getRemediation() {
    return this.remediation;
  }

  @java.lang.SuppressWarnings("all")
  @MechanicallyGenerated
  public String getRunbook() {
    return this.runbook;
  }

  @java.lang.Override
  @java.lang.SuppressWarnings("all")
  @MechanicallyGenerated
  public boolean equals(final java.lang.Object o) {
    if (o == this) {
      return true;
    }
    if (!(o instanceof ErrorDiagnostic)) {
      return false;
    }
    final ErrorDiagnostic other = (ErrorDiagnostic) o;
    final java.lang.Object this$upstreamStatus = this.getUpstreamStatus();
    final java.lang.Object other$upstreamStatus = other.getUpstreamStatus();
    if (this$upstreamStatus == null
        ? other$upstreamStatus != null
        : !this$upstreamStatus.equals(other$upstreamStatus)) {
      return false;
    }
    final java.lang.Object this$incidentId = this.getIncidentId();
    final java.lang.Object other$incidentId = other.getIncidentId();
    if (this$incidentId == null
        ? other$incidentId != null
        : !this$incidentId.equals(other$incidentId)) {
      return false;
    }
    final java.lang.Object this$code = this.getCode();
    final java.lang.Object other$code = other.getCode();
    if (this$code == null ? other$code != null : !this$code.equals(other$code)) {
      return false;
    }
    final java.lang.Object this$category = this.getCategory();
    final java.lang.Object other$category = other.getCategory();
    if (this$category == null ? other$category != null : !this$category.equals(other$category)) {
      return false;
    }
    final java.lang.Object this$service = this.getService();
    final java.lang.Object other$service = other.getService();
    if (this$service == null ? other$service != null : !this$service.equals(other$service)) {
      return false;
    }
    final java.lang.Object this$layer = this.getLayer();
    final java.lang.Object other$layer = other.getLayer();
    if (this$layer == null ? other$layer != null : !this$layer.equals(other$layer)) {
      return false;
    }
    final java.lang.Object this$component = this.getComponent();
    final java.lang.Object other$component = other.getComponent();
    if (this$component == null
        ? other$component != null
        : !this$component.equals(other$component)) {
      return false;
    }
    final java.lang.Object this$operation = this.getOperation();
    final java.lang.Object other$operation = other.getOperation();
    if (this$operation == null
        ? other$operation != null
        : !this$operation.equals(other$operation)) {
      return false;
    }
    final java.lang.Object this$origin = this.getOrigin();
    final java.lang.Object other$origin = other.getOrigin();
    if (this$origin == null ? other$origin != null : !this$origin.equals(other$origin)) {
      return false;
    }
    final java.lang.Object this$reason = this.getReason();
    final java.lang.Object other$reason = other.getReason();
    if (this$reason == null ? other$reason != null : !this$reason.equals(other$reason)) {
      return false;
    }
    final java.lang.Object this$upstreamSystem = this.getUpstreamSystem();
    final java.lang.Object other$upstreamSystem = other.getUpstreamSystem();
    if (this$upstreamSystem == null
        ? other$upstreamSystem != null
        : !this$upstreamSystem.equals(other$upstreamSystem)) {
      return false;
    }
    final java.lang.Object this$upstreamCode = this.getUpstreamCode();
    final java.lang.Object other$upstreamCode = other.getUpstreamCode();
    if (this$upstreamCode == null
        ? other$upstreamCode != null
        : !this$upstreamCode.equals(other$upstreamCode)) {
      return false;
    }
    final java.lang.Object this$causeChain = this.getCauseChain();
    final java.lang.Object other$causeChain = other.getCauseChain();
    if (this$causeChain == null
        ? other$causeChain != null
        : !this$causeChain.equals(other$causeChain)) {
      return false;
    }
    final java.lang.Object this$remediation = this.getRemediation();
    final java.lang.Object other$remediation = other.getRemediation();
    if (this$remediation == null
        ? other$remediation != null
        : !this$remediation.equals(other$remediation)) {
      return false;
    }
    final java.lang.Object this$runbook = this.getRunbook();
    final java.lang.Object other$runbook = other.getRunbook();
    if (this$runbook == null ? other$runbook != null : !this$runbook.equals(other$runbook)) {
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
    final java.lang.Object $upstreamStatus = this.getUpstreamStatus();
    result = result * PRIME + ($upstreamStatus == null ? 43 : $upstreamStatus.hashCode());
    final java.lang.Object $incidentId = this.getIncidentId();
    result = result * PRIME + ($incidentId == null ? 43 : $incidentId.hashCode());
    final java.lang.Object $code = this.getCode();
    result = result * PRIME + ($code == null ? 43 : $code.hashCode());
    final java.lang.Object $category = this.getCategory();
    result = result * PRIME + ($category == null ? 43 : $category.hashCode());
    final java.lang.Object $service = this.getService();
    result = result * PRIME + ($service == null ? 43 : $service.hashCode());
    final java.lang.Object $layer = this.getLayer();
    result = result * PRIME + ($layer == null ? 43 : $layer.hashCode());
    final java.lang.Object $component = this.getComponent();
    result = result * PRIME + ($component == null ? 43 : $component.hashCode());
    final java.lang.Object $operation = this.getOperation();
    result = result * PRIME + ($operation == null ? 43 : $operation.hashCode());
    final java.lang.Object $origin = this.getOrigin();
    result = result * PRIME + ($origin == null ? 43 : $origin.hashCode());
    final java.lang.Object $reason = this.getReason();
    result = result * PRIME + ($reason == null ? 43 : $reason.hashCode());
    final java.lang.Object $upstreamSystem = this.getUpstreamSystem();
    result = result * PRIME + ($upstreamSystem == null ? 43 : $upstreamSystem.hashCode());
    final java.lang.Object $upstreamCode = this.getUpstreamCode();
    result = result * PRIME + ($upstreamCode == null ? 43 : $upstreamCode.hashCode());
    final java.lang.Object $causeChain = this.getCauseChain();
    result = result * PRIME + ($causeChain == null ? 43 : $causeChain.hashCode());
    final java.lang.Object $remediation = this.getRemediation();
    result = result * PRIME + ($remediation == null ? 43 : $remediation.hashCode());
    final java.lang.Object $runbook = this.getRunbook();
    result = result * PRIME + ($runbook == null ? 43 : $runbook.hashCode());
    return result;
  }
}
