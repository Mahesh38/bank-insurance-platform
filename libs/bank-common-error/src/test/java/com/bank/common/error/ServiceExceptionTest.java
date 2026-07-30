package com.bank.common.error;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

class ServiceExceptionTest {

    @Test
    void validationException_wrapsResponseCorrectly() {
        var errors = List.of(ServiceError.ofField("pan", "INVALID_PAN", "PAN format invalid"));
        var ex = ServiceException.validation("Validation failed", errors);

        assertThat(ex.getHttpStatus()).isEqualTo(400);
        assertThat(ex.isRetryable()).isFalse();
        assertThat(ex.getErrorResponse().getErrors()).hasSize(1);
        assertThat(ex.getMessage()).isEqualTo("Validation failed");
    }

    @Test
    void upstreamAuthException_hasCorrectCode() {
        var ex = ServiceException.upstreamAuth("1SB returned 401");

        assertThat(ex.getHttpStatus()).isEqualTo(502);
        assertThat(ex.getErrorResponse().getCode()).isEqualTo(ErrorCodes.UPSTREAM_AUTH_FAILURE);
        assertThat(ex.isRetryable()).isFalse();
    }

    @Test
    void upstreamUnavailableException_isRetryable_andPreservesCause() {
        var cause = new RuntimeException("connection refused");
        var ex = ServiceException.upstreamUnavailable("1SB down", cause);

        assertThat(ex.isRetryable()).isTrue();
        assertThat(ex.getCause()).isSameAs(cause);
        assertThat(ex.getHttpStatus()).isEqualTo(503);
    }

    @Test
    void upstreamBusinessException_includesUpstreamCode() {
        var ex = ServiceException.upstreamBusiness("Insurer rejected", "ERR_INSURER_500");

        assertThat(ex.getErrorResponse().getUpstreamCode()).isEqualTo("ERR_INSURER_500");
        assertThat(ex.getHttpStatus()).isEqualTo(422);
    }
}
