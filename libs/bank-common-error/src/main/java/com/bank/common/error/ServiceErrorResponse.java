package com.bank.common.error;

import java.time.Instant;
import java.util.List;

/**
 * RFC 7807 "Problem Details for HTTP APIs" response envelope, extended with bank-platform fields
 * ({@code code}, {@code retryable}, {@code upstreamCode}).
 *
 * <p>Intended to be serialised as the HTTP response body for all error responses. Controllers must
 * not return raw upstream error JSON.
 *
 * <h2>Two renderings</h2>
 *
 * This type carries both halves of a failure. Below the redaction boundary — service to service —
 * it may carry {@link #getOrigin() origin} and {@link #getDiagnostic() diagnostic}, so a calling
 * service can see which service actually failed. At the boundary, {@link #toPublic()} strips them
 * and replaces the wording with the catalogue's safe text.
 *
 * <p><strong>The BFF (L4) is that boundary</strong> — the last hop that may hold a diagnostic and
 * the first that must never emit one ({@code docs/journey-execution/07-PLATFORM-ERROR-CONTRACT.md
 * §4.4}).
 *
 * <p>Prefer {@link #of(String)} over the raw builder: it takes status, title, detail, retryability
 * and category from {@link ErrorCatalogue}, so the same code cannot be worded two ways in two
 * services.
 */
public final class ServiceErrorResponse {
  private final String type;

  private final String title;
  private final int status;
  private final String detail;

  private final String code;
  private final boolean retryable;
  private final String upstreamCode;

  /** Stamped at build time when the caller does not supply one. */
  private final Instant timestamp;

  /**
   * Field-level errors.
   *
   * <p>{@code @Singular} gives the accumulating semantics this envelope needs — {@code addError}
   * for one, {@code errors} for many, {@code clearErrors} to reset — and an immutable list at build
   * time, which is what the hand-written builder was doing by hand.
   */
  private final List<ServiceError> errors;

  // --- Added by ERR-001. Additive: existing fields and their values are unchanged. ---
  /** How the caller must treat this failure. Null only on responses built before the catalogue. */
  private final ErrorCategory category;

  /** The service that produced this response. */
  private final String service;

  /** Names this failure across every hop and every log line. Safe to show an end user. */
  private final String incidentId;

  /** Groups every hop of the request this failure belongs to. */
  private final String correlationId;

  /** Where the failure first occurred. <strong>Diagnostic — stripped at the boundary.</strong> */
  private final ErrorOrigin origin;

  /** The engineer-facing half. <strong>Diagnostic — stripped at the boundary.</strong> */
  private final ErrorDiagnostic diagnostic;

  // --- Catalogue-driven construction ---
  /**
   * A response for {@code code}, with status, title, detail, retryability and category taken from
   * {@link ErrorCatalogue}, and a fresh incident id.
   *
   * @throws IllegalArgumentException if the code is not registered
   */
  public static ServiceErrorResponseBuilder of(String code) {
    ErrorDefinition d = ErrorCatalogue.require(code);
    return builder()
        .title(d.publicTitle())
        .status(d.httpStatus())
        .detail(d.publicDetail())
        .code(d.code())
        .category(d.category())
        .retryable(d.retryability().toBoolean())
        .incidentId(IncidentId.generate());
  }

  /**
   * The rendering safe to send across a trust boundary.
   *
   * <p>Drops {@code origin} and {@code diagnostic}, and — when the code is registered — replaces
   * {@code title} and {@code detail} with the catalogue's fixed text, so no upstream body, internal
   * route or vendor name can survive in the wording. {@code incidentId} is kept deliberately: it is
   * what lets support find the diagnostic that was <em>not</em> sent.
   */
  public ServiceErrorResponse toPublic() {
    ErrorDefinition d = ErrorCatalogue.find(code).orElse(null);
    return toBuilder()
        .title(d != null ? d.publicTitle() : title)
        .detail(d != null ? d.publicDetail() : detail)
        .origin(null)
        .diagnostic(null)
        .build();
  }

