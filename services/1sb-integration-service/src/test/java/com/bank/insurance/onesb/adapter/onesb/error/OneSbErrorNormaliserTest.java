package com.bank.insurance.onesb.adapter.onesb.error;

import com.bank.insurance.onesb.TestErrors;

import com.bank.common.error.ErrorCatalogue;
import com.bank.common.error.ErrorCodes;
import com.bank.common.error.PlatformLayer;
import com.bank.common.error.ServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OneSbErrorNormaliserTest {

    private OneSbErrorNormaliser normaliser;

    @BeforeEach
    void setUp() {
        normaliser = new OneSbErrorNormaliser(TestErrors.ONESB);
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

    @Test
    void upstreamProseNeverReachesTheCallerButIsKeptForSupport() {
        String body = """
                {
                  "errors": [
                    {"field": "pan", "code": "ERR_PAN_INVALID", "message": "PAN format invalid"}
                  ]
                }
                """;

        ServiceException ex = normaliser.normalise(400, body);

        // Defect D1: this text used to be the response `detail`.
        assertThat(ex.getErrorResponse().getDetail())
                .as("the provider's own words must not be the bank caller's error message")
                .doesNotContain("PAN format invalid")
                .isEqualTo(ErrorCatalogue.require(ErrorCodes.UPSTREAM_BUSINESS_ERROR).publicDetail());

        // ...and is not lost: it moved to the half engineers and L1/L2 read.
        assertThat(ex.getDiagnostic().getReason()).isEqualTo("PAN format invalid");
        assertThat(ex.getDiagnostic().getUpstreamSystem()).isEqualTo("1SB");
        assertThat(ex.getDiagnostic().getUpstreamStatus()).isEqualTo(400);
        assertThat(ex.getDiagnostic().getService()).isEqualTo("onesb");
        assertThat(ex.getDiagnostic().getLayer()).isEqualTo(PlatformLayer.L5);
        assertThat(ex.getIncidentId()).isNotNull();
    }

    @Test
    void neitherTheProviderNameNorItsStatusAppearsInAnyDetail() {
        // Defect D2: "1SB returned 503" / "Unexpected 1SB status" were response details.
        for (ServiceException ex : java.util.List.of(
                normaliser.normalise(401, "{\"message\":\"unauthorized\"}"),
                normaliser.normalise(503, "gateway timeout"),
                normaliser.normalise(302, ""),
                normaliser.normalise(422, ""))) {

            assertThat(ex.getErrorResponse().getDetail())
                    .as("code %s", ex.getErrorResponse().getCode())
                    .doesNotContain("1SB")
                    .doesNotContainIgnoringCase("unauthorized")
                    .doesNotContain("503");
            assertThat(ex.getErrorResponse().getTitle()).doesNotContain("1SB");
            assertThat(ex.getDiagnostic().getReason())
                    .as("the operator still needs to know what the provider said")
                    .isNotBlank();
        }
    }

    @Test
    void the401CarriesTheCredentialRunbookForLevelOneSupport() {
        ServiceException ex = normaliser.normalise(401, "");

        assertThat(ex.getDiagnostic().getRunbook()).isEqualTo("RB-UPSTREAM_AUTH_FAILURE");
        assertThat(ex.getDiagnostic().getRemediation()).contains("IP whitelist");
    }
}
