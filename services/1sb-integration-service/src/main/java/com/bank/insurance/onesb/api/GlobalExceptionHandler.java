package com.bank.insurance.onesb.api;

import com.bank.common.error.PlatformErrorHandler;
import com.bank.common.error.PlatformLayer;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * The 1SB integration service's error contract — the shared one, with two local facts declared.
 *
 * <p>Validation failures use HTTP 422 (FUNC-001 master-data, FUNC-002 quotes), which is why
 * {@link #validationStatus()} is overridden rather than inherited: the platform default and this
 * service's published contract genuinely differ, and the contract wins.
 *
 * <p>The boundary is {@code INTERNAL}: this service is reached by platform services, never by a
 * device. Its callers receive the diagnostic and redact in their turn at L4. The mapping and
 * behaviour previously written out here now live in {@link PlatformErrorHandler}.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends PlatformErrorHandler {

    public GlobalExceptionHandler() {
        super("onesb", PlatformLayer.L5, Boundary.INTERNAL);
    }

    @Override
    protected int validationStatus() {
        return 422;
    }
}
