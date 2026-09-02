package com.bank.common.error;

import java.util.Objects;

/**
 * Where a failure <em>first</em> occurred, when that is not the service answering the caller.
 *
 * <p>This is the field that answers "which service actually failed" when a request has crossed
 * several hops. In the journey BFF → consent → orchestrator, an orchestrator refusal reaches the
 * BFF carrying {@code service=journey-orchestration}, its original {@code code}, and the
 * {@link PlatformLayer} it fired at.
 *
 * <p><strong>First origin wins.</strong> When a service receives an error that already carries an
 * origin, it forwards that origin unchanged rather than overwriting it with the immediate caller —
 * otherwise every hop rewrites history and the true source is lost at the second hop.
 *
 * <p>Diagnostic-only. Never serialised past the redaction boundary
 * ({@code 07-PLATFORM-ERROR-CONTRACT.md §4.4}).
 */
public record ErrorOrigin(String service, String code, PlatformLayer layer) {

    public ErrorOrigin {
        Objects.requireNonNull(service, "service must not be null");
        Objects.requireNonNull(code, "code must not be null");
    }

    public static ErrorOrigin of(String service, String code, PlatformLayer layer) {
        return new ErrorOrigin(service, code, layer);
    }

    /**
     * The origin to carry forward: {@code existing} when the failure already had one, otherwise
     * a new origin naming the service that produced it.
     *
     * <p>Implements the "first origin wins" rule above.
     */
    public static ErrorOrigin inherit(ErrorOrigin existing, String service, String code, PlatformLayer layer) {
        return existing != null ? existing : new ErrorOrigin(service, code, layer);
    }
}