  /** True when nothing on this response may cross a trust boundary unchanged. */
  public boolean carriesDiagnostics() {
    return origin != null || diagnostic != null;
  }

  /**
   * A copy stamped with the service that is answering, when it does not already name one.
   *
   * <p>Set by the shared handler rather than at a throw site, so no call site can forget it and no
   * call site can claim to be a service it is not.
   */
  public ServiceErrorResponse withService(String serviceId) {
    return serviceId == null || service != null ? this : copy(b -> b.service(serviceId));
  }

  /** A copy carrying {@code incidentId}, when it does not already have one. */
  public ServiceErrorResponse withIncidentId(String id) {
    return id == null || incidentId != null ? this : copy(b -> b.incidentId(id));
  }

  /** A copy carrying {@code correlationId}, when it does not already have one. */
  public ServiceErrorResponse withCorrelationId(String id) {
    return id == null || correlationId != null ? this : copy(b -> b.correlationId(id));
  }

  /**
   * The single copy path.
   *
   * <p>Every {@code withX} used to re-list all fifteen constructor arguments. Adding a field then
   * meant editing five arg lists, and forgetting one silently dropped that field from a copied
   * response — a defect that shows up as a missing incident id in production, not as a compile
   * error.
   */
  private ServiceErrorResponse copy(
      java.util.function.UnaryOperator<ServiceErrorResponseBuilder> change) {
    return change.apply(toBuilder()).build();
  }

  /**
   * Only the one builder method that carries behaviour.
   *
   * <p>Everything else — defaults, the accumulating {@code errors} list, the null checks on {@code
   * title} and {@code code}, and {@code build()} itself — is generated. The hand-written versions
   * re-listed all fifteen constructor arguments, which is the duplication that made adding a field
   * a five-site edit.
   */
  public static class ServiceErrorResponseBuilder {
    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    private boolean type$set;

    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    private String type$value;

    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    private String title;

    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    private int status;

    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    private String detail;

    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    private String code;

    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    private boolean retryable;

    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    private String upstreamCode;

    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    private boolean timestamp$set;

    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    private Instant timestamp$value;

    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    private java.util.ArrayList<ServiceError> errors;

    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    private ErrorCategory category;

    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    private String service;

    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    private String incidentId;

    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    private String correlationId;

    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    private ErrorOrigin origin;

    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    private ErrorDiagnostic diagnostic;

    /**
     * Attaches the diagnostic and adopts its incident id, origin and service, so the two halves of
     * one failure cannot drift apart.
     */
    public ServiceErrorResponseBuilder diagnostic(ErrorDiagnostic d) {
      this.diagnostic = d;
      if (d != null) {
        if (this.incidentId == null) {
          this.incidentId = d.getIncidentId();
        }
        if (this.origin == null) {
          this.origin = d.getOrigin();
        }
        if (this.service == null) {
          this.service = d.getService();
        }
      }
      return this;
    }

    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    ServiceErrorResponseBuilder() {}

