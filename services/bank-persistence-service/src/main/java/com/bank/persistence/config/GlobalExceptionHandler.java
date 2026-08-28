package com.bank.persistence.config;

import com.bank.common.error.PlatformErrorHandler;
import com.bank.common.observability.ErrorMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.ObjectProvider;
import com.bank.common.error.PlatformLayer;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * The persistence service's error contract.
 *
 * <p>Validation failures answer 400 here, not the platform default of 422. That difference is
 * published on {@code /internal/v1} and asserted by its API tests, so it is declared rather than
 * unified — changing it would be a silent breaking change to a live contract.
 *
 * <p>The boundary is {@code INTERNAL}: this service is only ever called by other platform
 * services, and it owns the store (L7).
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends PlatformErrorHandler {

    public GlobalExceptionHandler(ObjectProvider<MeterRegistry> meterRegistry) {
        super("persistence", PlatformLayer.L7, Boundary.INTERNAL, errorMetrics(meterRegistry));
    }

    @Override
    protected int validationStatus() {
        return 400;
    }
}
