package com.bank.insurance.onesb.adapter.onesb.error;

import com.bank.common.error.ErrorCodes;
import com.bank.common.error.ServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OneSbErrorNormaliserTest {

    private OneSbErrorNormaliser normaliser;

    @BeforeEach
    void setUp() {
        normaliser = new OneSbErrorNormaliser();
    }

    @Test
    void status401_mapsToUpstreamAuthFailure_notRetryable() {
        ServiceException ex = normaliser.normalise(401, "{\"message\":\"unauthorized\"}");

        assertThat(ex.getErrorResponse().getCode()).isEqualTo(ErrorCodes.UPSTREAM_AUTH_FAILURE);
        assertThat(ex.isRetryable()).isFalse();
        assertThat(ex.getHttpStatus()).isEqualTo(502);
    }

    @Test
    void business4xx_withErrorsArray_mapsToBankServiceErrors() {
        String body = """
                {
                  "errors": [
                    {"field": "pan", "code": "ERR_PAN_INVALID", "message": "PAN format invalid"},
                    {"field": "mobile", "code": "ERR_MOBILE", "message": "Mobile required"}
                  ]
                }
                """;

        ServiceException ex = normaliser.normalise(400, body);

        assertThat(ex.getErrorResponse().getCode()).isEqualTo(ErrorCodes.UPSTREAM_BUSINESS_ERROR);
        assertThat(ex.isRetryable()).isFalse();
        assertThat(ex.getErrorResponse().getUpstreamCode()).isEqualTo("ERR_PAN_INVALID");
        assertThat(ex.getErrorResponse().getErrors()).hasSize(2);
        assertThat(ex.getErrorResponse().getErrors().get(0).code())
                .isEqualTo(ErrorCodes.UPSTREAM_BUSINESS_ERROR);
        assertThat(ex.getErrorResponse().getErrors().get(0).field()).isEqualTo("pan");
        assertThat(ex.getErrorResponse().getErrors().get(0).message()).isEqualTo("PAN format invalid");
        // Raw 1SB code must not be the sole/top-level response code
        assertThat(ex.getErrorResponse().getCode()).isNotEqualTo("ERR_PAN_INVALID");
    }

    @Test
    void status5xx_mapsToUpstreamUnavailable_retryable() {
        ServiceException ex = normaliser.normalise(503, "gateway timeout");

        assertThat(ex.getErrorResponse().getCode()).isEqualTo(ErrorCodes.UPSTREAM_UNAVAILABLE);
        assertThat(ex.isRetryable()).isTrue();
        assertThat(ex.getHttpStatus()).isEqualTo(503);
    }

    @Test
    void blankBody4xx_stillProducesBusinessError() {
        ServiceException ex = normaliser.normalise(422, "");

        assertThat(ex.getErrorResponse().getCode()).isEqualTo(ErrorCodes.UPSTREAM_BUSINESS_ERROR);
        assertThat(ex.isRetryable()).isFalse();
    }
}