    /**
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    public ServiceErrorResponse.ServiceErrorResponseBuilder type(final String type) {
      this.type$value = type;
      type$set = true;
      return this;
    }

    /**
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    public ServiceErrorResponse.ServiceErrorResponseBuilder title(final String title) {
      if (title == null) {
        throw new java.lang.NullPointerException("title is marked non-null but is null");
      }
      this.title = title;
      return this;
    }

    /**
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    public ServiceErrorResponse.ServiceErrorResponseBuilder status(final int status) {
      this.status = status;
      return this;
    }

    /**
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    public ServiceErrorResponse.ServiceErrorResponseBuilder detail(final String detail) {
      this.detail = detail;
      return this;
    }

    /**
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    public ServiceErrorResponse.ServiceErrorResponseBuilder code(final String code) {
      if (code == null) {
        throw new java.lang.NullPointerException("code is marked non-null but is null");
      }
      this.code = code;
      return this;
    }

    /**
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    public ServiceErrorResponse.ServiceErrorResponseBuilder retryable(final boolean retryable) {
      this.retryable = retryable;
      return this;
    }

    /**
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    public ServiceErrorResponse.ServiceErrorResponseBuilder upstreamCode(
        final String upstreamCode) {
      this.upstreamCode = upstreamCode;
      return this;
    }

    /**
     * Stamped at build time when the caller does not supply one.
     *
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    public ServiceErrorResponse.ServiceErrorResponseBuilder timestamp(final Instant timestamp) {
      this.timestamp$value = timestamp;
      timestamp$set = true;
      return this;
    }

    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    public ServiceErrorResponse.ServiceErrorResponseBuilder addError(final ServiceError addError) {
      if (this.errors == null) {
        this.errors = new java.util.ArrayList<ServiceError>();
      }
      this.errors.add(addError);
      return this;
    }

    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    public ServiceErrorResponse.ServiceErrorResponseBuilder errors(
        final java.util.Collection<? extends ServiceError> errors) {
      if (errors == null) {
        throw new java.lang.NullPointerException("errors cannot be null");
      }
      if (this.errors == null) {
        this.errors = new java.util.ArrayList<ServiceError>();
      }
      this.errors.addAll(errors);
      return this;
    }

    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    public ServiceErrorResponse.ServiceErrorResponseBuilder clearErrors() {
      if (this.errors != null) {
        this.errors.clear();
      }
      return this;
    }

    /**
     * How the caller must treat this failure. Null only on responses built before the catalogue.
     *
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    public ServiceErrorResponse.ServiceErrorResponseBuilder category(final ErrorCategory category) {
      this.category = category;
      return this;
    }

    /**
     * The service that produced this response.
     *
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    public ServiceErrorResponse.ServiceErrorResponseBuilder service(final String service) {
      this.service = service;
      return this;
    }

    /**
     * Names this failure across every hop and every log line. Safe to show an end user.
     *
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    public ServiceErrorResponse.ServiceErrorResponseBuilder incidentId(final String incidentId) {
      this.incidentId = incidentId;
      return this;
    }

    /**
     * Groups every hop of the request this failure belongs to.
     *
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    public ServiceErrorResponse.ServiceErrorResponseBuilder correlationId(
        final String correlationId) {
      this.correlationId = correlationId;
      return this;
    }

    /**
     * Where the failure first occurred. <strong>Diagnostic — stripped at the boundary.</strong>
     *
     * @return {@code this}.
     */
    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    public ServiceErrorResponse.ServiceErrorResponseBuilder origin(final ErrorOrigin origin) {
      this.origin = origin;
      return this;
    }

    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    public ServiceErrorResponse build() {
      java.util.List<ServiceError> errors;
      switch (this.errors == null ? 0 : this.errors.size()) {
        case 0:
          errors = java.util.Collections.emptyList();
          break;
        case 1:
          errors = java.util.Collections.singletonList(this.errors.get(0));
          break;
        default:
          errors =
              java.util.Collections.unmodifiableList(
                  new java.util.ArrayList<ServiceError>(this.errors));
      }
      String type$value = this.type$value;
      if (!this.type$set) {
        type$value = ServiceErrorResponse.$default$type();
      }
      Instant timestamp$value = this.timestamp$value;
      if (!this.timestamp$set) {
        timestamp$value = ServiceErrorResponse.$default$timestamp();
      }
      return new ServiceErrorResponse(
          type$value,
          this.title,
          this.status,
          this.detail,
          this.code,
          this.retryable,
          this.upstreamCode,
          timestamp$value,
          errors,
          this.category,
          this.service,
          this.incidentId,
          this.correlationId,
          this.origin,
          this.diagnostic);
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("all")
    @MechanicallyGenerated
    public java.lang.String toString() {
      return "ServiceErrorResponse.ServiceErrorResponseBuilder(type$value="
          + this.type$value
          + ", title="
          + this.title
          + ", status="
          + this.status
          + ", detail="
          + this.detail
          + ", code="
          + this.code
          + ", retryable="
          + this.retryable
          + ", upstreamCode="
          + this.upstreamCode
          + ", timestamp$value="
          + this.timestamp$value
          + ", errors="
          + this.errors
          + ", category="
          + this.category
          + ", service="
          + this.service
          + ", incidentId="
          + this.incidentId
          + ", correlationId="
          + this.correlationId
          + ", origin="
          + this.origin
          + ", diagnostic="
          + this.diagnostic
          + ")";
    }
  }

