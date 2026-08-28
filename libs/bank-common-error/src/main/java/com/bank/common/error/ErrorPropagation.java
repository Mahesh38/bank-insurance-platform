package com.bank.common.error;

/**
 * Turns another service's failure into this service's failure without losing who actually failed —
 * work item {@code ERR-003}.
 *
 * <p>Two things must survive every hop, and both are lost by the obvious implementation:
 *
 * <ol>
 *   <li>the <strong>incident id</strong>, so one failure keeps one identity end to end. Minting a
 *       fresh id per hop gives support three ids for one event and no way to join them;</li>
 *   <li>the <strong>first origin</strong>, so the RM's screen can be traced back to the service
 *       that actually refused rather than to the last one in the chain.</li>
 * </ol>
 *
 * <p>The third rule is {@link ErrorDefinition#propagation()}: a refusal the caller can act on keeps
 * its own code, and a dependency failure is wrapped. Getting this backwards is how a compliance
 * gate the RM could have cleared arrives as an unactionable 502 — and how a dependency outage
 * arrives as a validation error the RM tries to fix by editing the form.
 *
 * <pre>{@code
 * catch (RestClientResponseException ex) {
 *     throw ErrorPropagation.from(parseProblemJson(ex))
 *             .receivedBy("bff", PlatformLayer.L4)
 *             .calling("journey-orchestration", "createQuote")
 *             .toException();
 * }
 * }</pre>
 */
public final class ErrorPropagation {

    private final ServiceErrorResponse upstream;
    private String service;
    private PlatformLayer layer;
    private String calledService;
    private String operation;
    private Throwable cause;

    private ErrorPropagation(ServiceErrorResponse upstream) {
        this.upstream = upstream;
    }

    /** Starts from the error another service returned. */
    public static ErrorPropagation from(ServiceErrorResponse upstream) {
        if (upstream == null) {
            throw new IllegalArgumentException("upstream response must not be null");
        }
        return new ErrorPropagation(upstream);
    }

    /** Starts from an upstream {@link ServiceException}. */
    public static ErrorPropagation from(ServiceException upstream) {
        return from(upstream.getErrorResponse()).causedBy(upstream);
    }

    /** The service doing the receiving, and where it sits. */
    public ErrorPropagation receivedBy(String service, PlatformLayer layer) {
        this.service = service;
        this.layer = layer;
        return this;
    }

    /** The service that was called and the operation attempted. */
    public ErrorPropagation calling(String calledService, String operation) {
        this.calledService = calledService;
        this.operation = operation;
        return this;
    }

    public ErrorPropagation causedBy(Throwable cause) {
        this.cause = cause;
        return this;
    }

    /**
     * The code this service will emit: the upstream's own when the catalogue marks it
     * {@link Propagation#PROPAGATE}, otherwise a wrapping {@code UPSTREAM_*} code.
     */
    public String resolvedCode() {
        ErrorDefinition definition = ErrorCatalogue.find(upstream.getCode()).orElse(null);
        if (definition == null) {
            // An unregistered upstream code cannot be re-emitted as our own: we would be
            // publishing a code with no declared status, wording or retryability.
            return ErrorCodes.UPSTREAM_BAD_RESPONSE;
        }
        if (definition.propagation() == Propagation.PROPAGATE) {
            return definition.code();
        }
        return switch (definition.category()) {
            case UPSTREAM, CONFIG, INTERNAL -> ErrorCodes.UPSTREAM_UNAVAILABLE;
            default -> ErrorCodes.UPSTREAM_BUSINESS_ERROR;
        };
    }

    /** True when the upstream code reaches our caller unchanged. */
    public boolean propagatesCode() {
        return resolvedCode().equals(upstream.getCode());
    }

    /**
     * The origin to carry forward — the upstream's own if it had one, otherwise the service we
     * called. First origin wins, so the true source survives an arbitrary number of hops.
     */
    public ErrorOrigin resolvedOrigin() {
        return ErrorOrigin.inherit(
            upstream.getOrigin(),
            calledService != null ? calledService : upstream.getService(),
            upstream.getCode(),
            upstream.getDiagnostic() != null ? upstream.getDiagnostic().getLayer() : null);
    }

    /** Builds this service's exception, carrying the upstream's incident id and origin. */
    public ServiceException toException() {
        String code = resolvedCode();
        ErrorOrigin origin = resolvedOrigin();

        ServiceException.Builder builder = ServiceException.of(code)
            .service(service)
            .layer(layer)
            .operation(operation)
            .origin(origin)
            .incidentId(upstream.getIncidentId())
            .correlationId(upstream.getCorrelationId())
            .reason(reason(origin))
            .upstream(origin.service(), upstream.getUpstreamCode(), upstream.getStatus());

        if (cause != null) {
            builder.cause(cause);
        }
        return builder.build();
    }

    private String reason(ErrorOrigin origin) {
        String upstreamReason = upstream.getDiagnostic() != null
            ? upstream.getDiagnostic().getReason()
            : null;

        String base = origin.service() + " answered " + upstream.getCode()
            + " (" + upstream.getStatus() + ")";
        return upstreamReason != null && !upstreamReason.isBlank()
            ? base + ": " + upstreamReason
            : base;
    }
}
