package com.bank.common.error;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class ServiceErrorResponseTest {

    @Test
    void validationResponse_containsCorrectFields() {
        var errors = List.of(
            ServiceError.ofField("lob", ErrorCodes.VALIDATION_ERROR, "Unsupported LOB: DENTAL"),
            ServiceError.ofField("productCode", ErrorCodes.MISSING_REQUIRED_FIELD, "productCode is required")
        );

        var response = ServiceErrorResponse.validation("Request validation failed", errors);

        assertThat(response.getStatus()).isEqualTo(400);
        assertThat(response.getCode()).isEqualTo(ErrorCodes.VALIDATION_ERROR);
        assertThat(response.getTitle()).isEqualTo("Validation Failed");
        assertThat(response.isRetryable()).isFalse();
        assertThat(response.getErrors()).hasSize(2);
        assertThat(response.getUpstreamCode()).isNull();
        assertThat(response.getTimestamp()).isNotNull();
    }

    @Test
    void upstreamBusinessResponse_includesUpstreamCode() {
        var response = ServiceErrorResponse.upstreamBusiness("Insurer rejected proposal", "INSURER_ERR_422");

        assertThat(response.getStatus()).isEqualTo(422);
        assertThat(response.getCode()).isEqualTo(ErrorCodes.UPSTREAM_BUSINESS_ERROR);
        assertThat(response.getUpstreamCode()).isEqualTo("INSURER_ERR_422");
        assertThat(response.isRetryable()).isFalse();
    }

    @Test
    void upstreamUnavailableResponse_isRetryable() {
        var response = ServiceErrorResponse.upstreamUnavailable("1SB service is down");

        assertThat(response.getStatus()).isEqualTo(503);
        assertThat(response.getCode()).isEqualTo(ErrorCodes.UPSTREAM_UNAVAILABLE);
        assertThat(response.isRetryable()).isTrue();
    }

    @Test
    void builder_buildsFull_response() {
        var response = ServiceErrorResponse.builder()
            .type("https://bank.example.com/errors/upstream-auth")
            .title("Upstream Authentication Failure")
            .status(502)
            .detail("1SB returned 401 – invalid API key")
            .code(ErrorCodes.UPSTREAM_AUTH_FAILURE)
            .retryable(false)
            .upstreamCode("1SB_AUTH_FAIL")
            .build();

        assertThat(response.getType()).isEqualTo("https://bank.example.com/errors/upstream-auth");
        assertThat(response.getStatus()).isEqualTo(502);
        assertThat(response.getCode()).isEqualTo(ErrorCodes.UPSTREAM_AUTH_FAILURE);
        assertThat(response.getUpstreamCode()).isEqualTo("1SB_AUTH_FAIL");
    }

    @Test
    void errorsListIsImmutable() {
        var response = ServiceErrorResponse.validation("fail", List.of());
        assertThatThrownBy(() -> response.getErrors().add(ServiceError.of("CODE", "msg")))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void builder_usesExplicitTimestamp_andAddError() {
        Instant fixed = Instant.parse("2026-07-30T12:00:00Z");
        var response = ServiceErrorResponse.builder()
            .title("Conflict")
            .status(409)
            .detail("duplicate")
            .code(ErrorCodes.CONFLICT)
            .timestamp(fixed)
            .addError(ServiceError.of(ErrorCodes.CONFLICT, "already exists"))
            .build();

        assertThat(response.getTimestamp()).isEqualTo(fixed);
        assertThat(response.getErrors()).hasSize(1);
        assertThat(response.getErrors().getFirst().code()).isEqualTo(ErrorCodes.CONFLICT);
    }

    @Test
    void builder_rejectsNullTitleOrCode() {
        assertThatNullPointerException()
            .isThrownBy(() -> ServiceErrorResponse.builder()
                .code(ErrorCodes.INTERNAL_ERROR)
                .build())
            .withMessageContaining("title");

        assertThatNullPointerException()
            .isThrownBy(() -> ServiceErrorResponse.builder()
                .title("x")
                .build())
            .withMessageContaining("code");
    }
}