  @java.lang.SuppressWarnings("all")
  @MechanicallyGenerated
  private static String $default$type() {
    return "about:blank";
  }

  @java.lang.SuppressWarnings("all")
  @MechanicallyGenerated
  private static Instant $default$timestamp() {
    return Instant.now();
  }

  /**
   * Creates a new {@code ServiceErrorResponse} instance.
   *
   * @param type
   * @param title
   * @param status
   * @param detail
   * @param code
   * @param retryable
   * @param upstreamCode
   * @param timestamp Stamped at build time when the caller does not supply one.
   * @param errors Field-level errors.
   *     <p>{@code @Singular} gives the accumulating semantics this envelope needs — {@code
   *     addError} for one, {@code errors} for many, {@code clearErrors} to reset — and an immutable
   *     list at build time, which is what the hand-written builder was doing by hand.
   * @param category How the caller must treat this failure. Null only on responses built before the
   *     catalogue.
   * @param service The service that produced this response.
   * @param incidentId Names this failure across every hop and every log line. Safe to show an end
   *     user.
   * @param correlationId Groups every hop of the request this failure belongs to.
   * @param origin Where the failure first occurred. <strong>Diagnostic — stripped at the
   *     boundary.</strong>
   * @param diagnostic The engineer-facing half. <strong>Diagnostic — stripped at the
   *     boundary.</strong>
   */
  @java.lang.SuppressWarnings("all")
  @MechanicallyGenerated
  ServiceErrorResponse(
      final String type,
      final String title,
      final int status,
      final String detail,
      final String code,
      final boolean retryable,
      final String upstreamCode,
      final Instant timestamp,
      final List<ServiceError> errors,
      final ErrorCategory category,
      final String service,
      final String incidentId,
      final String correlationId,
      final ErrorOrigin origin,
      final ErrorDiagnostic diagnostic) {
    if (title == null) {
      throw new java.lang.NullPointerException("title is marked non-null but is null");
    }
    if (code == null) {
      throw new java.lang.NullPointerException("code is marked non-null but is null");
    }
    this.type = type;
    this.title = title;
    this.status = status;
    this.detail = detail;
    this.code = code;
    this.retryable = retryable;
    this.upstreamCode = upstreamCode;
    this.timestamp = timestamp;
    this.errors = errors;
    this.category = category;
    this.service = service;
    this.incidentId = incidentId;
    this.correlationId = correlationId;
    this.origin = origin;
    this.diagnostic = diagnostic;
  }

  @java.lang.SuppressWarnings("all")
  @MechanicallyGenerated
  public static ServiceErrorResponse.ServiceErrorResponseBuilder builder() {
    return new ServiceErrorResponse.ServiceErrorResponseBuilder();
  }

  @java.lang.SuppressWarnings("all")
  @MechanicallyGenerated
  public ServiceErrorResponse.ServiceErrorResponseBuilder toBuilder() {
    final ServiceErrorResponse.ServiceErrorResponseBuilder builder =
        new ServiceErrorResponse.ServiceErrorResponseBuilder()
            .type(this.type)
            .title(this.title)
            .status(this.status)
            .detail(this.detail)
            .code(this.code)
            .retryable(this.retryable)
            .upstreamCode(this.upstreamCode)
            .timestamp(this.timestamp)
            .category(this.category)
            .service(this.service)
            .incidentId(this.incidentId)
            .correlationId(this.correlationId)
            .origin(this.origin)
            .diagnostic(this.diagnostic);
    if (this.errors != null) {
      builder.errors(this.errors);
    }
    return builder;
  }

  @java.lang.SuppressWarnings("all")
  @MechanicallyGenerated
  public String getType() {
    return this.type;
  }

  @java.lang.SuppressWarnings("all")
  @MechanicallyGenerated
  public String getTitle() {
    return this.title;
  }

