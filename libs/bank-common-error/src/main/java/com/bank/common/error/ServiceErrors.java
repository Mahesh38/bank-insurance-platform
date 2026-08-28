package com.bank.common.error;

/**
 * A service's own error factory — knows who it is and where it sits, so throw sites do not.
 *
 * <p>Every throw site used to open with the same two calls:
 *
 * <pre>{@code
 * ServiceException.of(CODE).service("onesb").layer(PlatformLayer.L5)...
 * }</pre>
 *
 * repeated twenty-three times, with the service id as a literal in each. That is duplication with a
 * sharp edge: a rename becomes a thirty-one-site edit, and a single typo produces a
 * <strong>silently mis-attributed</strong> error — which defeats the purpose of recording the
 * service at all, and does so in a way no test would notice.
 *
 * <p>Identity now arrives by injection, from configuration:
 *
 * <pre>{@code
 * throw errors.error(ErrorCodes.SUITABILITY_REQUIRED)
 *         .component("QuoteService")
 *         .operation("createQuote")
 *         .reason("assessment " + id + " expired at " + expiry)
 *         .build();
 * }</pre>
 *
 * <p>What remains at the call site is exactly what the call site knows and the configuration
 * cannot: the component, the operation, the reason and the cause.
 */
public class ServiceErrors {

    private final ErrorHandlingSettings settings;

    public ServiceErrors(ErrorHandlingSettings settings) {
        this.settings = settings;
    }

    /**
     * A test double with a fixed identity, for unit tests that construct a service directly rather
     * than through Spring.
     */
    public static ServiceErrors of(String serviceId, PlatformLayer layer) {
        return new ServiceErrors(ErrorHandlingSettings.builder(serviceId).layer(layer).build());
    }

    /** An exception builder for {@code code}, already stamped with this service's identity. */
    public ServiceException.Builder error(String code) {
        return ServiceException.of(code)
            .service(settings.serviceId())
            .layer(settings.layer());
    }

    /** A diagnostic builder for {@code code}, for the few places that build one directly. */
    public ErrorDiagnostic.Builder diagnostic(String code) {
        return ErrorDiagnostic.builder(code)
            .service(settings.serviceId())
            .layer(settings.layer());
    }

    /** This service's registered id. */
    public String serviceId() {
        return settings.serviceId();
    }
}
