package com.bank.common.error;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * ERR-003 — a failure keeps one identity and one true origin across the whole chain.
 *
 * <p>The scenario is the one the requirement names: the request lands on the BFF, moves on, and
 * the orchestrator refuses. What must be true at the BFF is that the refusal still says
 * <em>journey-orchestration</em>.
 */
class ErrorPropagationTest {

    private ServiceErrorResponse orchestratorRefused(String code) {
        return ServiceException.of(code)
            .service("journey-orchestration")
            .layer(PlatformLayer.L6)
            .component("QuoteService")
            .correlationId("corr-77")
            .reason("assessment a3f2 expired at 2026-08-27T08:10Z")
            .build()
            .getErrorResponse();
    }

    @Test
    void oneFailureKeepsOneIncidentIdAcrossHops() {
        ServiceErrorResponse upstream = orchestratorRefused(ErrorCodes.SUITABILITY_REQUIRED);

        ServiceException atTheBff = ErrorPropagation.from(upstream)
            .receivedBy("bff", PlatformLayer.L4)
            .calling("journey-orchestration", "createQuote")
            .toException();

        assertThat(atTheBff.getIncidentId())
            .as("a fresh id per hop gives support three ids for one event and no way to join them")
            .isEqualTo(upstream.getIncidentId());
        assertThat(atTheBff.getErrorResponse().getCorrelationId()).isEqualTo("corr-77");
    }

    @Test
    void anActionableRefusalReachesTheRmAsItself() {
        ServiceErrorResponse upstream = orchestratorRefused(ErrorCodes.SUITABILITY_REQUIRED);

        ServiceException atTheBff = ErrorPropagation.from(upstream)
            .receivedBy("bff", PlatformLayer.L4)
            .calling("journey-orchestration", "createQuote")
            .toException();

        assertThat(atTheBff.getErrorResponse().getCode())
            .as("a gate the RM can clear must not arrive as an unactionable 502")
            .isEqualTo(ErrorCodes.SUITABILITY_REQUIRED);
        assertThat(atTheBff.getHttpStatus()).isEqualTo(403);
        assertThat(atTheBff.getDiagnostic().getOrigin().service()).isEqualTo("journey-orchestration");
        assertThat(atTheBff.getDiagnostic().getOrigin().layer()).isEqualTo(PlatformLayer.L6);
    }

    @Test
    void aDependencyFailureIsWrappedNotPropagated() {
        ServiceErrorResponse upstream = ServiceException.of(ErrorCodes.UPSTREAM_UNAVAILABLE)
            .service("persistence")
            .layer(PlatformLayer.L7)
            .reason("connection pool exhausted")
            .build()
            .getErrorResponse();

        ServiceException wrapped = ErrorPropagation.from(upstream)
            .receivedBy("onesb", PlatformLayer.L5)
            .calling("persistence", "findQuoteJob")
            .toException();

        assertThat(wrapped.getErrorResponse().getCode())
            .as("the caller can do nothing about a dependency being down")
            .isEqualTo(ErrorCodes.UPSTREAM_UNAVAILABLE);
        assertThat(wrapped.getDiagnostic().effectiveOriginService()).isEqualTo("persistence");
        assertThat(wrapped.getDiagnostic().getReason())
            .contains("persistence answered")
            .contains("connection pool exhausted");
    }

    @Test
    void anInternalUpstreamDefectDoesNotReachTheCallerAsTheirMistake() {
        ServiceErrorResponse upstream = ServiceException.of(ErrorCodes.INTERNAL_ERROR)
            .service("persistence")
            .layer(PlatformLayer.L7)
            .reason("null aggregate id")
            .build()
            .getErrorResponse();

        ServiceException wrapped = ErrorPropagation.from(upstream)
            .receivedBy("onesb", PlatformLayer.L5)
            .calling("persistence", "saveJob")
            .toException();

        assertThat(wrapped.getErrorResponse().getCode()).isEqualTo(ErrorCodes.UPSTREAM_UNAVAILABLE);
        assertThat(wrapped.getErrorResponse().getCategory()).isEqualTo(ErrorCategory.UPSTREAM);
        assertThat(wrapped.getDiagnostic().effectiveOriginService())
            .as("wrapping must not erase whose defect it was")
            .isEqualTo("persistence");
    }

    @Test
    void theFirstOriginSurvivesThreeHops() {
        ServiceErrorResponse hop1 = orchestratorRefused(ErrorCodes.SUITABILITY_REQUIRED);

        ServiceErrorResponse hop2 = ErrorPropagation.from(hop1)
            .receivedBy("onesb", PlatformLayer.L5)
            .calling("journey-orchestration", "createQuote")
            .toException()
            .getErrorResponse();

        ServiceException hop3 = ErrorPropagation.from(hop2)
            .receivedBy("bff", PlatformLayer.L4)
            .calling("onesb", "createQuote")
            .toException();

        assertThat(hop3.getDiagnostic().effectiveOriginService())
            .as("every hop rewriting the origin loses the true source at the second hop")
            .isEqualTo("journey-orchestration");
        assertThat(hop3.getIncidentId()).isEqualTo(hop1.getIncidentId());
    }

    @Test
    void anUnregisteredUpstreamCodeIsNeverReEmittedAsOurOwn() {
        ServiceErrorResponse odd = ServiceErrorResponse.builder()
            .title("Weird").status(418).code("SOMETHING_WE_DO_NOT_KNOW")
            .build();

        ErrorPropagation propagation = ErrorPropagation.from(odd)
            .receivedBy("bff", PlatformLayer.L4)
            .calling("mystery", "call");

        assertThat(propagation.resolvedCode())
            .as("re-emitting it would publish a code with no declared status or wording")
            .isEqualTo(ErrorCodes.UPSTREAM_BAD_RESPONSE);
        assertThat(propagation.propagatesCode()).isFalse();
        assertThat(propagation.toException().getErrorResponse().getCode())
            .isEqualTo(ErrorCodes.UPSTREAM_BAD_RESPONSE);
    }

    @Test
    void propagationCarriesTheCauseAndRejectsAMissingUpstream() {
        ServiceException upstream = ServiceException.of(ErrorCodes.QUOTE_EXPIRED)
            .service("onesb").layer(PlatformLayer.L5).build();

        ServiceException propagated = ErrorPropagation.from(upstream)
            .receivedBy("bff", PlatformLayer.L4)
            .calling("onesb", "getQuote")
            .toException();

        assertThat(propagated).hasCause(upstream);
        assertThat(propagated.getErrorResponse().getCode()).isEqualTo(ErrorCodes.QUOTE_EXPIRED);

        assertThatIllegalArgumentException()
            .isThrownBy(() -> ErrorPropagation.from((ServiceErrorResponse) null));
    }
}