  @java.lang.SuppressWarnings("all")
  @MechanicallyGenerated
  public int getStatus() {
    return this.status;
  }

  @java.lang.SuppressWarnings("all")
  @MechanicallyGenerated
  public String getDetail() {
    return this.detail;
  }

  @java.lang.SuppressWarnings("all")
  @MechanicallyGenerated
  public String getCode() {
    return this.code;
  }

  @java.lang.SuppressWarnings("all")
  @MechanicallyGenerated
  public boolean isRetryable() {
    return this.retryable;
  }

  @java.lang.SuppressWarnings("all")
  @MechanicallyGenerated
  public String getUpstreamCode() {
    return this.upstreamCode;
  }

  /** Stamped at build time when the caller does not supply one. */
  @java.lang.SuppressWarnings("all")
  @MechanicallyGenerated
  public Instant getTimestamp() {
    return this.timestamp;
  }

  /**
   * Field-level errors.
   *
   * <p>{@code @Singular} gives the accumulating semantics this envelope needs — {@code addError}
   * for one, {@code errors} for many, {@code clearErrors} to reset — and an immutable list at build
   * time, which is what the hand-written builder was doing by hand.
   */
  @java.lang.SuppressWarnings("all")
  @MechanicallyGenerated
  public List<ServiceError> getErrors() {
    return this.errors;
  }

  /** How the caller must treat this failure. Null only on responses built before the catalogue. */
  @java.lang.SuppressWarnings("all")
  @MechanicallyGenerated
  public ErrorCategory getCategory() {
    return this.category;
  }

  /** The service that produced this response. */
  @java.lang.SuppressWarnings("all")
  @MechanicallyGenerated
  public String getService() {
    return this.service;
  }

  /** Names this failure across every hop and every log line. Safe to show an end user. */
  @java.lang.SuppressWarnings("all")
  @MechanicallyGenerated
  public String getIncidentId() {
    return this.incidentId;
  }

  /** Groups every hop of the request this failure belongs to. */
  @java.lang.SuppressWarnings("all")
  @MechanicallyGenerated
  public String getCorrelationId() {
    return this.correlationId;
  }

  /** Where the failure first occurred. <strong>Diagnostic — stripped at the boundary.</strong> */
  @java.lang.SuppressWarnings("all")
  @MechanicallyGenerated
  public ErrorOrigin getOrigin() {
    return this.origin;
  }

  /** The engineer-facing half. <strong>Diagnostic — stripped at the boundary.</strong> */
  @java.lang.SuppressWarnings("all")
  @MechanicallyGenerated
  public ErrorDiagnostic getDiagnostic() {
    return this.diagnostic;
  }

