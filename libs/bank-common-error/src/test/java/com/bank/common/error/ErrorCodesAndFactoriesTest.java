package com.bank.common.error;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke coverage for constant holders and remaining ServiceException factories (QA-001 / QA-007).
 */
class ErrorCodesAndFactoriesTest {

    @Test
    void errorCodes_areStableContractValues() {
        assertThat(ErrorCodes.VALIDATION_ERROR).isEqualTo("VALIDATION_ERROR");
        assertThat(ErrorCodes.MISSING_IDEMPOTENCY_KEY).isEqualTo("MISSING_IDEMPOTENCY_KEY");
        assertThat(ErrorCodes.UPSTREAM_TIMEOUT).isEqualTo("UPSTREAM_TIMEOUT");
        assertThat(ErrorCodes.IDEMPOTENCY_CONFLICT).isEqualTo("IDEMPOTENCY_CONFLICT");
        assertThat(ErrorCodes.INTERNAL_ERROR).isEqualTo("INTERNAL_ERROR");
    }

    @Test
    void serviceError_of_helpers() {
        ServiceError plain = ServiceError.of(ErrorCodes.CONFLICT, "already exists");
        assertThat(plain.code()).isEqualTo(ErrorCodes.CONFLICT);
        assertThat(plain.field()).isNull();

        ServiceError field = ServiceError.ofField(ErrorCodes.INVALID_REQUEST, "bad", "pan");
        assertThat(field.field()).isEqualTo("pan");
    }

    @Test
    void unauthorizedAndForbiddenFactories() {
        assertThat(ServiceException.unauthorized().getHttpStatus()).isEqualTo(401);
        assertThat(ServiceException.unauthorized().getErrorResponse().getCode())
                .isEqualTo(ErrorCodes.UNAUTHORIZED);

        assertThat(ServiceException.forbidden().getHttpStatus()).isEqualTo(403);
        assertThat(ServiceException.forbidden().getErrorResponse().getCode())
                .isEqualTo(ErrorCodes.FORBIDDEN);
    }

    @Test
    void internalFactory_preservesCause() {
        RuntimeException cause = new RuntimeException("boom");
        ServiceException ex = ServiceException.internal("unexpected", cause);

        assertThat(ex.getHttpStatus()).isEqualTo(500);
        assertThat(ex.getCause()).isSameAs(cause);
        assertThat(ex.getErrorResponse().getCode()).isEqualTo(ErrorCodes.INTERNAL_ERROR);
    }
}