  @java.lang.Override
  @java.lang.SuppressWarnings("all")
  @MechanicallyGenerated
  public boolean equals(final java.lang.Object o) {
    if (o == this) {
      return true;
    }
    if (!(o instanceof ServiceErrorResponse)) {
      return false;
    }
    final ServiceErrorResponse other = (ServiceErrorResponse) o;
    if (this.getStatus() != other.getStatus()) {
      return false;
    }
    if (this.isRetryable() != other.isRetryable()) {
      return false;
    }
    final java.lang.Object this$type = this.getType();
    final java.lang.Object other$type = other.getType();
    if (this$type == null ? other$type != null : !this$type.equals(other$type)) {
      return false;
    }
    final java.lang.Object this$title = this.getTitle();
    final java.lang.Object other$title = other.getTitle();
    if (this$title == null ? other$title != null : !this$title.equals(other$title)) {
      return false;
    }
    final java.lang.Object this$detail = this.getDetail();
    final java.lang.Object other$detail = other.getDetail();
    if (this$detail == null ? other$detail != null : !this$detail.equals(other$detail)) {
      return false;
    }
    final java.lang.Object this$code = this.getCode();
    final java.lang.Object other$code = other.getCode();
    if (this$code == null ? other$code != null : !this$code.equals(other$code)) {
      return false;
    }
    final java.lang.Object this$upstreamCode = this.getUpstreamCode();
    final java.lang.Object other$upstreamCode = other.getUpstreamCode();
    if (this$upstreamCode == null
        ? other$upstreamCode != null
        : !this$upstreamCode.equals(other$upstreamCode)) {
      return false;
    }
    final java.lang.Object this$timestamp = this.getTimestamp();
    final java.lang.Object other$timestamp = other.getTimestamp();
    if (this$timestamp == null
        ? other$timestamp != null
        : !this$timestamp.equals(other$timestamp)) {
      return false;
    }
    final java.lang.Object this$errors = this.getErrors();
    final java.lang.Object other$errors = other.getErrors();
    if (this$errors == null ? other$errors != null : !this$errors.equals(other$errors)) {
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
    final java.lang.Object this$incidentId = this.getIncidentId();
    final java.lang.Object other$incidentId = other.getIncidentId();
    if (this$incidentId == null
        ? other$incidentId != null
        : !this$incidentId.equals(other$incidentId)) {
      return false;
    }
    final java.lang.Object this$correlationId = this.getCorrelationId();
    final java.lang.Object other$correlationId = other.getCorrelationId();
    if (this$correlationId == null
        ? other$correlationId != null
        : !this$correlationId.equals(other$correlationId)) {
      return false;
    }
    final java.lang.Object this$origin = this.getOrigin();
    final java.lang.Object other$origin = other.getOrigin();
    if (this$origin == null ? other$origin != null : !this$origin.equals(other$origin)) {
      return false;
    }
    final java.lang.Object this$diagnostic = this.getDiagnostic();
    final java.lang.Object other$diagnostic = other.getDiagnostic();
    if (this$diagnostic == null
        ? other$diagnostic != null
        : !this$diagnostic.equals(other$diagnostic)) {
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
    result = result * PRIME + this.getStatus();
    result = result * PRIME + (this.isRetryable() ? 79 : 97);
    final java.lang.Object $type = this.getType();
    result = result * PRIME + ($type == null ? 43 : $type.hashCode());
    final java.lang.Object $title = this.getTitle();
    result = result * PRIME + ($title == null ? 43 : $title.hashCode());
    final java.lang.Object $detail = this.getDetail();
    result = result * PRIME + ($detail == null ? 43 : $detail.hashCode());
    final java.lang.Object $code = this.getCode();
    result = result * PRIME + ($code == null ? 43 : $code.hashCode());
    final java.lang.Object $upstreamCode = this.getUpstreamCode();
    result = result * PRIME + ($upstreamCode == null ? 43 : $upstreamCode.hashCode());
    final java.lang.Object $timestamp = this.getTimestamp();
    result = result * PRIME + ($timestamp == null ? 43 : $timestamp.hashCode());
    final java.lang.Object $errors = this.getErrors();
    result = result * PRIME + ($errors == null ? 43 : $errors.hashCode());
    final java.lang.Object $category = this.getCategory();
    result = result * PRIME + ($category == null ? 43 : $category.hashCode());
    final java.lang.Object $service = this.getService();
    result = result * PRIME + ($service == null ? 43 : $service.hashCode());
    final java.lang.Object $incidentId = this.getIncidentId();
    result = result * PRIME + ($incidentId == null ? 43 : $incidentId.hashCode());
    final java.lang.Object $correlationId = this.getCorrelationId();
    result = result * PRIME + ($correlationId == null ? 43 : $correlationId.hashCode());
    final java.lang.Object $origin = this.getOrigin();
    result = result * PRIME + ($origin == null ? 43 : $origin.hashCode());
    final java.lang.Object $diagnostic = this.getDiagnostic();
    result = result * PRIME + ($diagnostic == null ? 43 : $diagnostic.hashCode());
    return result;
  }

  @java.lang.Override
  @java.lang.SuppressWarnings("all")
  @MechanicallyGenerated
  public java.lang.String toString() {
    return "ServiceErrorResponse(status="
        + this.getStatus()
        + ", detail="
        + this.getDetail()
        + ", code="
        + this.getCode()
        + ", service="
        + this.getService()
        + ", incidentId="
        + this.getIncidentId()
        + ")";
  }
}
